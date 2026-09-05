package com.yueti.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yueti.app.data.HomeLayoutDefaults
import com.yueti.app.data.HomeLayoutStore
import com.yueti.app.data.HomeModuleId
import com.yueti.app.data.HomeModuleLayout
import com.yueti.app.data.normalizeHomeLayout
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val ControlBackground = Color(0xFF111012)
private val ControlSurface = Color(0xFF3A393C)
private val ControlSurfaceRaised = Color(0xFF4A494C)
private val ControlWhite = Color(0xFFF8F5F8)
private val ControlPurple = Color(0xFFD77BFF)
private val ControlLime = Color(0xFFD7FF72)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeControlCenter(
    state: AppUiState,
    animationsEnabled: Boolean,
    pageActive: Boolean,
    onBank: (BankType) -> Unit,
    onQuantity: (Int) -> Unit,
    onDailyGoal: (Int) -> Unit,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onAssistant: () -> Unit,
    onVocabulary: () -> Unit,
    onProfile: () -> Unit,
    onEditingChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { HomeLayoutStore(context) }
    val cachedLayout = remember { HomeLayoutStore.cachedOrNull() }
    val persisted by store.layout.collectAsStateWithLifecycle(cachedLayout ?: emptyList())
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var layouts by remember { mutableStateOf(cachedLayout ?: emptyList()) }
    var hydrated by remember { mutableStateOf(cachedLayout != null) }
    var botReady by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var selectionSheet by remember { mutableStateOf(false) }
    var goalSheet by remember { mutableStateOf(false) }
    var translatorSheet by remember { mutableStateOf(false) }
    var comingSoon by remember { mutableStateOf<HomeModuleId?>(null) }
    LaunchedEffect(persisted) {
        if (persisted.isNotEmpty()) {
            if (!editing) layouts = persisted
            hydrated = true
        }
    }
    LaunchedEffect(editing) { onEditingChanged(editing) }
    LaunchedEffect(pageActive, animationsEnabled) {
        botReady = false
        if (pageActive) {
            if (animationsEnabled) delay(180)
            botReady = true
        }
    }

    fun persist(next: List<HomeModuleLayout>, priority: HomeModuleId? = null) {
        layouts = normalizeHomeLayout(next, priority)
        scope.launch { store.save(layouts) }
    }

    Box(Modifier.fillMaxSize().background(ControlBackground).windowInsetsPadding(WindowInsets.statusBars)) {
        val contentTop by animateDpAsState(if (editing) 72.dp else 10.dp, spring(dampingRatio = .82f, stiffness = 500f), label = "edit toolbar clearance")
        BoxWithConstraints(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).padding(top = contentTop, bottom = 120.dp),
        ) {
            val gap = 10.dp
            // Keep the four-column coordinate system on tablets without inflating every logical
            // row into a huge empty square.  Phone layouts remain edge-to-edge; wide layouts use
            // a centered control-board measure that is still comfortably touch-sized.
            val boardWidth = minOf(maxWidth, 760.dp)
            val cell = (boardWidth - gap * 3) / 4
            val canvasHeight = cell * HomeLayoutDefaults.rows + gap * (HomeLayoutDefaults.rows - 1)
            AnimatedVisibility(
                visible = hydrated,
                enter = fadeIn(tween(if (animationsEnabled) 150 else 0)),
                exit = fadeOut(tween(0)),
                modifier = Modifier.width(boardWidth).height(canvasHeight).align(Alignment.TopCenter),
            ) {
                Box(Modifier.fillMaxSize()) {
                layouts.filterNot { it.hidden }.forEach { item ->
                    HomeModule(
                        item = item,
                        cell = cell,
                        gap = gap,
                        state = state,
                        editing = editing,
                        animationsEnabled = animationsEnabled,
                        // AndroidView does not reliably inherit an AnimatedContent parent alpha.
                        // Instantiate it only after the saved grid's short cross-fade settles.
                        pageActive = botReady,
                        onEnterEditing = {
                            editing = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onClick = {
                            when (item.id) {
                                HomeModuleId.Profile -> onProfile()
                                HomeModuleId.Totals -> Unit
                                HomeModuleId.DailyGoal -> goalSheet = true
                                HomeModuleId.Assistant -> onAssistant()
                                HomeModuleId.Practice -> if (state.exam != null && state.result == null) onContinue() else selectionSheet = true
                                HomeModuleId.Translator -> translatorSheet = true
                                HomeModuleId.BeijingClock -> Unit
                                HomeModuleId.Pk, HomeModuleId.EnglishSpeaking -> comingSoon = item.id
                                HomeModuleId.Vocabulary -> onVocabulary()
                            }
                        },
                        onMove = { dx, dy ->
                            val moved = item.copy(
                                x = (item.x + dx).coerceIn(0, HomeLayoutDefaults.columns - item.width),
                                y = (item.y + dy).coerceIn(0, HomeLayoutDefaults.rows - item.height),
                            )
                            persist(layouts.map { if (it.id == item.id) moved else it }, item.id)
                        },
                        onResize = { direction ->
                            val sizes = HomeLayoutDefaults.allowedSizes(item.id)
                            val current = sizes.indexOf(item.width to item.height).coerceAtLeast(0)
                            val nextSize = sizes[(current + direction).coerceIn(0, sizes.lastIndex)]
                            if (nextSize != (item.width to item.height)) {
                                val resized = item.copy(width = nextSize.first, height = nextSize.second)
                                persist(layouts.map { if (it.id == item.id) resized else it }, item.id)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onHide = {
                            if (!HomeLayoutDefaults.isLocked(item.id)) persist(layouts.map { if (it.id == item.id) it.copy(hidden = true) else it })
                        },
                    )
                }
                }
            }
        }
        AnimatedVisibility(visible = editing, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).height(54.dp),
                shape = RoundedCornerShape(20.dp), color = Color(0xFF242326), contentColor = ControlWhite,
                shadowElevation = 10.dp,
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("调整主页", Modifier.weight(1f).padding(start = 10.dp), fontWeight = FontWeight.Black)
                    IconButton(onClick = { scope.launch { store.reset() }; layouts = HomeLayoutDefaults.layouts }) { Icon(Icons.Rounded.Refresh, "恢复默认") }
                    IconButton(
                        onClick = {
                            layouts.firstOrNull { it.hidden }?.let { hidden ->
                                persist(layouts.map { if (it.id == hidden.id) it.copy(hidden = false) else it }, hidden.id)
                            }
                        },
                        enabled = layouts.any { it.hidden },
                    ) { Icon(Icons.Rounded.Add, "添加隐藏组件") }
                    Button(onClick = { editing = false }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) {
                        Icon(Icons.Rounded.Done, null); Spacer(Modifier.width(5.dp)); Text("完成")
                    }
                }
            }
        }
    }

    if (selectionSheet) {
        PracticeSelectionSheet(
            selectedBank = state.selectedBank,
            selectedQuantity = state.selectedQuantity,
            animationsEnabled = animationsEnabled,
            onDismiss = { selectionSheet = false },
            onStart = { bank, quantity ->
                onBank(bank); onQuantity(quantity); selectionSheet = false; onStart()
            },
        )
    }
    if (goalSheet) {
        GoalSelectionSheet(state.profile.dailyGoal, animationsEnabled, { goalSheet = false }) {
            onDailyGoal(it); goalSheet = false
        }
    }
    if (translatorSheet) TranslatorSheet(onDismiss = { translatorSheet = false })
    comingSoon?.let { module ->
        AlertDialog(
            onDismissRequest = { comingSoon = null },
            icon = { Icon(if (module == HomeModuleId.Pk) Icons.Rounded.EmojiEvents else Icons.Rounded.Mic, null) },
            title = { Text(if (module == HomeModuleId.Pk) "学习 PK" else "英语听说") },
            text = { Text("入口已经加入洞洞板，完整功能敬请期待。") },
            confirmButton = { TextButton(onClick = { comingSoon = null }) { Text("知道了") } },
        )
    }
}

@Composable
private fun HomeModule(
    item: HomeModuleLayout,
    cell: Dp,
    gap: Dp,
    state: AppUiState,
    editing: Boolean,
    animationsEnabled: Boolean,
    pageActive: Boolean,
    onEnterEditing: () -> Unit,
    onClick: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onResize: (Int) -> Unit,
    onHide: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var dragging by remember(item.id) { mutableStateOf(false) }
    var committingMove by remember(item.id) { mutableStateOf(false) }
    var dragX by remember(item.id) { mutableFloatStateOf(0f) }
    var dragY by remember(item.id) { mutableFloatStateOf(0f) }
    var resizing by remember(item.id) { mutableStateOf(false) }
    var resizeX by remember(item.id) { mutableFloatStateOf(0f) }
    var resizeY by remember(item.id) { mutableFloatStateOf(0f) }
    val x by animateDpAsState(
        (cell + gap) * item.x,
        if (committingMove) snap() else spring(stiffness = 520f, dampingRatio = .84f),
        label = "module x",
    )
    val y by animateDpAsState(
        (cell + gap) * item.y,
        if (committingMove) snap() else spring(stiffness = 520f, dampingRatio = .84f),
        label = "module y",
    )
    val width by animateDpAsState(cell * item.width + gap * (item.width - 1), spring(stiffness = 520f, dampingRatio = .86f), label = "module width")
    val height by animateDpAsState(cell * item.height + gap * (item.height - 1), spring(stiffness = 520f, dampingRatio = .86f), label = "module height")
    val elevation by animateDpAsState(if (editing) 14.dp else 2.dp, label = "module elevation")
    val scale by animateFloatAsState(if (editing) .975f else 1f, spring(dampingRatio = .7f), label = "module scale")
    val visualDragX by animateFloatAsState(dragX, if (dragging || committingMove) snap() else spring(dampingRatio = .78f, stiffness = 520f), label = "module drag x")
    val visualDragY by animateFloatAsState(dragY, if (dragging || committingMove) snap() else spring(dampingRatio = .78f, stiffness = 520f), label = "module drag y")
    val visualResizeX by animateFloatAsState(resizeX, if (resizing) snap() else spring(dampingRatio = .68f, stiffness = 460f), label = "module stretch x")
    val visualResizeY by animateFloatAsState(resizeY, if (resizing) snap() else spring(dampingRatio = .68f, stiffness = 460f), label = "module stretch y")
    val border by animateColorAsState(if (editing) Color.White else Color.White.copy(alpha = .16f), label = "module border")

    val needsTransformLayer = editing || dragging || resizing || committingMove ||
        scale != 1f || visualDragX != 0f || visualDragY != 0f || visualResizeX != 0f || visualResizeY != 0f
    val transformModifier = if (needsTransformLayer) {
        Modifier.graphicsLayer {
            translationX = visualDragX
            translationY = visualDragY
            transformOrigin = TransformOrigin(0f, 0f)
            scaleX = scale * (1f + visualResizeX / width.toPx().coerceAtLeast(1f)).coerceIn(.72f, 1.38f)
            scaleY = scale * (1f + visualResizeY / height.toPx().coerceAtLeast(1f)).coerceIn(.72f, 1.38f)
        }
    } else {
        Modifier
    }
    Box(
        Modifier.zIndex(if (dragging || resizing) 10f else 0f).offset(x, y).size(width, height)
            .then(transformModifier)
            .shadow(elevation, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp)).background(if (editing) ControlSurfaceRaised else ControlSurface)
            .border(if (editing) 2.dp else 1.dp, border, RoundedCornerShape(28.dp))
            .pointerInput(editing, item) {
                if (!editing) return@pointerInput
                detectDragGestures(
                    onDragStart = { dragging = true; dragX = 0f; dragY = 0f },
                    onDrag = { change, amount ->
                        if (resizing) return@detectDragGestures
                        change.consume(); dragX += amount.x; dragY += amount.y
                    },
                    onDragEnd = {
                        val cellPx = with(density) { (cell + gap).toPx() }
                        val cellsX = (dragX / cellPx).roundToInt()
                        val cellsY = (dragY / cellPx).roundToInt()
                        dragging = false
                        if (cellsX == 0 && cellsY == 0) {
                            dragX = 0f; dragY = 0f
                        } else {
                            dragX = cellsX * cellPx; dragY = cellsY * cellPx
                            scope.launch {
                                delay(80)
                                committingMove = true
                                onMove(cellsX, cellsY)
                                dragX = 0f; dragY = 0f
                                delay(20)
                                committingMove = false
                            }
                        }
                    },
                    onDragCancel = { dragging = false; dragX = 0f; dragY = 0f },
                )
            }
            .combinedClickable(enabled = !editing, onClick = onClick, onLongClick = onEnterEditing),
    ) {
        ModuleContent(
            item, state, animationsEnabled, pageActive,
            Modifier.fillMaxSize().padding(if (item.height == 1) 10.dp else if (item.width == 1) 12.dp else 16.dp),
        )
        if (editing && !HomeLayoutDefaults.isLocked(item.id)) {
            Surface(onClick = onHide, modifier = Modifier.offset((-7).dp, (-7).dp).size(38.dp), shape = CircleShape, color = Color(0xFF323236), contentColor = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(.7f))) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "隐藏组件", Modifier.size(19.dp)) }
            }
        }
        if (editing) {
            ResizeCorner(
                modifier = Modifier.align(Alignment.BottomEnd).size(54.dp).pointerInput(item) {
                    detectDragGestures(
                        onDragStart = { resizing = true; resizeX = 0f; resizeY = 0f },
                        onDrag = { change, amount -> change.consume(); resizeX += amount.x; resizeY += amount.y },
                        onDragEnd = {
                            val direction = if (resizeX + resizeY >= 0f) 1 else -1
                            resizing = false
                            onResize(direction)
                            resizeX = 0f; resizeY = 0f
                        },
                        onDragCancel = { resizing = false; resizeX = 0f; resizeY = 0f },
                    )
                },
            )
        }
    }
}

@Composable
private fun ModuleContent(item: HomeModuleLayout, state: AppUiState, animationsEnabled: Boolean, pageActive: Boolean, modifier: Modifier) {
    when (item.id) {
        HomeModuleId.Profile -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Avatar(state.profile.avatarPath, Modifier.size(if (item.width >= 4) 52.dp else 36.dp))
            Spacer(Modifier.width(if (item.width >= 4) 12.dp else 8.dp)); Column(Modifier.weight(1f)) {
                Text(state.profile.name, color = ControlWhite, style = if (item.width >= 4) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.width >= 4) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.School, null, Modifier.size(16.dp), tint = ControlPurple); Spacer(Modifier.width(5.dp)); Text(state.profile.school, color = ControlWhite.copy(.72f), maxLines = 1) }
            }
            if (item.width >= 4) Icon(Icons.Rounded.Person, null, tint = ControlPurple)
        }
        HomeModuleId.Totals -> if (item.width == 1) {
            Column(modifier, verticalArrangement = Arrangement.Center) {
                Text(state.totalAnswered.toString(), color = ControlWhite, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, maxLines = 1)
                Text("答题", color = ControlWhite.copy(.72f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        } else {
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                Text(state.totalAnswered.toString(), color = ControlWhite, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, maxLines = 1)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("累计答题", color = ControlWhite, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("正确率 ${state.totalAccuracy}%", color = ControlPurple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        HomeModuleId.DailyGoal -> if (item.width == 1) {
            Column(modifier, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(20.dp), tint = ControlLime)
                Text(state.todayAnswered.toString(), color = ControlWhite, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, maxLines = 1)
                Text("目标 ${state.profile.dailyGoal}", color = ControlWhite.copy(.68f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        } else {
            Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(22.dp), tint = ControlLime); Spacer(Modifier.width(7.dp))
                    Column(Modifier.weight(1f)) { Text("今日目标", color = ControlWhite, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, maxLines = 1); Text("连续 ${state.streakDays} 天", color = ControlWhite.copy(.68f), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    Text("${state.todayAnswered}/${state.profile.dailyGoal}", color = ControlWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1)
                }
                if (item.height > 1) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (state.todayAnswered >= state.profile.dailyGoal) "今日目标已经完成" else "再完成 ${(state.profile.dailyGoal - state.todayAnswered).coerceAtLeast(0)} 题达标",
                        color = ControlWhite.copy(.78f), style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                    )
                }
                LinearWavyProgressIndicator(progress = { state.todayProgress }, color = ControlLime, trackColor = Color.White.copy(.14f), modifier = Modifier.fillMaxWidth().height(7.dp), amplitude = { if (animationsEnabled) .45f else 0f })
            }
        }
        HomeModuleId.Assistant -> Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = ControlPurple, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(7.dp))
                Text(if (item.width == 2) "跃跃" else "跃跃 AI", color = ControlWhite, fontWeight = FontWeight.Black, maxLines = 1)
                Spacer(Modifier.weight(1f)); Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = ControlWhite.copy(.7f), modifier = Modifier.size(20.dp))
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (pageActive) {
                    EmotionBallView(BotEmotion.Idle, active = true, lite = !animationsEnabled, onTap = {}, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(54.dp), tint = ControlPurple.copy(alpha = .42f))
                }
            }
        }
        HomeModuleId.Practice -> Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(if (state.exam != null && state.result == null) "继续答题" else "开始刷题", color = ControlWhite, style = if (item.width == 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, maxLines = 1); if (item.width > 2 || item.height > 1) Text("${state.selectedBank.label} · ${state.selectedQuantity}题", color = ControlPurple, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Icon(Icons.Rounded.Tune, null, tint = ControlLime) }
            if (item.height > 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PracticeMeta("题库", state.selectedBank.label, Modifier.weight(1f))
                    PracticeMeta("题量", "${state.selectedQuantity} 题", Modifier.weight(1f))
                }
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(disabledContainerColor = ControlLime, disabledContentColor = ControlBackground)) { Text("点击组件选择并开始", fontWeight = FontWeight.Black) }
            }
        }
        HomeModuleId.Translator -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Translate, null, tint = ControlPurple)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text("AI 翻译", color = ControlWhite, fontWeight = FontWeight.Black, maxLines = 1); if (item.width >= 4) Text("快速中英互译", color = ControlWhite.copy(.68f), maxLines = 1) }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = ControlWhite.copy(.65f), modifier = Modifier.size(20.dp))
        }
        HomeModuleId.BeijingClock -> BeijingClockContent(item, modifier)
        HomeModuleId.Pk -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = ControlLime); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text("学习 PK", color = ControlWhite, fontWeight = FontWeight.Black, maxLines = 1); if (item.width >= 4) Text("敬请期待", color = ControlWhite.copy(.62f), maxLines = 1) }
        }
        HomeModuleId.EnglishSpeaking -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Mic, null, tint = ControlPurple); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text("英语听说", color = ControlWhite, fontWeight = FontWeight.Black, maxLines = 1); if (item.width >= 4) Text("敬请期待", color = ControlWhite.copy(.62f), maxLines = 1) }
        }
        HomeModuleId.Vocabulary -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Style, null, tint = ControlLime, modifier = Modifier.size(if (item.height > 1) 34.dp else 25.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text("雅思词卡", color = ControlWhite, fontWeight = FontWeight.Black, style = if (item.height > 1) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium, maxLines = 1)
                if (item.width >= 4) Text("4,794 词 · 五箱间隔复习", color = ControlWhite.copy(.68f), maxLines = 1)
                if (item.height > 1) Text("上下滑动切词 · 跃跃自动生成例句", color = ControlPurple, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, "打开雅思词卡", tint = ControlWhite.copy(.72f))
        }
    }
}

@Composable
private fun PracticeMeta(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color.White.copy(alpha = .07f), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = ControlWhite.copy(alpha = .62f), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text(value, color = ControlWhite, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun BeijingClockContent(item: HomeModuleLayout, modifier: Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }
    val time = remember(now) {
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Shanghai")).format(Instant.ofEpochMilli(now))
    }
    if (item.width == 1) {
        Column(modifier, verticalArrangement = Arrangement.Center) {
            Text(time, color = ControlWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
            Text("北京", color = ControlWhite.copy(.66f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, null, tint = ControlLime)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(time, color = ControlWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, maxLines = 1)
                Text("北京时间", color = ControlWhite.copy(.66f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ResizeCorner(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawArc(Color.White.copy(.95f), 0f, 90f, false, topLeft = Offset(size.width * .16f, size.height * .16f), size = Size(size.width * .68f, size.height * .68f), style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeSelectionSheet(
    selectedBank: BankType,
    selectedQuantity: Int,
    animationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onStart: (BankType, Int) -> Unit,
) {
    var bank by remember { mutableStateOf(selectedBank) }
    var quantity by remember { mutableIntStateOf(selectedQuantity.coerceIn(questionQuantityRange(selectedBank))) }
    val range = questionQuantityRange(bank)
    val haptics = LocalHapticFeedback.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("开始一组练习", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                BankType.entries.forEachIndexed { i, value -> SegmentedButton(selected = bank == value, onClick = { bank = value; quantity = quantity.coerceIn(questionQuantityRange(value)) }, shape = SegmentedButtonDefaults.itemShape(i, BankType.entries.size), label = { Text(value.label.removeSuffix("题库")) }) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("题量", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                RollingNumber(quantity, animationsEnabled, suffix = " 题")
            }
            Slider(
                value = quantity.toFloat(), valueRange = range.first.toFloat()..range.last.toFloat(),
                onValueChange = { value -> val next = value.roundToInt().coerceIn(range); if (next != quantity) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); quantity = next },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${range.first}题"); Text("任意题量", color = MaterialTheme.colorScheme.primary); Text("${range.last}题") }
            Button(onClick = { onStart(bank, quantity) }, modifier = Modifier.fillMaxWidth().height(58.dp)) { Text("生成试卷并开始", fontWeight = FontWeight.Black) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslatorSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("英文") }
    var result by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val targets = listOf("中文", "英文", "日文")
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 34.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Translate, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text("快捷 AI 翻译", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                targets.forEachIndexed { index, language ->
                    SegmentedButton(selected = target == language, onClick = { target = language }, shape = SegmentedButtonDefaults.itemShape(index, targets.size), label = { Text(language) })
                }
            }
            OutlinedTextField(value = source, onValueChange = { source = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 7, label = { Text("输入要翻译的内容") })
            Button(
                onClick = {
                    if (source.isBlank() || loading) return@Button
                    scope.launch {
                        loading = true
                        val saved = withContext(Dispatchers.IO) { AiCredentialStore.load(context) }
                        if (saved.isNotBlank()) DeepSeekAiGateway.configure(saved)
                        result = if (!DeepSeekAiGateway.configured) {
                            "请先在跃跃助手右上角的钥匙按钮中配置 DeepSeek API。"
                        } else {
                            DeepSeekAiGateway.chat("请把以下内容准确翻译成$target，只输出译文，不添加解释：\n$source", AssistantMode.Chat)
                                .getOrElse { it.message ?: "翻译失败，请稍后重试。" }
                        }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp), enabled = source.isNotBlank() && !loading,
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp) else Icon(Icons.Rounded.Translate, null)
                Spacer(Modifier.width(8.dp)); Text(if (loading) "正在翻译" else "开始翻译", fontWeight = FontWeight.Bold)
            }
            if (result.isNotBlank()) Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                Text(result, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalSelectionSheet(selected: Int, animationsEnabled: Boolean, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val values = listOf(10, 20, 30, 50)
    var goal by remember { mutableStateOf(selected) }
    val haptics = LocalHapticFeedback.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("调整每日目标", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("每日完成", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                RollingNumber(goal, animationsEnabled, suffix = " 题")
            }
            Slider(
                value = values.indexOf(goal).coerceAtLeast(0).toFloat(),
                onValueChange = {
                    val next = values[it.roundToInt().coerceIn(values.indices)]
                    if (next != goal) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    goal = next
                },
                valueRange = 0f..values.lastIndex.toFloat(),
                steps = 2,
            )
            Button(onClick = { onSelect(goal) }, Modifier.fillMaxWidth()) { Text("保存目标") }
        }
    }
}
