package com.yueti.app.music

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.webkit.CookieManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.yueti.app.data.MusicPlaybackEntity
import com.yueti.app.data.MusicQueueEntity
import com.yueti.app.data.YuetiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MusicUiState(
    val auth: MusicAuthState = MusicAuthState.LoggedOut,
    val playlists: List<MusicPlaylist> = emptyList(),
    val activePlaylist: MusicPlaylist? = null,
    val visibleTracks: List<MusicTrack> = emptyList(),
    val searchResults: List<MusicTrack> = emptyList(),
    val queue: MusicQueueState = MusicQueueState(),
    val overlay: MusicOverlayMode = MusicOverlayMode.Closed,
    val loading: Boolean = false,
    val playbackPhase: MusicPlaybackPhase = MusicPlaybackPhase.Idle,
    val notice: String? = null,
    val compatibilityDnsEnabled: Boolean = true,
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val gateway: NeteaseGateway = PrivateNeteaseGateway(MusicNetworkStack.callFactory(application))
    private val dao = YuetiDatabase.get(application).dao()
    private val _state = MutableStateFlow(MusicUiState())
    val state: StateFlow<MusicUiState> = _state.asStateFlow()
    private var cookie: String = ""
    private var controller: MediaController? = null
    private var retriedExpiredPlaybackUrl = false
    private val artworkRefreshInFlight = mutableSetOf<String>()
    private val controllerFuture = MediaController.Builder(
        application,
        SessionToken(application, ComponentName(application, MusicPlaybackService::class.java)),
    ).buildAsync()

    init {
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(playerListener)
                    syncPlayer(mediaController)
                }
            },
            ContextCompat.getMainExecutor(application),
        )
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                dao.getMusicQueue() to dao.getMusicPlayback()
            }
            val tracks = restored.first.map(MusicQueueEntity::toTrack)
            _state.update {
                it.copy(
                    queue = MusicQueueState(
                        tracks = tracks,
                        currentIndex = restored.second?.currentIndex ?: -1,
                        playing = false,
                        positionMs = restored.second?.positionMs ?: 0L,
                    ),
                    compatibilityDnsEnabled = MusicNetworkSettings.compatibilityDnsEnabled(application),
                )
            }
            cookie = withContext(Dispatchers.IO) { MusicCredentialStore.load(application) }
            if (cookie.isNotBlank()) verifyLogin(cookie)
        }
        viewModelScope.launch {
            while (true) {
                delay(250L)
                val player = controller ?: continue
                if (player.mediaItemCount > 0) {
                    _state.update { current ->
                        val mediaId = player.currentMediaItem?.mediaId
                        val logicalIndex = current.queue.tracks.indexOfFirst { it.id == mediaId }
                        current.copy(
                            queue = current.queue.copy(
                                currentIndex = logicalIndex.takeIf { it >= 0 } ?: current.queue.currentIndex,
                                playing = player.isPlaying,
                                positionMs = player.currentPosition.coerceAtLeast(0L),
                                pendingTrackId = null,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun setOverlay(mode: MusicOverlayMode) = _state.update { it.copy(overlay = mode) }

    fun setCompatibilityDnsEnabled(enabled: Boolean) {
        MusicNetworkSettings.setCompatibilityDnsEnabled(getApplication(), enabled)
        _state.update { it.copy(compatibilityDnsEnabled = enabled) }
    }

    fun acceptWebCookie(rawCookie: String) {
        val sanitized = sanitizeCookie(rawCookie)
        if (sanitized.isBlank() || !sanitized.contains("MUSIC_U=")) return
        if (sanitized == cookie && _state.value.auth !is MusicAuthState.Error && _state.value.auth !is MusicAuthState.LoggedOut) return
        cookie = sanitized
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                with(getApplication<Application>()) { MusicCredentialStore.save(this, sanitized) }
            }
            verifyLogin(sanitized)
        }
    }

    private suspend fun verifyLogin(candidate: String) {
        _state.update { it.copy(auth = MusicAuthState.WaitingForQr, loading = true, notice = null) }
        gateway.account(candidate).onSuccess { account ->
            _state.update { it.copy(auth = account, loading = false) }
            loadPlaylists(account.userId)
        }.onFailure { error ->
            _state.update { it.copy(auth = MusicAuthState.Error(error.message ?: "登录验证失败"), loading = false) }
        }
    }

    fun refreshPlaylists() {
        val account = _state.value.auth as? MusicAuthState.LoggedIn
        viewModelScope.launch {
            if (account != null) loadPlaylists(account.userId)
            else if (cookie.isNotBlank()) verifyLogin(cookie)
            else _state.update { it.copy(auth = MusicAuthState.LoggedOut) }
        }
    }

    private suspend fun loadPlaylists(userId: String) {
        _state.update { it.copy(loading = true) }
        gateway.playlists(cookie, userId).onSuccess { playlists ->
            _state.update { it.copy(playlists = playlists, loading = false, notice = null) }
        }.onFailure(::showError)
    }

    fun openPlaylist(playlist: MusicPlaylist) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, activePlaylist = playlist) }
            gateway.playlistTracks(cookie, playlist).onSuccess { tracks ->
                _state.update { it.copy(visibleTracks = tracks, loading = false) }
            }.onFailure(::showError)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            gateway.search(cookie, query).onSuccess { tracks ->
                _state.update { it.copy(searchResults = tracks, loading = false) }
            }.onFailure(::showError)
        }
    }

    fun playVisible(track: MusicTrack) {
        val source = when {
            track in _state.value.searchResults -> _state.value.searchResults
            track in _state.value.visibleTracks -> _state.value.visibleTracks
            else -> listOf(track)
        }.filter(MusicTrack::playable)
        val index = source.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playQueue(source, index)
    }

    fun playQueue(tracks: List<MusicTrack>, startIndex: Int, startPositionMs: Long = 0L) {
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, playbackPhase = MusicPlaybackPhase.Resolving, notice = null) }
            gateway.resolveUrls(cookie, tracks.map(MusicTrack::id)).onSuccess { urls ->
                val resolvedIds = urls.keys
                val logicalIndex = nextResolvedQueueIndex(
                    queueTrackIds = tracks.map(MusicTrack::id),
                    resolvedTrackIds = resolvedIds,
                    requestedIndex = startIndex,
                )
                if (logicalIndex < 0) {
                    showError(
                        IllegalStateException(
                            "从当前歌曲开始没有可播放曲目，可能需要会员或存在地区限制",
                        ),
                    )
                    return@onSuccess
                }
                val playable = tracks.filter { it.id in resolvedIds }
                val chosenTrack = tracks[logicalIndex]
                val resolvedIndex = playable.indexOfFirst { it.id == chosenTrack.id }
                val items = playable.map { track -> track.toMediaItem(requireNotNull(urls[track.id])) }
                val player = controller ?: run {
                    showError(IllegalStateException("播放器仍在准备，请稍后重试"))
                    return@onSuccess
                }
                player.setMediaItems(items, resolvedIndex, startPositionMs.coerceAtLeast(0L))
                player.volume = 1f
                player.prepare()
                player.play()
                val skipped = tracks.size - playable.size
                _state.update {
                    it.copy(
                        queue = MusicQueueState(
                            tracks = tracks,
                            currentIndex = logicalIndex,
                            playing = false,
                            positionMs = 0L,
                            pendingTrackId = chosenTrack.id,
                            unavailableTrackIds = tracks.map(MusicTrack::id).toSet() - resolvedIds,
                        ),
                        playbackPhase = MusicPlaybackPhase.Buffering,
                        loading = false,
                        overlay = MusicOverlayMode.Closed,
                        notice = when {
                            logicalIndex != startIndex -> "所选歌曲暂不可播，已跳到《${chosenTrack.title}》"
                            skipped > 0 -> "当前队列有 $skipped 首歌曲受会员或地区限制"
                            else -> null
                        },
                    )
                }
                persistQueue(tracks, logicalIndex, 0L)
            }.onFailure(::showError)
        }
    }

    fun togglePlayback() {
        val player = controller ?: return
        if ((player.mediaItemCount == 0 || _state.value.playbackPhase == MusicPlaybackPhase.Error) &&
            _state.value.queue.tracks.isNotEmpty()
        ) {
            playQueue(
                _state.value.queue.tracks,
                _state.value.queue.currentIndex.coerceAtLeast(0),
                _state.value.queue.positionMs,
            )
        } else if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun next() { controller?.takeIf(Player::hasNextMediaItem)?.seekToNextMediaItem() }
    fun previous() { controller?.takeIf(Player::hasPreviousMediaItem)?.seekToPreviousMediaItem() }

    fun playAt(index: Int) {
        val queue = _state.value.queue.tracks
        if (index !in queue.indices) return
        val player = controller
        val mediaIds = player?.let { active ->
            (0 until active.mediaItemCount).map { active.getMediaItemAt(it).mediaId }.toSet()
        }.orEmpty()
        val logicalIndex = nextResolvedQueueIndex(queue.map(MusicTrack::id), mediaIds, index)
        val mediaIndex = if (logicalIndex >= 0) {
            (0 until (player?.mediaItemCount ?: 0)).firstOrNull {
                player?.getMediaItemAt(it)?.mediaId == queue[logicalIndex].id
            } ?: -1
        } else {
            -1
        }
        if (player != null && mediaIndex >= 0) {
            _state.update {
                it.copy(
                    playbackPhase = MusicPlaybackPhase.Buffering,
                    queue = it.queue.copy(pendingTrackId = queue[logicalIndex].id),
                    notice = if (logicalIndex != index) "所选歌曲暂不可播，已跳到《${queue[logicalIndex].title}》" else it.notice,
                )
            }
            player.seekTo(mediaIndex, 0L)
            player.play()
        } else {
            // Room can restore the visible queue before Media3 reconnects/rebuilds its items.
            // A Dock selection must still be actionable after a force-stop.
            playQueue(queue, index)
        }
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        val duration = player.duration.takeIf { it > 0L }
            ?: _state.value.queue.current?.durationMs?.takeIf { it > 0L }
            ?: return
        val target = positionMs.coerceIn(0L, duration)
        player.seekTo(target)
        _state.update { it.copy(queue = it.queue.copy(positionMs = target)) }
        persistQueue(_state.value.queue.tracks, player.currentMediaItemIndex, target)
    }

    fun refreshArtwork(trackId: String) {
        if (trackId.isBlank() || !artworkRefreshInFlight.add(trackId)) return
        viewModelScope.launch {
            gateway.artwork(cookie, trackId)
                .onSuccess { url ->
                    if (!url.isNullOrBlank()) {
                        _state.update { state -> state.withArtwork(trackId, url) }
                    }
                }
            artworkRefreshInFlight.remove(trackId)
        }
    }

    fun logout() {
        controller?.run { stop(); clearMediaItems() }
        cookie = ""
        MusicCredentialStore.clear(getApplication())
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        viewModelScope.launch(Dispatchers.IO) { dao.clearMusicQueue() }
        _state.value = MusicUiState(overlay = MusicOverlayMode.Library)
    }

    fun consumeNotice() = _state.update { it.copy(notice = null) }

    private fun showError(error: Throwable) {
        val message = musicNetworkErrorMessage(error)
        val expired = message.contains("登录状态") || message.contains("重新扫码")
        _state.update {
            it.copy(
                loading = false,
                playbackPhase = MusicPlaybackPhase.Error,
                queue = it.queue.copy(playing = false),
                notice = message,
                auth = if (expired) MusicAuthState.Error(message) else it.auth,
            )
        }
    }

    private fun syncPlayer(player: Player) {
        val mediaId = player.currentMediaItem?.mediaId
        val phase = musicPlaybackPhase(
            playbackState = player.playbackState,
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady,
            hasError = player.playerError != null,
        )
        val volumeMuted = phase == MusicPlaybackPhase.Playing &&
            (getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                .getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        _state.update { state ->
            val logicalIndex = state.queue.tracks.indexOfFirst { it.id == mediaId }
            state.copy(
                queue = state.queue.copy(
                    currentIndex = logicalIndex.takeIf { it >= 0 } ?: state.queue.currentIndex,
                    playing = phase == MusicPlaybackPhase.Playing,
                    positionMs = player.currentPosition,
                    pendingTrackId = null,
                ),
                playbackPhase = phase,
                loading = if (phase == MusicPlaybackPhase.Playing || phase == MusicPlaybackPhase.ReadyPaused) false else state.loading,
                notice = if (volumeMuted) state.notice ?: "媒体音量为 0，请提高手机媒体音量" else state.notice,
            )
        }
        val logicalIndex = _state.value.queue.currentIndex
        if (logicalIndex >= 0) persistQueue(_state.value.queue.tracks, logicalIndex, player.currentPosition)
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (player.playbackState == Player.STATE_READY) retriedExpiredPlaybackUrl = false
            syncPlayer(player)
        }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val queue = _state.value.queue
            val shouldRefreshUrl = !retriedExpiredPlaybackUrl && queue.tracks.isNotEmpty() &&
                error.errorCode in setOf(
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                )
            if (shouldRefreshUrl) {
                retriedExpiredPlaybackUrl = true
                playQueue(queue.tracks, queue.currentIndex.coerceAtLeast(0))
            } else {
                showError(IllegalStateException("播放失败，请检查网络后重试：${error.errorCodeName}"))
            }
        }
    }

    private fun persistQueue(tracks: List<MusicTrack>, index: Int, positionMs: Long) {
        if (tracks.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.replaceMusicQueue(
                tracks.mapIndexed { position, track -> track.toEntity(position) },
                MusicPlaybackEntity(currentIndex = index, positionMs = positionMs),
            )
        }
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}

private val AllowedCookieNames = setOf("MUSIC_U", "__csrf", "NMTID", "__remember_me", "os", "appver")

internal fun sanitizeCookie(raw: String): String = raw.split(';').map(String::trim).mapNotNull { entry ->
    val name = entry.substringBefore('=', "")
    entry.takeIf { name in AllowedCookieNames && entry.contains('=') }
}.joinToString("; ")

private fun MusicTrack.toMediaItem(url: String): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(url)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(normalizeNeteaseMediaUrl(artworkUrl)?.let(Uri::parse))
            .build(),
    ).build()

private fun MusicTrack.toEntity(position: Int) = MusicQueueEntity(
    position, id, title, artist, album, artworkUrl, playlistId, playlistName, durationMs, playable,
)

private fun MusicQueueEntity.toTrack() = MusicTrack(
    id = trackId,
    title = title,
    artist = artist,
    album = album,
    artworkUrl = normalizeNeteaseMediaUrl(artworkUrl).orEmpty(),
    playlistId = playlistId,
    playlistName = playlistName,
    durationMs = durationMs,
    playable = playable,
)

private fun MusicUiState.withArtwork(trackId: String, artworkUrl: String): MusicUiState {
    fun List<MusicTrack>.updated(): List<MusicTrack> = map { track ->
        if (track.id == trackId) track.copy(artworkUrl = artworkUrl) else track
    }
    return copy(
        visibleTracks = visibleTracks.updated(),
        searchResults = searchResults.updated(),
        queue = queue.copy(tracks = queue.tracks.updated()),
    )
}
