package com.yueti.app.music

import kotlin.math.roundToInt

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val playlistId: String = "",
    val playlistName: String = "",
    val durationMs: Long = 0L,
    val playable: Boolean = true,
)

data class MusicPlaylist(
    val id: String,
    val name: String,
    val coverUrl: String = "",
    val trackCount: Int = 0,
    val subscribed: Boolean = false,
)

sealed interface MusicAuthState {
    data object LoggedOut : MusicAuthState
    data object WaitingForQr : MusicAuthState
    data object Scanned : MusicAuthState
    data class LoggedIn(val userId: String, val nickname: String) : MusicAuthState
    data class Error(val message: String, val recoverable: Boolean = true) : MusicAuthState
}

enum class MusicOverlayMode { Closed, Library, Search, ArcQueue }

enum class MusicPlaybackPhase { Idle, Resolving, Buffering, ReadyPaused, Playing, Error }

/** Pure presentation reducer. The pause affordance is only legal while Media3 reports isPlaying. */
internal fun musicPlaybackPhase(
    playbackState: Int,
    isPlaying: Boolean,
    playWhenReady: Boolean,
    hasError: Boolean,
): MusicPlaybackPhase = when {
    hasError -> MusicPlaybackPhase.Error
    isPlaying -> MusicPlaybackPhase.Playing
    playbackState == 2 || (playWhenReady && playbackState != 3) -> MusicPlaybackPhase.Buffering
    playbackState == 3 -> MusicPlaybackPhase.ReadyPaused
    else -> MusicPlaybackPhase.Idle
}

internal fun normalizedPlaybackProgress(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toDouble() / durationMs.toDouble()).toFloat().coerceIn(0f, 1f)
}

data class MusicQueueState(
    val tracks: List<MusicTrack> = emptyList(),
    val currentIndex: Int = -1,
    val playing: Boolean = false,
    val positionMs: Long = 0L,
    val pendingTrackId: String? = null,
    val unavailableTrackIds: Set<String> = emptySet(),
) {
    val current: MusicTrack? get() = tracks.getOrNull(currentIndex)
}

internal fun nextResolvedQueueIndex(
    queueTrackIds: List<String>,
    resolvedTrackIds: Set<String>,
    requestedIndex: Int,
): Int {
    if (queueTrackIds.isEmpty()) return -1
    val start = requestedIndex.coerceIn(queueTrackIds.indices)
    return (start..queueTrackIds.lastIndex).firstOrNull { queueTrackIds[it] in resolvedTrackIds } ?: -1
}

data class MusicArcItem(
    val track: MusicTrack,
    val relativeIndex: Int,
    val queueIndex: Int = -1,
    val visualSlot: Int = 0,
)

data class MusicFanAnchor(
    val centerXInWindowPx: Float,
    val centerYInWindowPx: Float,
    val artworkSizePx: Float,
)

internal fun musicArcWindow(
    queue: List<MusicTrack>,
    currentIndex: Int,
): List<MusicArcItem> = (-2..2).mapNotNull { relative ->
    queue.getOrNull(currentIndex + relative)?.let { MusicArcItem(it, relative, currentIndex + relative) }
}

/** Four real neighbours around the preview focus; the playing track remains in the Dock pivot. */
internal fun musicFanWindow(
    queue: List<MusicTrack>,
    playingIndex: Int,
    previewIndex: Float,
    limit: Int = 4,
): List<MusicArcItem> {
    if (queue.isEmpty() || limit <= 0) return emptyList()
    val focus = previewIndex.roundToInt().coerceIn(queue.indices)
    return queue.indices.asSequence()
        .filter { it != playingIndex }
        .sortedWith(compareBy<Int> { kotlin.math.abs(it - focus) }.thenBy { it })
        .take(limit)
        .mapIndexed { slot, index ->
            MusicArcItem(
                track = queue[index],
                relativeIndex = index - playingIndex,
                queueIndex = index,
                visualSlot = slot + 1,
            )
        }
        .toList()
}
