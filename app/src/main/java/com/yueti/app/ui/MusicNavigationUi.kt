package com.yueti.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yueti.app.music.MusicArcItem
import com.yueti.app.music.MusicAuthState
import com.yueti.app.music.MusicFanPhase
import com.yueti.app.music.MusicFanAnchor
import com.yueti.app.music.MusicOverlayMode
import com.yueti.app.music.MusicPlaybackPhase
import com.yueti.app.music.MusicPlaylist
import com.yueti.app.music.MusicTrack
import com.yueti.app.music.MusicUiState
import com.yueti.app.music.musicFanPose
import com.yueti.app.music.musicFanWindow
import com.yueti.app.music.normalizeNeteaseMediaUrl
import com.yueti.app.music.normalizedPlaybackProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val MusicDockColor = Color(0xF21A171B)
private val MusicDockContent = Color(0xFFF5EFF7)

/** Music UI intentionally lives above the app shell: the plus tool remains outside this layer. */
@Composable
internal fun MusicOverlayHost(
    state: MusicUiState,
    animationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onCookie: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenPlaylist: (MusicPlaylist) -> Unit,
    onPlay: (MusicTrack) -> Unit,
    onSearch: (String) -> Unit,
    onSearchMode: () -> Unit,
    onLibraryMode: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onLogout: () -> Unit,
    fanAnchor: MusicFanAnchor? = null,
    onCompatibilityDnsChanged: (Boolean) -> Unit = {},
    onRefreshArtwork: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        when (state.overlay) {
            MusicOverlayMode.Closed -> Unit
            MusicOverlayMode.Library -> MusicLibraryPage(
            state = state,
            animationsEnabled = animationsEnabled,
            onDismiss = onDismiss,
            onCookie = onCookie,
            onRefresh = onRefresh,
            onOpenPlaylist = onOpenPlaylist,
            onPlay = onPlay,
            onSearchMode = onSearchMode,
            onToggle = onToggle,
            onPrevious = onPrevious,
            onNext = onNext,
            onLogout = onLogout,
            onCompatibilityDnsChanged = onCompatibilityDnsChanged,
            onRefreshArtwork = onRefreshArtwork,
        )
            MusicOverlayMode.Search -> MusicSearchOverlay(
            state = state,
            animationsEnabled = animationsEnabled,
            onDismiss = onLibraryMode,
            onSearch = onSearch,
            onPlay = onPlay,
            onPrevious = onPrevious,
            onNext = onNext,
            onRefreshArtwork = onRefreshArtwork,
        )
            MusicOverlayMode.ArcQueue -> MusicArcQueueOverlay(
            state = state,
            animationsEnabled = animationsEnabled,
            onDismiss = onDismiss,
            onPlayAt = onPlayAt,
            fanAnchor = fanAnchor,
            onRefreshArtwork = onRefreshArtwork,
            )
        }
    }
}

@Composable
private fun MusicLibraryPage(
    state: MusicUiState,
    animationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onCookie: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenPlaylist: (MusicPlaylist) -> Unit,
    onPlay: (MusicTrack) -> Unit,
    onSearchMode: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLogout: () -> Unit,
    onCompatibilityDnsChanged: (Boolean) -> Unit,
    onRefreshArtwork: (String) -> Unit,
) {
    var contentReady by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) delay(338)
        contentReady = true
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MusicPanelHeader(state, onDismiss, onSearchMode)
            if (!contentReady) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (val auth = state.auth) {
                    MusicAuthState.LoggedOut, MusicAuthState.WaitingForQr, MusicAuthState.Scanned ->
                        NeteaseLoginWebView(onCookie)
                    is MusicAuthState.Error -> MusicLoginError(auth.message, onRefresh)
                    is MusicAuthState.LoggedIn -> MusicLibraryContent(
                        state, auth.nickname, onRefresh, onOpenPlaylist, onPlay, onToggle,
                        onPrevious, onNext, onLogout, onCompatibilityDnsChanged, onRefreshArtwork,
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicPanelHeader(state: MusicUiState, onDismiss: () -> Unit, onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Rounded.LibraryMusic, null, Modifier.padding(10.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("网易云音乐", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                if (state.auth is MusicAuthState.LoggedIn) "私人非商业 · 直接连接网易云" else "扫码登录后播放你的歌单",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.auth is MusicAuthState.LoggedIn) IconButton(onClick = onSearch) {
            Icon(Icons.Rounded.Search, "搜索歌曲")
        }
        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "关闭音乐面板") }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NeteaseLoginWebView(onCookie: (String) -> Unit) {
    val context = LocalContext.current
    val riskPreferences = remember { context.getSharedPreferences("music_risk_consent", android.content.Context.MODE_PRIVATE) }
    var riskAccepted by remember { mutableStateOf(riskPreferences.getBoolean("accepted_v1", false)) }
    if (!riskAccepted) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("启用前请确认", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                "此功能直接调用网易云未公开接口，仅供私人非商业使用。接口可能随时变化，也可能触发行为验证或账号风控；跃题不提供下载、解灰、会员或 DRM 绕过。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                riskPreferences.edit().putBoolean("accepted_v1", true).apply()
                riskAccepted = true
            }) { Text("我了解风险，打开官方登录页") }
            Text("可随时用右上角关闭，不会产生登录或播放请求。", style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(webView) {
        while (webView != null) {
            CookieManager.getInstance().getCookie("https://music.163.com")?.let(onCookie)
            delay(1200)
        }
    }
    Column(Modifier.fillMaxWidth().heightIn(min = 360.dp, max = 500.dp).padding(horizontal = 12.dp)) {
        Text(
            "在网易云官方页面选择二维码登录；行为验证和扫码确认均由官网完成。",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)).background(Color.White)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.userAgentString = settings.userAgentString.replace("; wv", "")
                        CookieManager.getInstance().setAcceptCookie(true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val host = request.url.host.orEmpty().lowercase()
                                return request.url.scheme != "https" ||
                                    !(host == "music.163.com" || host.endsWith(".music.163.com") || host.endsWith(".163.com"))
                            }
                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) { loading = true }
                            override fun onPageFinished(view: WebView, url: String) {
                                loading = false
                                view.evaluateJavascript(OpenOfficialQrLoginScript, null)
                            }
                            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                                handler.cancel()
                            }
                        }
                        loadUrl("https://music.163.com/#/login")
                        webView = this
                    }
                },
            )
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            webView?.run { stopLoading(); loadUrl("about:blank"); removeAllViews(); destroy() }
            webView = null
        }
    }
}

@Composable
private fun MusicLoginError(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text("重新验证") }
    }
}

@Composable
private fun ColumnScope.MusicLibraryContent(
    state: MusicUiState,
    nickname: String,
    onRefresh: () -> Unit,
    onOpenPlaylist: (MusicPlaylist) -> Unit,
    onPlay: (MusicTrack) -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLogout: () -> Unit,
    onCompatibilityDnsChanged: (Boolean) -> Unit,
    onRefreshArtwork: (String) -> Unit,
) {
    if (state.loading) CircularProgressIndicator(Modifier.padding(horizontal = 22.dp).size(22.dp))
    state.queue.current?.let { track ->
        NowPlayingRow(track, state.queue.playing, onToggle, onPrevious, onNext, onRefreshArtwork)
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$nickname 的音乐", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "刷新歌单") }
        IconButton(onClick = onLogout) { Icon(Icons.Rounded.Logout, "退出网易云") }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("兼容 DNS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                "系统解析失败时，仅为网易云域名启用加密恢复",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = state.compatibilityDnsEnabled,
            onCheckedChange = onCompatibilityDnsChanged,
        )
    }
    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
        if (state.activePlaylist != null && state.visibleTracks.isNotEmpty()) {
            item {
                Text(state.activePlaylist.name, Modifier.padding(18.dp, 10.dp), style = MaterialTheme.typography.titleMedium)
            }
            items(state.visibleTracks, key = MusicTrack::id) { track -> MusicTrackRow(track, onPlay, onRefreshArtwork) }
        } else {
            items(state.playlists, key = MusicPlaylist::id) { playlist ->
                ListItem(
                    headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text("${playlist.trackCount} 首${if (playlist.subscribed) " · 收藏歌单" else ""}") },
                    leadingContent = { Artwork(playlist.coverUrl, playlist.name, 52.dp) },
                    modifier = Modifier.combinedClickable(onClick = { onOpenPlaylist(playlist) }, onLongClick = {}),
                )
            }
        }
    }
}

@Composable
private fun NowPlayingRow(
    track: MusicTrack,
    playing: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRefreshArtwork: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(track.artworkUrl, track.title, 52.dp, track.id, onRefreshArtwork = onRefreshArtwork)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            IconButton(onClick = onPrevious) { Icon(Icons.Rounded.SkipPrevious, "上一首") }
            IconButton(onClick = onToggle) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "暂停" else "播放") }
            IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, "下一首") }
        }
    }
}

@Composable
private fun MusicTrackRow(
    track: MusicTrack,
    onPlay: (MusicTrack) -> Unit,
    onRefreshArtwork: (String) -> Unit,
) {
    ListItem(
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(listOf(track.artist, track.album).filter(String::isNotBlank).joinToString(" · "), maxLines = 1) },
        leadingContent = { Artwork(track.artworkUrl, track.title, 48.dp, track.id, onRefreshArtwork = onRefreshArtwork) },
        trailingContent = { if (!track.playable) Text("不可播", color = MaterialTheme.colorScheme.error) else Icon(Icons.Rounded.PlayArrow, null) },
        modifier = Modifier.combinedClickable(enabled = track.playable, onClick = { onPlay(track) }, onLongClick = {}),
    )
}

@Composable
private fun MusicSearchOverlay(
    state: MusicUiState,
    animationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onPlay: (MusicTrack) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRefreshArtwork: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var horizontal by remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .68f))) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp).imePadding(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回音乐库") }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("搜索歌曲、歌手或专辑") },
                        trailingIcon = { IconButton(onClick = { onSearch(query) }) { Icon(Icons.Rounded.Search, "搜索") } },
                    )
                }
                state.queue.current?.let { track ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                            .pointerInput(state.queue.currentIndex) {
                                detectHorizontalDragGestures(
                                    onDragStart = { horizontal = 0f },
                                    onHorizontalDrag = { change, amount -> change.consume(); horizontal += amount },
                                    onDragEnd = {
                                        if (abs(horizontal) > 42.dp.toPx()) if (horizontal < 0) onNext() else onPrevious()
                                        horizontal = 0f
                                    },
                                )
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Artwork(track.artworkUrl, track.title, 46.dp, track.id, onRefreshArtwork = onRefreshArtwork)
                            Spacer(Modifier.width(10.dp))
                            Column { Text(track.title, fontWeight = FontWeight.Bold); Text(track.artist, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(state.searchResults, key = MusicTrack::id) { MusicTrackRow(it, onPlay, onRefreshArtwork) }
                }
            }
        }
    }
}

@Composable
private fun MusicArcQueueOverlay(
    state: MusicUiState,
    animationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onPlayAt: (Int) -> Unit,
    fanAnchor: MusicFanAnchor?,
    onRefreshArtwork: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val expansion = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    var previewIndex by remember(state.queue.tracks, state.queue.currentIndex) {
        mutableFloatStateOf(state.queue.currentIndex.coerceAtLeast(0).toFloat())
    }
    val mergeProgress = remember { Animatable(0f) }
    var phase by remember { mutableStateOf(MusicFanPhase.Entering) }
    var mergingIndex by remember { mutableStateOf<Int?>(null) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val focusIndex = if (state.queue.tracks.isEmpty()) 0 else {
        previewIndex.roundToInt().coerceIn(state.queue.tracks.indices)
    }
    val items = remember(state.queue.tracks, state.queue.currentIndex, focusIndex) {
        musicFanWindow(state.queue.tracks, state.queue.currentIndex, focusIndex.toFloat())
    }
    var renderedItems by remember(state.queue.tracks, state.queue.currentIndex) { mutableStateOf(items) }
    LaunchedEffect(items) {
        renderedItems = (renderedItems + items).distinctBy { it.track.id }
        if (animationsEnabled) delay(190)
        renderedItems = items
    }
    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            expansion.animateTo(1f, spring(dampingRatio = .58f, stiffness = 330f))
        } else {
            expansion.snapTo(1f)
        }
        phase = MusicFanPhase.Browsing
    }
    val closeFan: () -> Unit = {
        if (phase != MusicFanPhase.Merging && phase != MusicFanPhase.Closing) {
            settleJob?.cancel()
            scope.launch {
                phase = MusicFanPhase.Closing
                if (animationsEnabled) expansion.animateTo(0f, spring(dampingRatio = .78f, stiffness = 520f))
                onDismiss()
            }
        }
    }
    val selectAndClose: (Int) -> Unit = { target ->
        if (phase != MusicFanPhase.Merging && phase != MusicFanPhase.Closing && target in state.queue.tracks.indices) {
            settleJob?.cancel()
            settleJob = scope.launch {
                phase = MusicFanPhase.Merging
                mergingIndex = target
                mergeProgress.snapTo(0f)
                if (animationsEnabled) mergeProgress.animateTo(.70f, tween(300)) else mergeProgress.snapTo(.70f)
                onPlayAt(target)
                if (animationsEnabled) {
                    mergeProgress.animateTo(1f, spring(dampingRatio = .56f, stiffness = 430f))
                    expansion.animateTo(0f, tween(140))
                } else {
                    mergeProgress.snapTo(1f)
                    expansion.snapTo(0f)
                }
                phase = MusicFanPhase.Closing
                onDismiss()
            }
        }
    }
    BackHandler(onBack = closeFan)
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = .035f * expansion.value),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = .075f * expansion.value),
                        Color.White.copy(alpha = .06f * expansion.value),
                    ),
                ),
            )
            .pointerInput(state.queue.tracks, state.queue.currentIndex, mergingIndex) {
                if (state.queue.tracks.isEmpty() || mergingIndex != null) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    settleJob?.cancel()
                    val velocityTracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
                    val stepPx = with(density) { 64.dp.toPx() }
                    var lastPosition = down.position
                    var totalDrag = 0f
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.position.y - lastPosition.y
                        lastPosition = change.position
                        totalDrag += delta
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        if (!dragging && abs(totalDrag) > viewConfiguration.touchSlop) dragging = true
                        if (dragging) {
                            change.consume()
                            previewIndex = (previewIndex - delta / stepPx)
                                .coerceIn(0f, state.queue.tracks.lastIndex.toFloat())
                        }
                    }
                    if (dragging) {
                        val velocity = velocityTracker.calculateVelocity().y
                        val projected = (previewIndex - velocity / stepPx * .16f)
                            .coerceIn(0f, state.queue.tracks.lastIndex.toFloat())
                        val target = projected.roundToInt().toFloat()
                        settleJob = scope.launch {
                            phase = MusicFanPhase.Snapping
                            if (animationsEnabled) {
                                Animatable(previewIndex).animateTo(
                                    target,
                                    spring(dampingRatio = .64f, stiffness = 380f),
                                    initialVelocity = (-velocity / stepPx).coerceIn(-5f, 5f),
                                ) { previewIndex = value }
                            } else {
                                previewIndex = target
                            }
                            phase = MusicFanPhase.Browsing
                        }
                    }
                }
            },
    ) {
        Box(
            Modifier.fillMaxSize().pointerInput(phase) {
                detectTapGestures(onTap = { closeFan() })
            },
        )
        val fallbackCenterX = with(density) { 45.dp.toPx() }
        val fallbackCenterY = with(density) { maxHeight.toPx() - 50.dp.toPx() }
        val centerX = fanAnchor?.centerXInWindowPx ?: fallbackCenterX
        val centerY = fanAnchor?.centerYInWindowPx ?: fallbackCenterY
        val artworkSizePx = with(density) { 42.dp.toPx() }
        val leftDp = with(density) { (centerX - artworkSizePx / 2f).toDp() }
        val availableWidth = maxWidth - leftDp - 14.dp
        val cardWidth = minOf(330.dp, maxOf(190.dp, availableWidth))
        Box(Modifier.fillMaxSize()) {
            renderedItems.forEach { rendered ->
                key(rendered.track.id) {
                    val desired = items.firstOrNull { it.track.id == rendered.track.id }
                    val visible = desired != null
                    val targetSlot = desired?.visualSlot?.toFloat()
                        ?: if (rendered.queueIndex < focusIndex) 0f else 5f
                    ArcTrackCard(
                        item = desired ?: rendered,
                        targetSlot = targetSlot,
                        visible = visible,
                        expansion = expansion.value,
                        previewFraction = previewIndex - previewIndex.roundToInt(),
                        globalMergeProgress = mergeProgress.value,
                        isMergeTarget = mergingIndex == rendered.queueIndex,
                        mergeActive = mergingIndex != null,
                        anchorCenterXInWindowPx = centerX,
                        anchorCenterYInWindowPx = centerY,
                        cardWidth = cardWidth,
                        animationsEnabled = animationsEnabled,
                        onPlayAt = selectAndClose,
                        onRefreshArtwork = onRefreshArtwork,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ArcTrackCard(
    item: MusicArcItem,
    targetSlot: Float,
    visible: Boolean,
    expansion: Float,
    previewFraction: Float,
    globalMergeProgress: Float,
    isMergeTarget: Boolean,
    mergeActive: Boolean,
    anchorCenterXInWindowPx: Float,
    anchorCenterYInWindowPx: Float,
    cardWidth: androidx.compose.ui.unit.Dp,
    animationsEnabled: Boolean,
    onPlayAt: (Int) -> Unit,
    onRefreshArtwork: (String) -> Unit,
) {
    var entered by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(Unit) { entered = true }
    val animatedSlot by animateFloatAsState(
        targetValue = targetSlot,
        animationSpec = spring(dampingRatio = .72f, stiffness = 430f),
        label = "track slot ${item.track.id}",
    )
    val itemAlpha by animateFloatAsState(
        targetValue = if (visible && entered) 1f else 0f,
        animationSpec = tween(if (animationsEnabled) 170 else 0),
        label = "track alpha ${item.track.id}",
    )
    val visualSlot = animatedSlot
    val staggerOrder = (targetSlot - 1f).coerceAtLeast(0f)
    val threshold = staggerOrder * .055f
    val localExpansion = ((expansion - threshold) / (1f - threshold)).coerceIn(0f, 1f)
    val pose = musicFanPose(item.relativeIndex.coerceIn(-2, 2), localExpansion, visualSlot.roundToInt())
    val merge = if (isMergeTarget) globalMergeProgress else 0f
    val mergeY = (pose.yDp - previewFraction * 64f) * (1f - merge)
    val primaryDroplet = MaterialTheme.colorScheme.primary
    val secondaryDroplet = MaterialTheme.colorScheme.primaryContainer
    val density = LocalDensity.current
    val artworkSizePx = with(density) { 42.dp.toPx() }
    val baseLeftPx = anchorCenterXInWindowPx - artworkSizePx / 2f
    val baseTopPx = anchorCenterYInWindowPx - with(density) { 28.dp.toPx() }
    Box(
        modifier = Modifier.offset { IntOffset(baseLeftPx.roundToInt(), baseTopPx.roundToInt()) }
            .width(cardWidth).height(56.dp)
            .zIndex(if (isMergeTarget) 40f else 10f - visualSlot).graphicsLayer {
                transformOrigin = TransformOrigin((artworkSizePx / 2f / size.width).coerceIn(0f, 1f), .5f)
                translationY = mergeY.dp.toPx()
                rotationZ = pose.rotationDegrees * (1f - merge)
                scaleX = pose.scale + (1f - pose.scale) * merge
                scaleY = pose.scale + (1f - pose.scale) * merge
                alpha = itemAlpha * pose.alpha * if (mergeActive && !isMergeTarget) (1f - globalMergeProgress) else 1f
            },
    ) {
        Surface(
            onClick = { onPlayAt(item.queueIndex) },
            modifier = Modifier.fillMaxSize(),
            color = MusicDockColor,
            contentColor = MusicDockContent,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
        ) {
            Row(Modifier.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(
                    item.track.artworkUrl,
                    item.track.title,
                    42.dp,
                    item.track.id,
                    onRefreshArtwork = onRefreshArtwork,
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.track.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.track.artist, color = MusicDockContent.copy(alpha = .70f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (isMergeTarget && merge in .06f..0.94f) {
            Canvas(Modifier.fillMaxSize()) {
                val fade = (1f - abs(merge - .5f) * 2f).coerceIn(0f, 1f)
                drawCircle(
                    color = primaryDroplet.copy(alpha = .34f * fade),
                    radius = 8.dp.toPx() * (1f - merge * .35f),
                    center = Offset(size.width * .78f, size.height + 7.dp.toPx()),
                )
                drawCircle(
                    color = secondaryDroplet.copy(alpha = .28f * fade),
                    radius = 5.dp.toPx() * (1f - merge * .45f),
                    center = Offset(size.width * .88f, size.height + 2.dp.toPx()),
                )
            }
        }
    }
}

@Composable
internal fun MiniMusicBar(
    state: MusicUiState,
    revealProgress: Float,
    rotationX: Float,
    onOpenPage: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onArc: () -> Unit,
    onArtworkAnchorChanged: (MusicFanAnchor) -> Unit,
    onRefreshArtwork: (String) -> Unit,
) {
    val track = state.queue.current
    val foreground = MusicDockContent
    val secondaryForeground = foreground.copy(alpha = .64f)
    val duration = track?.durationMs?.coerceAtLeast(0L) ?: 0L
    var scrubbing by remember(track?.id) { mutableStateOf(false) }
    var scrubProgress by remember(track?.id) {
        mutableFloatStateOf(normalizedPlaybackProgress(state.queue.positionMs, duration))
    }
    LaunchedEffect(state.queue.positionMs, duration, scrubbing) {
        if (!scrubbing) scrubProgress = normalizedPlaybackProgress(state.queue.positionMs, duration)
    }
    val interactive = if (revealProgress > .7f) {
        Modifier.combinedClickable(
            onClick = onOpenPage,
            onLongClick = { if (track != null) onArc() },
        )
    } else {
        Modifier
    }
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            alpha = revealProgress
            this.rotationX = rotationX
            scaleX = .94f + revealProgress * .06f
            scaleY = .94f + revealProgress * .06f
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, 1f)
        }.then(interactive)
            .semantics {
                contentDescription = if (track == null) {
                    if (state.auth is MusicAuthState.LoggedIn) "网易云音乐，点按打开音乐库" else "网易云音乐，点按进入扫码登录"
                } else {
                    "正在播放${track.title}，长按打开 Dock 扇形队列"
                }
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (track != null) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).onGloballyPositioned { coordinates ->
                        val origin = coordinates.positionInWindow()
                        onArtworkAnchorChanged(
                            MusicFanAnchor(
                                centerXInWindowPx = origin.x + coordinates.size.width / 2f,
                                centerYInWindowPx = origin.y + coordinates.size.height / 2f,
                                artworkSizePx = coordinates.size.width.toFloat(),
                            ),
                        )
                    },
                ) {
                    AnimatedContent(
                        targetState = track,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            (slideInVertically(tween(170)) { it / 3 } + fadeIn(tween(150))) togetherWith
                                (slideOutVertically(tween(120)) { -it / 3 } + fadeOut(tween(100)))
                        },
                        label = "mini artwork change",
                    ) { shownTrack ->
                        Artwork(
                            shownTrack.artworkUrl,
                            shownTrack.title,
                            36.dp,
                            shownTrack.id,
                            onRefreshArtwork = onRefreshArtwork,
                        )
                    }
                }
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = track,
                        transitionSpec = {
                            (slideInVertically(tween(170)) { it / 2 } + fadeIn(tween(150))) togetherWith
                                (slideOutVertically(tween(110)) { -it / 2 } + fadeOut(tween(90)))
                        },
                        label = "mini title change",
                    ) { shownTrack ->
                        Text(
                            shownTrack.title,
                            color = foreground,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Slider(
                        value = scrubProgress,
                        onValueChange = {
                            scrubbing = true
                            scrubProgress = it
                        },
                        onValueChangeFinished = {
                            scrubbing = false
                            if (duration > 0L) onSeek((duration * scrubProgress).toLong())
                        },
                        enabled = duration > 0L,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primaryContainer,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MusicDockContent.copy(alpha = .22f),
                            disabledThumbColor = MusicDockContent.copy(alpha = .36f),
                            disabledActiveTrackColor = MusicDockContent.copy(alpha = .24f),
                            disabledInactiveTrackColor = MusicDockContent.copy(alpha = .12f),
                        ),
                        modifier = Modifier.fillMaxWidth().height(18.dp),
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "上一首", Modifier.size(18.dp), tint = foreground)
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(34.dp)) {
                    if (state.playbackPhase == MusicPlaybackPhase.Resolving || state.playbackPhase == MusicPlaybackPhase.Buffering) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = foreground, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (state.playbackPhase == MusicPlaybackPhase.Playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (state.playbackPhase == MusicPlaybackPhase.Playing) "暂停" else "播放",
                            Modifier.size(20.dp),
                            tint = foreground,
                        )
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.SkipNext, "下一首", Modifier.size(18.dp), tint = foreground)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Rounded.LibraryMusic, null, Modifier.padding(10.dp).size(24.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("网易云音乐", color = foreground, fontWeight = FontWeight.Black)
                    Text(
                        if (state.auth is MusicAuthState.LoggedIn) "点按打开音乐库" else "点按进入扫码登录",
                        color = secondaryForeground,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Icon(Icons.Rounded.MoreVert, null, tint = foreground.copy(alpha = .72f))
            }
        }
    }
}

@Composable
private fun Artwork(
    url: String,
    label: String,
    size: androidx.compose.ui.unit.Dp,
    trackId: String? = null,
    modifier: Modifier = Modifier,
    onRefreshArtwork: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val secureUrl = remember(url) { normalizeNeteaseMediaUrl(url) }
    var failed by remember(secureUrl) { mutableStateOf(false) }
    var refreshRequested by remember(secureUrl, trackId) { mutableStateOf(false) }
    val request: ImageRequest? = remember(secureUrl) {
        secureUrl?.let {
            ImageRequest.Builder(context)
                .data(it)
                .httpHeaders(
                    NetworkHeaders.Builder()
                        .set("User-Agent", "Mozilla/5.0 (Linux; Android) Yueti/0.11.5")
                        .set("Referer", "https://music.163.com/")
                        .set("Accept", "image/avif,image/webp,image/png,image/jpeg,*/*")
                        .build(),
                )
                .crossfade(180)
                .build()
        }
    }
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(size / 4))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            contentDescription = "$label 专辑封面",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { failed = false },
            onError = {
                failed = true
                if (!refreshRequested && !trackId.isNullOrBlank()) {
                    refreshRequested = true
                    onRefreshArtwork(trackId)
                }
            },
        )
        if (request == null || failed) {
            Icon(
                Icons.Rounded.LibraryMusic,
                contentDescription = if (failed) "$label 封面加载失败" else null,
                modifier = Modifier.size(size * .48f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val OpenOfficialQrLoginScript = """
(function(){
  function clickText(pattern){
    var nodes = document.querySelectorAll('a,button,div,span');
    for (var i=0;i<nodes.length;i++) {
      var text = (nodes[i].innerText || '').trim();
      if (pattern.test(text) && nodes[i].offsetParent !== null) { nodes[i].click(); return true; }
    }
    return false;
  }
  setTimeout(function(){
    try {
      if (window.top && typeof window.top.login === 'function') window.top.login();
      else clickText(/^登录$|立即登录/);
    } catch(_) {}
    setTimeout(function(){ try { clickText(/二维码|扫码/); } catch(_) {} }, 650);
  }, 500);
})();
"""
