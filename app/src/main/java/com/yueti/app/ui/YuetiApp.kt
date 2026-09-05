/*
THESIS: 用 Material 3 Expressive 的强形状、清晰层级与克制动效，让整卷练习有连续的推进感。
OWN-WORLD: 50 道 0–100 加减法、本机成绩、错题复盘和日期统计共同构成“跃题”学习世界。
STORY: 首次资料 → 选择题库/题量 → 整卷作答 → 统一交卷 → 成绩与错题复盘。
FORM: 黑/紫主场，青柠负责行动与进度，珊瑚负责提醒；浮动胶囊导航和大圆 CTA 沿用参考图。
MOTION: 品牌揭示、背景缓动、容器推进、共享轴换题、答题卡展开、结果表情一次播放。
*/
package com.yueti.app.ui

import android.animation.ValueAnimator
import android.Manifest
import android.graphics.BitmapFactory
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailItemDefaults
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yueti.app.R
import com.yueti.app.music.MusicOverlayMode
import com.yueti.app.music.MusicTrack
import com.yueti.app.music.MusicUiState
import com.yueti.app.music.MusicViewModel
import com.yueti.app.music.MusicGestureAxis
import com.yueti.app.music.MusicFanAnchor
import com.yueti.app.music.MusicNavRevealState
import com.yueti.app.music.musicChromeTransform
import com.yueti.app.music.musicRevealProgressForDrag
import com.yueti.app.music.resolveMusicRevealRelease
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import kotlin.math.abs

private data class NavItem(val label: String, val icon: ImageVector, val page: AppPage)

private val navItems = listOf(
    NavItem("首页", Icons.Rounded.Home, AppPage.Home),
    NavItem("错题本", Icons.AutoMirrored.Rounded.MenuBook, AppPage.WrongBook),
    NavItem("统计", Icons.Rounded.BarChart, AppPage.Stats),
    NavItem("我的", Icons.Rounded.Person, AppPage.Profile),
)

private val mainPages = navItems.map(NavItem::page)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun YuetiApp(
    viewModel: YuetiViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val musicState by musicViewModel.state.collectAsStateWithLifecycle()
    var musicFanAnchor by remember { mutableStateOf<MusicFanAnchor?>(null) }
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    val motion = rememberMaterialMotionTokens()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = mainPages.indexOf(state.page).coerceAtLeast(0),
        pageCount = { mainPages.size },
    )
    var pagerGestureLocked by remember { mutableStateOf(false) }
    var homeEditing by remember { mutableStateOf(false) }
    var musicNavRevealState by remember { mutableStateOf(MusicNavRevealState()) }
    var brandVisible by remember { mutableStateOf(true) }
    val view = LocalView.current
    val context = LocalContext.current
    val musicPermissionPreferences = remember {
        context.getSharedPreferences("music_permission_state", android.content.Context.MODE_PRIVATE)
    }
    var pendingMusicAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        pendingMusicAction?.invoke()
        pendingMusicAction = null
    }
    val runMusicAction: (() -> Unit) -> Unit = { action ->
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !musicPermissionPreferences.getBoolean("notification_asked", false)
        if (needsPermission) {
            pendingMusicAction = action
            musicPermissionPreferences.edit().putBoolean("notification_asked", true).apply()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val musicFullScreen = musicState.overlay in setOf(MusicOverlayMode.Library, MusicOverlayMode.Search)
    val lightStatusBars = !darkTheme && (musicFullScreen || state.page in setOf(
        AppPage.Onboarding, AppPage.Results, AppPage.Stats, AppPage.Profile,
    ))
    val lightNavigationBars = !darkTheme && (musicFullScreen || state.page in setOf(
        AppPage.Onboarding, AppPage.Results, AppPage.Exam, AppPage.Profile,
    ))

    SideEffect {
        if (!view.isInEditMode) {
            WindowCompat.getInsetsController(view.context.findActivity().window, view).apply {
                isAppearanceLightStatusBars = lightStatusBars
                isAppearanceLightNavigationBars = lightNavigationBars
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(1800)
        brandVisible = false
    }

    LaunchedEffect(state.notice) {
        state.notice?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeNotice()
        }
    }

    LaunchedEffect(musicState.notice) {
        musicState.notice?.let { message ->
            snackbarHostState.showSnackbar(message)
            musicViewModel.consumeNotice()
        }
    }

    LaunchedEffect(musicFullScreen) {
        if (!musicFullScreen) {
            if (musicState.queue.playing) {
                musicNavRevealState = musicNavRevealState.copy(progress = 1f, settledExpanded = true)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                val page = mainPages[pageIndex]
                if (state.page in mainPages && state.page != page) viewModel.navigate(page)
            }
    }

    LaunchedEffect(state.page) {
        val target = mainPages.indexOf(state.page)
        if (target >= 0 && pagerState.settledPage != target && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(target)
        }
    }

    val targetBackground = when (state.page) {
        AppPage.Exam -> MaterialTheme.colorScheme.secondaryContainer
        AppPage.Results -> MaterialTheme.colorScheme.tertiaryContainer
        AppPage.Onboarding -> MaterialTheme.colorScheme.primaryContainer
        AppPage.Profile -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.background
    }
    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = androidx.compose.animation.core.tween(
            if (animationsEnabled) motion.expressive else 0,
            easing = motion.emphasizedEasing,
        ),
        label = "page background",
    )

    BackHandler(enabled = state.page != AppPage.Home && state.page != AppPage.Onboarding && state.page != AppPage.Exam) {
        viewModel.navigate(AppPage.Home)
    }

    BackHandler(enabled = musicState.overlay != MusicOverlayMode.Closed) {
        if (musicState.overlay == MusicOverlayMode.Search) {
            musicViewModel.setOverlay(MusicOverlayMode.Library)
        } else {
            musicViewModel.setOverlay(MusicOverlayMode.Closed)
        }
    }

    Box(Modifier.fillMaxSize().background(background)) {
        val blurMusicBackground = (musicState.overlay == MusicOverlayMode.Search ||
            musicState.overlay == MusicOverlayMode.ArcQueue) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !Build.MANUFACTURER.equals("Meizu", ignoreCase = true) &&
            !Build.BRAND.equals("Meizu", ignoreCase = true)
        val musicBlurRadius by animateDpAsState(
            targetValue = if (blurMusicBackground && animationsEnabled) 12.dp else 0.dp,
            animationSpec = androidx.compose.animation.core.tween(if (animationsEnabled) 260 else 0),
            label = "music background blur",
        )
        SharedTransitionLayout(Modifier.fillMaxSize()) {
        val musicContainerState = rememberSharedContentState(key = "music-container")
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 720.dp
            val showMainNavigation = state.page in mainPages
            val selectMainPage: (AppPage) -> Unit = { page ->
                val target = mainPages.indexOf(page)
                if (target >= 0) scope.launch { pagerState.animateScrollToPage(target) }
            }
            AnimatedContent(
                targetState = showMainNavigation,
                modifier = Modifier.fillMaxSize().blur(musicBlurRadius),
                transitionSpec = {
                    // Tool destinations and the editable home grid do not share geometry.
                    // Scaling here made AndroidView-backed Emotion Ball escape its card for a
                    // frame and made saved modules look as if they resized from defaults.
                    fadeIn(androidx.compose.animation.core.tween(if (animationsEnabled) 180 else 0)) togetherWith
                        fadeOut(androidx.compose.animation.core.tween(if (animationsEnabled) 90 else 0))
                },
                label = "main practice container transform",
            ) { mainVisible ->
            Row(Modifier.fillMaxSize()) {
                if (mainVisible && isTablet) {
                    FloatingRail(mainPages[pagerState.currentPage], state.wrongRecords.size, selectMainPage)
                    Spacer(Modifier.width(8.dp))
                }
                if (mainVisible) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !pagerGestureLocked && !homeEditing &&
                            mainPages[pagerState.currentPage] != AppPage.WrongBook,
                        beyondViewportPageCount = mainPages.lastIndex,
                        key = { mainPages[it] },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    ) { pageIndex ->
                        MainPageContent(
                            page = mainPages[pageIndex],
                            // Vertical Home scrolling can briefly mark the parent pager as
                            // scrolling before gesture direction is resolved. Keep platform
                            // views alive until a different destination actually settles.
                            isPageActive = pagerState.settledPage == pageIndex,
                            state = state,
                            animationsEnabled = animationsEnabled,
                            isTablet = isTablet,
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState,
                            onPagerGestureLock = { pagerGestureLocked = it },
                            onHomeEditing = { homeEditing = it },
                            onNavigateAdjacent = { delta ->
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        (pageIndex + delta).coerceIn(mainPages.indices),
                                    )
                                }
                            },
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = state.page,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        transitionSpec = {
                            fadeIn(androidx.compose.animation.core.tween(if (animationsEnabled) 160 else 0)) togetherWith
                                fadeOut(androidx.compose.animation.core.tween(if (animationsEnabled) 80 else 0))
                        },
                        label = "modal destination",
                    ) { page ->
                        when (page) {
                        AppPage.Onboarding -> ProfileEditor(
                            title = "先认识一下你",
                            subtitle = "资料只保存在这台设备上，之后可随时修改。",
                            profile = state.profile,
                            submitLabel = "进入跃题",
                            onSave = viewModel::saveProfile,
                        )
                        AppPage.Exam -> state.exam?.let {
                            ExamScreen(
                                exam = it,
                                animationsEnabled = animationsEnabled,
                                onNavigate = viewModel::navigate,
                                onSelect = viewModel::selectAnswer,
                                onQuestion = viewModel::goToQuestion,
                                onToggleFlag = viewModel::toggleQuestionFlag,
                                onSubmit = viewModel::submitExam,
                            )
                        }
                        AppPage.Results -> state.result?.let {
                            ResultsScreen(it, animationsEnabled, viewModel::navigate, viewModel::startReviewExam)
                        }
                        AppPage.Assistant -> AssistantScreenV2(
                            onBack = { viewModel.navigate(AppPage.Home) },
                        )
                        AppPage.Scanner -> ScannerScreen(
                            onBack = { viewModel.navigate(AppPage.Home) },
                            onNotice = viewModel::postNotice,
                        )
                        AppPage.Graph -> GraphScreen(
                            onBack = { viewModel.navigate(AppPage.Home) },
                            onNotice = viewModel::postNotice,
                        )
                        AppPage.Vocabulary -> VocabularyScreen(
                            animationsEnabled = animationsEnabled,
                            onBack = { viewModel.navigate(AppPage.Home) },
                            onNotice = viewModel::postNotice,
                        )
                        AppPage.Licenses -> LicensesScreen { viewModel.navigate(AppPage.Profile) }
                        else -> Unit
                        }
                    }
                }
            }
            }

            if (!isTablet && musicState.overlay == MusicOverlayMode.ArcQueue) {
                MusicOverlayHost(
                    state = musicState,
                    animationsEnabled = animationsEnabled,
                    onDismiss = { musicViewModel.setOverlay(MusicOverlayMode.Closed) },
                    onCookie = musicViewModel::acceptWebCookie,
                    onRefresh = musicViewModel::refreshPlaylists,
                    onOpenPlaylist = musicViewModel::openPlaylist,
                    onPlay = { track -> runMusicAction { musicViewModel.playVisible(track) } },
                    onSearch = musicViewModel::search,
                    onSearchMode = { musicViewModel.setOverlay(MusicOverlayMode.Search) },
                    onLibraryMode = { musicViewModel.setOverlay(MusicOverlayMode.Library) },
                    onToggle = { runMusicAction(musicViewModel::togglePlayback) },
                    onPrevious = musicViewModel::previous,
                    onNext = musicViewModel::next,
                    onPlayAt = musicViewModel::playAt,
                    onLogout = musicViewModel::logout,
                    fanAnchor = musicFanAnchor,
                    onCompatibilityDnsChanged = musicViewModel::setCompatibilityDnsEnabled,
                    onRefreshArtwork = musicViewModel::refreshArtwork,
                )
            }

            AnimatedVisibility(
                visible = showMainNavigation && !isTablet && !musicFullScreen,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(androidx.compose.animation.core.tween(if (animationsEnabled) 120 else 0)),
                exit = fadeOut(
                    androidx.compose.animation.core.tween(
                        durationMillis = if (animationsEnabled) 100 else 0,
                        delayMillis = if (animationsEnabled) 420 else 0,
                    ),
                ),
            ) navVisibility@{
                val navigationShift by animateFloatAsState(
                    targetValue = when {
                        homeEditing -> 420f
                        else -> 0f
                    },
                    animationSpec = spring(dampingRatio = .82f, stiffness = 420f),
                    label = "home edit navigation sink",
                )
                FloatingBottomNavigation(
                    selected = mainPages[pagerState.currentPage],
                    wrongCount = state.wrongRecords.size,
                    onSelect = selectMainPage,
                    pagerState = pagerState,
                    onTool = viewModel::navigate,
                    musicState = musicState,
                    revealProgress = musicNavRevealState.progress,
                    onRevealProgress = {
                        musicNavRevealState = musicNavRevealState.copy(
                            progress = it,
                            settledExpanded = it >= .999f,
                        )
                    },
                    onOpenMusicPage = { musicViewModel.setOverlay(MusicOverlayMode.Library) },
                    onToggleMusic = { runMusicAction(musicViewModel::togglePlayback) },
                    onPreviousMusic = musicViewModel::previous,
                    onNextMusic = musicViewModel::next,
                    onSeekMusic = musicViewModel::seekTo,
                    onArcMusic = { musicViewModel.setOverlay(MusicOverlayMode.ArcQueue) },
                    onArtworkAnchorChanged = { anchor ->
                        if (musicFanAnchor != anchor) musicFanAnchor = anchor
                    },
                    onRefreshArtwork = musicViewModel::refreshArtwork,
                    leftContainerModifier = Modifier.sharedBounds(
                        sharedContentState = musicContainerState,
                        animatedVisibilityScope = this@navVisibility,
                        boundsTransform = { _, _ ->
                            androidx.compose.animation.core.tween(
                                durationMillis = if (animationsEnabled) 520 else 0,
                                easing = motion.emphasizedEasing,
                            )
                        },
                    ),
                    modifier = Modifier.graphicsLayer {
                        translationY = navigationShift
                        alpha = 1f - (navigationShift / 260f).coerceIn(0f, 1f)
                    },
                )
            }

            AnimatedVisibility(
                visible = musicFullScreen,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(androidx.compose.animation.core.tween(if (animationsEnabled) 180 else 0, delayMillis = if (animationsEnabled) 300 else 0)),
                exit = fadeOut(androidx.compose.animation.core.tween(if (animationsEnabled) 120 else 0)),
            ) musicPageVisibility@{
                MusicOverlayHost(
                    state = musicState,
                    animationsEnabled = animationsEnabled,
                    onDismiss = { musicViewModel.setOverlay(MusicOverlayMode.Closed) },
                    onCookie = musicViewModel::acceptWebCookie,
                    onRefresh = musicViewModel::refreshPlaylists,
                    onOpenPlaylist = musicViewModel::openPlaylist,
                    onPlay = { track -> runMusicAction { musicViewModel.playVisible(track) } },
                    onSearch = musicViewModel::search,
                    onSearchMode = { musicViewModel.setOverlay(MusicOverlayMode.Search) },
                    onLibraryMode = { musicViewModel.setOverlay(MusicOverlayMode.Library) },
                    onToggle = { runMusicAction(musicViewModel::togglePlayback) },
                    onPrevious = musicViewModel::previous,
                    onNext = musicViewModel::next,
                    onPlayAt = musicViewModel::playAt,
                    onLogout = musicViewModel::logout,
                    fanAnchor = musicFanAnchor,
                    onCompatibilityDnsChanged = musicViewModel::setCompatibilityDnsEnabled,
                    onRefreshArtwork = musicViewModel::refreshArtwork,
                    modifier = Modifier.fillMaxSize().sharedBounds(
                        sharedContentState = musicContainerState,
                        animatedVisibilityScope = this@musicPageVisibility,
                        boundsTransform = { _, _ ->
                            androidx.compose.animation.core.tween(
                                durationMillis = if (animationsEnabled) 520 else 0,
                                easing = motion.emphasizedEasing,
                            )
                        },
                    ),
                )
            }
        }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 104.dp),
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = brandVisible,
            enter = fadeIn(),
            exit = fadeOut(androidx.compose.animation.core.tween(if (animationsEnabled) 220 else 0)),
        ) {
            BrandReveal(animationsEnabled)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainPageContent(
    page: AppPage,
    isPageActive: Boolean,
    state: AppUiState,
    animationsEnabled: Boolean,
    isTablet: Boolean,
    viewModel: YuetiViewModel,
    snackbarHostState: SnackbarHostState,
    onPagerGestureLock: (Boolean) -> Unit,
    onHomeEditing: (Boolean) -> Unit,
    onNavigateAdjacent: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    when (page) {
        AppPage.Home -> HomeScreen(
            state = state,
            animationsEnabled = animationsEnabled,
            pageActive = isPageActive,
            expanded = isTablet,
            onBank = viewModel::setBank,
            onQuantity = viewModel::setQuantity,
            onDailyGoal = viewModel::setDailyGoal,
            onStart = viewModel::startExam,
            onContinue = { viewModel.navigate(AppPage.Exam) },
            onAssistant = { viewModel.navigate(AppPage.Assistant) },
            onVocabulary = { viewModel.navigate(AppPage.Vocabulary) },
            onScanner = { viewModel.navigate(AppPage.Scanner) },
            onGraph = { viewModel.navigate(AppPage.Graph) },
            onProfile = { viewModel.navigate(AppPage.Profile) },
            onEditingChanged = onHomeEditing,
        )
        AppPage.WrongBook -> WrongBookScreen(
            records = state.wrongRecords,
            animationsEnabled = animationsEnabled,
            onPin = { record ->
                viewModel.togglePinned(record.id)
                scope.launch {
                    val action = snackbarHostState.showSnackbar(
                        message = if (record.isPinned) "已取消置顶" else "已置顶",
                        actionLabel = "撤销",
                    )
                    if (action == SnackbarResult.ActionPerformed) viewModel.togglePinned(record.id)
                }
            },
            onDelete = { record ->
                viewModel.deleteWrong(record.id)
                scope.launch {
                    val action = snackbarHostState.showSnackbar("已删除这道错题", "撤销")
                    if (action == SnackbarResult.ActionPerformed) viewModel.restoreWrong(record)
                }
            },
            onNotice = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
            onCardGestureLock = onPagerGestureLock,
            onPageSwipe = onNavigateAdjacent,
        )
        AppPage.Stats -> StatsScreen(state.sessions, animationsEnabled)
        AppPage.Profile -> ProfileEditor(
            title = "我的资料",
            subtitle = "头像、姓名和学校会显示在首页。",
            profile = state.profile,
            submitLabel = "保存修改",
            onSave = viewModel::saveProfile,
            onLicenses = { viewModel.navigate(AppPage.Licenses) },
        )
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingBottomNavigation(
    selected: AppPage,
    wrongCount: Int,
    onSelect: (AppPage) -> Unit,
    pagerState: PagerState,
    onTool: (AppPage) -> Unit,
    musicState: MusicUiState,
    revealProgress: Float,
    onRevealProgress: (Float) -> Unit,
    onOpenMusicPage: () -> Unit,
    onToggleMusic: () -> Unit,
    onPreviousMusic: () -> Unit,
    onNextMusic: () -> Unit,
    onSeekMusic: (Long) -> Unit,
    onArcMusic: () -> Unit,
    onArtworkAnchorChanged: (MusicFanAnchor) -> Unit,
    onRefreshArtwork: (String) -> Unit,
    leftContainerModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    var dragDistanceX by remember { mutableFloatStateOf(0f) }
    var dragDistanceY by remember { mutableFloatStateOf(0f) }
    var gestureAxis by remember { mutableStateOf(MusicGestureAxis.Undecided) }
    var revealSettleJob by remember { mutableStateOf<Job?>(null) }
    var toolsExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val chrome = musicChromeTransform(revealProgress)
    val navigationSurfaceColor = Color(0xF21A171B)
    val currentRevealProgress by rememberUpdatedState(revealProgress)
    val musicArcLift by animateFloatAsState(
        targetValue = if (musicState.overlay == MusicOverlayMode.ArcQueue) -20f else 0f,
        animationSpec = spring(dampingRatio = .66f, stiffness = 360f),
        label = "left music navigation lift",
    )
    val musicArcNavAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = .82f, stiffness = 560f),
        label = "left navigation to fan pivot",
    )
    val plusComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.nav_plus))
    val plusProgress by animateFloatAsState(if (toolsExpanded) 1f else 0f, label = "tool plus morph")
    Row(
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 18.dp, vertical = 12.dp)
            .widthIn(max = 430.dp).fillMaxWidth().height(chrome.heightDp.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight().then(leftContainerModifier)
                .graphicsLayer {
                    translationY = musicArcLift.dp.toPx()
                    alpha = musicArcNavAlpha
                    scaleX = .96f + .04f * musicArcNavAlpha
                    scaleY = .96f + .04f * musicArcNavAlpha
                }
                .pointerInput(pagerState.currentPage, musicState.overlay, musicState.queue.currentIndex) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        revealSettleJob?.cancel()
                        val startReveal = currentRevealProgress
                        val velocityTracker = VelocityTracker().apply {
                            addPosition(down.uptimeMillis, down.position)
                        }
                        var lastPosition = down.position
                        var lastUptime = down.uptimeMillis
                        var longPressTriggered = false
                        dragDistanceX = 0f
                        dragDistanceY = 0f
                        gestureAxis = MusicGestureAxis.Undecided
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            lastUptime = change.uptimeMillis
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            if (!change.pressed) {
                                if (!longPressTriggered && gestureAxis == MusicGestureAxis.Undecided &&
                                    lastUptime - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis &&
                                    musicState.queue.current != null
                                ) {
                                    longPressTriggered = true
                                    onArcMusic()
                                    change.consume()
                                }
                                break
                            }
                            val amount = change.position - lastPosition
                            lastPosition = change.position
                            dragDistanceX += amount.x
                            dragDistanceY += amount.y
                            if (gestureAxis == MusicGestureAxis.Undecided &&
                                abs(dragDistanceX) + abs(dragDistanceY) > viewConfiguration.touchSlop
                            ) {
                                gestureAxis = if (abs(dragDistanceY) > abs(dragDistanceX)) {
                                    MusicGestureAxis.Vertical
                                } else {
                                    MusicGestureAxis.Horizontal
                                }
                            }
                            if (!longPressTriggered && gestureAxis == MusicGestureAxis.Undecided &&
                                lastUptime - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis &&
                                musicState.queue.current != null
                            ) {
                                longPressTriggered = true
                                onArcMusic()
                            }
                            if (gestureAxis == MusicGestureAxis.Vertical) {
                                change.consume()
                                val dragDp = with(density) { dragDistanceY.toDp().value }
                                onRevealProgress(musicRevealProgressForDrag(startReveal, dragDp))
                            } else if (gestureAxis == MusicGestureAxis.Horizontal) {
                                change.consume()
                            }
                        }
                        if (!longPressTriggered && gestureAxis == MusicGestureAxis.Vertical) {
                            val velocityDp = with(density) { velocityTracker.calculateVelocity().y.toDp().value }
                            val releasedProgress = with(density) {
                                musicRevealProgressForDrag(startReveal, dragDistanceY.toDp().value)
                            }
                            val release = resolveMusicRevealRelease(releasedProgress, velocityDp)
                            revealSettleJob = scope.launch {
                                androidx.compose.animation.core.Animatable(releasedProgress).animateTo(
                                    release.target,
                                    spring(dampingRatio = .62f, stiffness = 320f),
                                    initialVelocity = release.initialVelocity,
                                ) { onRevealProgress(value) }
                            }
                        } else if (!longPressTriggered && gestureAxis == MusicGestureAxis.Horizontal &&
                            abs(dragDistanceX) > 48.dp.toPx()
                        ) {
                            if (currentRevealProgress > .72f && musicState.queue.current != null) {
                                if (dragDistanceX < 0) onNextMusic() else onPreviousMusic()
                            } else if (musicState.overlay == MusicOverlayMode.Closed) {
                                val delta = if (dragDistanceX < 0) 1 else -1
                                val target = (pagerState.currentPage + delta).coerceIn(mainPages.indices)
                                onSelect(mainPages[target])
                            }
                        }
                        dragDistanceX = 0f
                        dragDistanceY = 0f
                        gestureAxis = MusicGestureAxis.Undecided
                    }
                },
            shape = RoundedCornerShape(chrome.cornerDp.dp),
            color = navigationSurfaceColor,
            contentColor = Color.White,
            tonalElevation = 6.dp,
        ) {
            Box(Modifier.fillMaxSize().padding(5.dp)) {
                MiniMusicBar(
                    state = musicState,
                    revealProgress = chrome.playerReveal,
                    rotationX = chrome.playerRotationX,
                    onOpenPage = onOpenMusicPage,
                    onToggle = onToggleMusic,
                    onPrevious = onPreviousMusic,
                    onNext = onNextMusic,
                    onSeek = onSeekMusic,
                    onArc = onArcMusic,
                    onArtworkAnchorChanged = onArtworkAnchorChanged,
                    onRefreshArtwork = onRefreshArtwork,
                )
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(54.dp).graphicsLayer {
                        translationY = chrome.iconTranslationYDp.dp.toPx()
                        scaleX = chrome.iconScale
                        scaleY = chrome.iconScale
                        alpha = chrome.iconAlpha
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                navItems.forEach { item ->
                    val selectedItem = selected == item.page
                    val iconScale by animateFloatAsState(
                        targetValue = if (selectedItem) 1.12f else .96f,
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = .72f, stiffness = 520f),
                        label = "navigation icon scale",
                    )
                    val itemColor by animateColorAsState(
                        if (selectedItem) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        label = "navigation selected surface",
                    )
                    val iconColor by animateColorAsState(
                        if (selectedItem) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFE8E0EA),
                        label = "navigation icon color",
                    )
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Surface(
                            onClick = { onSelect(item.page) },
                            modifier = Modifier.size(48.dp).semantics { contentDescription = item.label },
                            shape = RoundedCornerShape(15.dp),
                            color = itemColor,
                            contentColor = iconColor,
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                BadgedBox(badge = {
                                    if (item.page == AppPage.WrongBook && wrongCount > 0) Badge { Text(wrongCount.coerceAtMost(99).toString()) }
                                }) {
                                    Icon(item.icon, null, Modifier.size(25.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }, tint = iconColor)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        Box(Modifier.align(Alignment.Bottom)) {
            Surface(
                onClick = { toolsExpanded = !toolsExpanded },
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(19.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .94f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.semantics { contentDescription = "打开学习工具" }) {
                    LottieAnimation(plusComposition, progress = { plusProgress }, modifier = Modifier.size(34.dp))
                }
            }
            DropdownMenu(expanded = toolsExpanded, onDismissRequest = { toolsExpanded = false }, modifier = Modifier.width(230.dp)) {
                DropdownMenuItem(text = { Text("文档扫描") }, leadingIcon = { Icon(Icons.Rounded.DocumentScanner, null) }, onClick = { toolsExpanded = false; onTool(AppPage.Scanner) })
                DropdownMenuItem(text = { Text("函数图像") }, leadingIcon = { Icon(Icons.Rounded.ShowChart, null) }, onClick = { toolsExpanded = false; onTool(AppPage.Graph) })
                DropdownMenuItem(text = { Text("雅思词卡") }, leadingIcon = { Icon(Icons.Rounded.Style, null) }, onClick = { toolsExpanded = false; onTool(AppPage.Vocabulary) })
                DropdownMenuItem(text = { Text("敬请期待") }, leadingIcon = { Icon(Icons.Rounded.HourglassEmpty, null) }, enabled = false, onClick = {})
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FloatingRail(selected: AppPage, wrongCount: Int, onSelect: (AppPage) -> Unit) {
    val railState = rememberWideNavigationRailState(WideNavigationRailValue.Expanded)
    val scope = rememberCoroutineScope()
    WideNavigationRail(
        modifier = Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp).clip(RoundedCornerShape(36.dp)),
        state = railState,
        colors = androidx.compose.material3.WideNavigationRailDefaults.colors(
            containerColor = Color(0xF21A171B),
            contentColor = Color.White,
        ),
        header = {
            IconButton(onClick = { scope.launch { railState.toggle() } }) {
                Icon(
                    Icons.AutoMirrored.Rounded.MenuOpen,
                    if (railState.currentValue == WideNavigationRailValue.Expanded) "收起导航" else "展开导航",
                    tint = Color.White,
                )
            }
        },
    ) {
        navItems.forEach { item ->
            val tooltipState = rememberTooltipState()
            WideNavigationRailItem(
                selected = selected == item.page,
                onClick = { onSelect(item.page) },
                icon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.End),
                        tooltip = {
                            RichTooltip(title = { Text(item.label) }) {
                                Text("切换到${item.label}")
                            }
                        },
                        state = tooltipState,
                    ) {
                        BadgedBox(
                            badge = { if (item.page == AppPage.WrongBook && wrongCount > 0) Badge { Text(wrongCount.coerceAtMost(99).toString()) } },
                        ) { Icon(item.icon, item.label) }
                    }
                },
                label = { Text(item.label) },
                railExpanded = railState.currentValue == WideNavigationRailValue.Expanded,
                colors = WideNavigationRailItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColorTopIconPosition = MaterialTheme.colorScheme.secondary,
                    selectedTextColorStartIconPosition = MaterialTheme.colorScheme.secondary,
                    selectedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = Color(0xFFD7CDD9),
                    unselectedTextColor = Color(0xFFD7CDD9),
                ),
            )
        }
    }
}

@Composable
private fun ProfileEditor(
    title: String,
    subtitle: String,
    profile: com.yueti.app.data.UserProfile,
    submitLabel: String,
    onSave: (String, String, android.net.Uri?, String) -> Unit,
    onLicenses: (() -> Unit)? = null,
) {
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var school by remember(profile.school) { mutableStateOf(profile.school) }
    var avatarUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { avatarUri = it }

    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp).padding(bottom = 104.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Avatar(
            path = profile.avatarPath,
            uri = avatarUri,
            modifier = Modifier.size(104.dp),
        )
        androidx.compose.material3.TextButton(onClick = {
            picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text("选择头像") }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") }, leadingIcon = { Icon(Icons.Rounded.Person, null) }, singleLine = true, modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = school, onValueChange = { school = it }, label = { Text("学校") }, leadingIcon = { Icon(Icons.Rounded.School, null) }, singleLine = true, modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onSave(name, school, avatarUri, profile.avatarPath) },
            enabled = name.isNotBlank() && school.isNotBlank(),
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Text(submitLabel) }
        if (onLicenses != null) {
            androidx.compose.material3.TextButton(onClick = onLicenses) { Text("开源许可与非商业说明") }
        }
    }
}

@Composable
internal fun Avatar(path: String, modifier: Modifier = Modifier, uri: android.net.Uri? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(path, uri) {
        when {
            uri != null -> context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            path.isNotBlank() -> BitmapFactory.decodeFile(path)
            else -> null
        }
    }
    Surface(modifier = modifier.clip(CircleShape), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), "用户头像", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, "默认头像", Modifier.size(42.dp)) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BrandReveal(animationsEnabled: Boolean) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.brand_reveal))
    val progress by animateLottieCompositionAsState(composition, isPlaying = animationsEnabled, iterations = 1)
    Surface(Modifier.fillMaxSize(), color = Color(0xFF1B0732)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            LottieAnimation(composition, progress = { if (animationsEnabled) progress else 1f }, modifier = Modifier.size(180.dp))
            LoadingIndicator(modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(10.dp))
            Text("跃题", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Black)
            Text("每一题，都是向前一步", color = Color(0xFFD7FF72))
        }
    }
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> error("Activity context not found")
}
