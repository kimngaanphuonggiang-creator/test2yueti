package com.yueti.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ListItem
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yueti.app.R
import com.yueti.app.data.ExamSessionEntity
import com.yueti.app.data.WrongRecordEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ResultReviewFilter { All, Wrong, Unanswered }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ResultsScreen(
    result: ExamResult,
    animationsEnabled: Boolean,
    onNavigate: (AppPage) -> Unit,
    onRetryWrong: () -> Unit,
) {
    val resultEmotion = when {
        result.score >= 90 -> BotEmotion.Celebrating
        result.score >= 60 -> BotEmotion.Happy
        else -> BotEmotion.Disappointed
    }
    val review = remember(result.exam.questions, result.exam.selections) {
        buildReviewItems(result.exam.questions, result.exam.selections)
    }
    var reviewFilter by remember { mutableStateOf(ResultReviewFilter.All) }
    var resultRevealed by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(result.exam.id, animationsEnabled) {
        if (animationsEnabled) {
            delay(120)
            resultRevealed = true
        }
    }
    val scoreReveal by animateFloatAsState(
        targetValue = if (resultRevealed) result.score / 100f else 0f,
        animationSpec = tween(if (animationsEnabled) 720 else 0),
        label = "score reveal",
    )
    val visibleReview = remember(review, reviewFilter) {
        review.filter { item ->
            when (reviewFilter) {
                ResultReviewFilter.All -> true
                ResultReviewFilter.Wrong -> item.kind == ReviewItemKind.Wrong
                ResultReviewFilter.Unanswered -> item.kind == ReviewItemKind.Unanswered
            }
        }
    }
    var actionsExpanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        Modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 20.dp, 18.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(34.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("本组完成", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.size(224.dp), contentAlignment = Alignment.Center) {
                        ScoreRing(result.score, scoreReveal)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(result.score.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("分", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(184.dp), contentAlignment = Alignment.Center) {
                        EmotionBallView(
                            emotion = resultEmotion,
                            active = animationsEnabled,
                            lite = !animationsEnabled,
                            onTap = {},
                            modifier = Modifier.size(176.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (result.passed) "干得漂亮，稳稳向前！" else "别灰心，错题正是进步地图。",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResultMetric("正确题数", result.correctCount.toString(), MaterialTheme.colorScheme.tertiary)
                    ResultMetric("错误题数", result.wrongCount.toString(), MaterialTheme.colorScheme.secondary)
                    ResultMetric("未作答", result.unansweredCount.toString(), MaterialTheme.colorScheme.outline)
                    ResultMetric("用时", formatDuration(result.elapsedSeconds), MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Text(if (review.isEmpty()) "全部答对" else "错题与解析", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 6.dp))
        }
        if (review.isEmpty()) {
            item { EmptyState("这份试卷没有错题", "继续保持，下一组也会很稳。", Icons.Rounded.CheckCircle) }
        } else {
            item(key = "review-filters", contentType = "filters") {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ResultReviewFilter.All to "全部 ${review.size}",
                        ResultReviewFilter.Wrong to "选错 ${result.wrongCount}",
                        ResultReviewFilter.Unanswered to "未答 ${result.unansweredCount}",
                    ).forEach { (value, label) ->
                        FilterChip(selected = reviewFilter == value, onClick = { reviewFilter = value }, label = { Text(label) })
                    }
                }
            }
            itemsIndexed(
                visibleReview,
                key = { _, item -> "${item.kind}-${item.question.id}" },
                contentType = { _, _ -> "review" },
            ) { _, item ->
                ReviewCard(item)
            }
        }
        item {
            if (result.wrongCount > 0) {
                Button(onClick = onRetryWrong, Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Rounded.Replay, null)
                    Spacer(Modifier.width(8.dp))
                    Text("只重练本次错题")
                }
                Spacer(Modifier.height(10.dp))
            }
            Button(onClick = { onNavigate(AppPage.WrongBook) }, Modifier.fillMaxWidth().height(56.dp)) { Text("进入错题本") }
            TextButton(onClick = { onNavigate(AppPage.Home) }, Modifier.fillMaxWidth()) { Text("返回首页") }
        }
    }
        FloatingActionButtonMenu(
            expanded = actionsExpanded,
            button = {
                ToggleFloatingActionButton(
                    checked = actionsExpanded,
                    onCheckedChange = { actionsExpanded = it },
                ) {
                    Icon(if (actionsExpanded) Icons.Rounded.Clear else Icons.Rounded.MoreVert, if (actionsExpanded) "收起快捷操作" else "展开快捷操作")
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.safeDrawing).padding(18.dp),
        ) {
            if (result.wrongCount > 0) {
                FloatingActionButtonMenuItem(
                    onClick = { actionsExpanded = false; onRetryWrong() },
                    icon = { Icon(Icons.Rounded.Replay, null) },
                    text = { Text("重练本次错题") },
                )
            }
            FloatingActionButtonMenuItem(
                onClick = { actionsExpanded = false; onNavigate(AppPage.WrongBook) },
                icon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null) },
                text = { Text("复习错题") },
            )
            FloatingActionButtonMenuItem(
                onClick = { actionsExpanded = false; onNavigate(AppPage.Stats) },
                icon = { Icon(Icons.Rounded.BarChart, null) },
                text = { Text("查看统计") },
            )
            FloatingActionButtonMenuItem(
                onClick = { actionsExpanded = false; onNavigate(AppPage.Home) },
                icon = { Icon(Icons.Rounded.Home, null) },
                text = { Text("返回首页") },
            )
        }
    }
}

@Composable
private fun ScoreRing(score: Int, progress: Float) {
    val track = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .22f)
    val active = MaterialTheme.colorScheme.tertiary
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = "本次成绩 $score 分" }) {
        val stroke = 13.dp.toPx()
        drawArc(track, -90f, 360f, false, Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(active, -90f, progress.coerceIn(0f, 1f) * 360f, false, Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun ResultMetric(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).background(color, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ReviewCard(item: ReviewItem) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = if (item.kind == ReviewItemKind.Wrong) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (item.kind == ReviewItemKind.Wrong) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (item.kind == ReviewItemKind.Wrong) "选错" else "未作答",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(item.question.prompt, style = MaterialTheme.typography.titleLarge)
            Text("你的答案：${item.selectedAnswer?.toString() ?: "未作答"}", color = MaterialTheme.colorScheme.error)
            Text("正确答案：${item.question.answer}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("解析", fontWeight = FontWeight.Bold)
                    Text(item.question.explanation)
                }
            }
        }
    }
}

private enum class WrongFilter { All, Pinned, Recent }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WrongBookScreen(
    records: List<WrongRecordEntity>,
    animationsEnabled: Boolean,
    onPin: (WrongRecordEntity) -> Unit,
    onDelete: (WrongRecordEntity) -> Unit,
    onNotice: (String) -> Unit,
    onCardGestureLock: (Boolean) -> Unit = {},
    onPageSwipe: (Int) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(WrongFilter.All) }
    var refreshing by remember { mutableStateOf(false) }
    var detailRecord by remember { mutableStateOf<WrongRecordEntity?>(null) }
    val scope = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState()
    val searchTextState = rememberTextFieldState(query)
    LaunchedEffect(searchTextState) {
        snapshotFlow { searchTextState.text.toString() }.collect { query = it }
    }
    val now = System.currentTimeMillis()
    val visibleRecords = remember(records, query, filter) {
        records.filter { record ->
            val matchesQuery = query.isBlank() || listOf(
                record.prompt,
                record.selectedAnswer?.toString().orEmpty(),
                record.correctAnswer.toString(),
                record.explanation,
            ).any { it.contains(query.trim(), ignoreCase = true) }
            val matchesFilter = when (filter) {
                WrongFilter.All -> true
                WrongFilter.Pinned -> record.isPinned
                WrongFilter.Recent -> now - record.wrongAt <= 7L * 24 * 60 * 60 * 1000
            }
            matchesQuery && matchesFilter
        }
    }
    var entranceComplete by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            delay(520)
            entranceComplete = true
        }
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            scope.launch {
                delay(480)
                refreshing = false
                onNotice("本地错题已是最新")
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        Modifier.widthIn(max = 760.dp).fillMaxWidth().fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 20.dp, 18.dp, 148.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                Modifier.pointerInput(Unit) {
                    var dragDistance = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onDragEnd = {
                            if (abs(dragDistance) >= 48.dp.toPx()) {
                                onPageSwipe(if (dragDistance < 0f) 1 else -1)
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            dragDistance += amount
                            change.consume()
                        },
                    )
                },
            ) {
                Text("错题本", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black, modifier = Modifier.semantics { heading() })
                Text("右滑置顶，左滑删除；可同时置顶多题，最近置顶排在最前。", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .72f))
            }
            Spacer(Modifier.height(14.dp))
            AppBarWithSearch(
                state = searchBarState,
                inputField = {
                    SearchBarDefaults.InputField(
                        textFieldState = searchTextState,
                        searchBarState = searchBarState,
                        onSearch = {
                            query = searchTextState.text.toString()
                            scope.launch { searchBarState.animateToCollapsed() }
                        },
                        placeholder = { Text("搜索题目、答案或解析") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { searchTextState.edit { replace(0, length, "") } }) { Icon(Icons.Rounded.Clear, "清空搜索") }
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    WrongFilter.All to "全部",
                    WrongFilter.Pinned to "已置顶",
                    WrongFilter.Recent to "近7天",
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = filter == value,
                        onClick = { filter = value },
                        label = { Text(label) },
                        leadingIcon = if (filter == value) ({ Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp)) }) else null,
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .82f),
                            iconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .82f),
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
            if (query.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                InputChip(
                    selected = true,
                    onClick = { searchTextState.edit { replace(0, length, "") } },
                    label = { Text("关键词：$query · ${visibleRecords.size} 道") },
                    trailingIcon = { Icon(Icons.Rounded.Clear, "清除关键词", Modifier.size(18.dp)) },
                )
            }
        }
        if (records.isEmpty()) {
            item { EmptyState("还没有错题", "完成一份试卷后，真正选错的题会保存在这里。", Icons.AutoMirrored.Rounded.MenuBook) }
        } else if (visibleRecords.isEmpty()) {
            item { EmptyState("没有匹配的错题", "换个关键词或筛选条件再试试。", Icons.Rounded.Search) }
        } else {
            itemsIndexed(
                items = visibleRecords,
                key = { _, item -> item.id },
                contentType = { _, _ -> "wrong-record" },
            ) { index, record ->
                Box(
                    Modifier.animateItem(
                        fadeInSpec = null,
                        placementSpec = tween(durationMillis = 280),
                        fadeOutSpec = null,
                    ),
                ) {
                    WrongBookFlyIn(index, animationsEnabled && !entranceComplete) {
                        SwipeableWrongRecord(
                            index = index + 1,
                            record = record,
                            onPin = onPin,
                            onDelete = onDelete,
                            onGestureLock = onCardGestureLock,
                            onDetails = { detailRecord = record },
                        )
                    }
                }
            }
        }
    }
    }
    }
    detailRecord?.let { record ->
        ModalBottomSheet(onDismissRequest = { detailRecord = null }) {
            WrongRecordDetail(record)
        }
    }
}

@Composable
private fun SwipeableWrongRecord(
    index: Int,
    record: WrongRecordEntity,
    onPin: (WrongRecordEntity) -> Unit,
    onDelete: (WrongRecordEntity) -> Unit,
    onGestureLock: (Boolean) -> Unit,
    onDetails: () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(positionalThreshold = { it * .28f })
    val scope = rememberCoroutineScope()
    var actionInFlight by remember(record.id) { mutableStateOf(false) }
    SwipeToDismissBox(
        modifier = Modifier.pointerInput(record.id) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                onGestureLock(true)
                try {
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                } finally {
                    onGestureLock(false)
                }
            }
        },
        state = state,
        gesturesEnabled = !actionInFlight,
        onDismiss = { direction ->
            if (actionInFlight) return@SwipeToDismissBox
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    actionInFlight = true
                    scope.launch {
                        settleBeforeReorder(
                            settle = { state.reset() },
                            reorder = { onPin(record) },
                        )
                        actionInFlight = false
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> onDelete(record)
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        backgroundContent = {
            val pinning = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                Modifier.fillMaxSize().background(
                    if (pinning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                    RoundedCornerShape(28.dp),
                ).padding(horizontal = 24.dp),
                contentAlignment = if (pinning) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (pinning) Icons.Rounded.PushPin else Icons.Rounded.Delete, null)
                    Text(if (pinning) if (record.isPinned) "取消置顶" else "置顶" else "删除", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) {
        WrongRecordCard(index, record, onPin, onDelete, onDetails)
    }
}

internal suspend fun settleBeforeReorder(
    settle: suspend () -> Unit,
    reorder: () -> Unit,
) {
    settle()
    reorder()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WrongRecordCard(
    index: Int,
    record: WrongRecordEntity,
    onPin: (WrongRecordEntity) -> Unit,
    onDelete: (WrongRecordEntity) -> Unit,
    onDetails: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth()
            .combinedClickable(onClick = onDetails, onLongClick = { confirmDelete = true }),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val indexColor = if (record.isPinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primaryContainer
                val indexContent = if (record.isPinned) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimaryContainer
                Box(Modifier.background(indexColor, RoundedCornerShape(12.dp))) {
                    Text("%02d".format(index), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = indexContent, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.prompt, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("点击查看完整解析", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
                if (record.isPinned) Icon(Icons.Rounded.PushPin, "已置顶", tint = MaterialTheme.colorScheme.primary)
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "更多操作") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(if (record.isPinned) "取消置顶" else "置顶") }, leadingIcon = { Icon(Icons.Rounded.PushPin, null) }, onClick = { menu = false; onPin(record) })
                        DropdownMenuItem(text = { Text("删除") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { menu = false; confirmDelete = true })
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnswerBadge("你的答案", record.selectedAnswer?.toString() ?: "未作答", true, Modifier.weight(1f))
                AnswerBadge("正确答案", record.correctAnswer.toString(), false, Modifier.weight(1f))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这道错题？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = { Button(onClick = { confirmDelete = false; onDelete(record) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun WrongRecordDetail(record: WrongRecordEntity) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        Text(record.prompt, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(formatDateTime(record.wrongAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        ListItem(
            overlineContent = { Text("当时选择") },
            leadingContent = { Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.error) },
        ) { Text(record.selectedAnswer?.toString() ?: "未作答") }
        ListItem(
            overlineContent = { Text("正确答案") },
            leadingContent = { Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary) },
        ) { Text(record.correctAnswer.toString()) }
        Spacer(Modifier.height(12.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("解析", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text(record.explanation)
            }
        }
    }
}

@Composable
private fun AnswerBadge(label: String, value: String, isError: Boolean, modifier: Modifier = Modifier) {
    val background = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val foreground = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Column(modifier.background(background, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = foreground)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = foreground)
    }
}

@Composable
private fun WrongBookFlyIn(index: Int, animateEntrance: Boolean, content: @Composable () -> Unit) {
    val motion = rememberMaterialMotionTokens()
    val shouldAnimate = animateEntrance && index < 6
    val progress = remember { Animatable(if (shouldAnimate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (shouldAnimate) {
            delay(index * 52L)
            progress.animateTo(1f, tween(motion.expressive, easing = motion.emphasizedEasing))
        }
    }
    val entranceModifier = Modifier.graphicsLayer {
        val current = progress.value
        alpha = current
        translationX = (1f - current) * if (index % 2 == 0) 96f else -96f
        scaleX = .96f + .04f * current
        scaleY = .96f + .04f * current
    }
    Box(Modifier.fillMaxWidth().then(entranceModifier)) { content() }
}

private data class DailyAccuracy(val date: LocalDate, val correct: Int, val total: Int) {
    val percent: Int? get() = if (total == 0) null else correct * 100 / total
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsScreen(sessions: List<ExamSessionEntity>, animationsEnabled: Boolean) {
    var days by remember { mutableIntStateOf(7) }
    var customStart by remember { mutableStateOf<LocalDate?>(null) }
    var customEnd by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val rangeEnd = customEnd ?: today
    val rangeStart = customStart ?: rangeEnd.minusDays((days - 1).toLong())
    val points = remember(sessions, rangeStart, rangeEnd) { buildDailyAccuracy(sessions, rangeStart, rangeEnd) }
    val rangeSessions = remember(sessions, rangeStart, rangeEnd) {
        sessions.filter { session ->
            val date = Instant.ofEpochMilli(session.completedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(rangeStart) && !date.isAfter(rangeEnd)
        }
    }
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = rangeStart.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        initialSelectedEndDateMillis = rangeEnd.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    var selected by remember(points) {
        mutableIntStateOf(points.indexOfLast { it.percent != null }.takeIf { it >= 0 } ?: points.lastIndex)
    }
    val practiced = points.filter { it.percent != null }
    val periodCorrect = practiced.sumOf { it.correct }
    val periodTotal = practiced.sumOf { it.total }
    val periodAverage = if (periodTotal == 0) null else periodCorrect * 100 / periodTotal
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("学习统计", fontWeight = FontWeight.Black) },
                subtitle = { Text("按日期查看正答率") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { scaffoldPadding ->
    Box(Modifier.fillMaxSize().padding(scaffoldPadding), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        Modifier.widthIn(max = 840.dp).fillMaxWidth().fillMaxHeight(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 12.dp, 18.dp, 148.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    listOf(7, 30).forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = customStart == null && days == option,
                            onClick = { days = option; customStart = null; customEnd = null },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            modifier = Modifier.weight(1f),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .82f),
                            ),
                            label = { Text("近 $option 天") },
                        )
                    }
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Rounded.DateRange, "选择日期范围", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            if (customStart != null) {
                Text(
                    "${rangeStart.format(DateTimeFormatter.ofPattern("M月d日"))}—${rangeEnd.format(DateTimeFormatter.ofPattern("M月d日"))}",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .72f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121014)),
                shape = RoundedCornerShape(32.dp),
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("正确率趋势", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                            Text("练习日连线 · 空白日留出断点", color = Color(0xFFD8C2FF))
                        }
                        Surface(color = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), horizontalAlignment = Alignment.End) {
                                Text("区间平均", style = MaterialTheme.typography.labelSmall)
                                Text(periodAverage?.let { "$it%" } ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    AccuracyChart(points, selected, animationsEnabled) { selected = it }
                    Spacer(Modifier.height(14.dp))
                    val point = points.getOrNull(selected)
                    val pointSummary = if (point?.percent == null) {
                        "${point?.date?.format(DateTimeFormatter.ofPattern("M月d日")) ?: ""} · 当天没有练习"
                    } else {
                        "${point.date.format(DateTimeFormatter.ofPattern("M月d日"))} · ${point.percent}%（${point.correct}/${point.total}）"
                    }
                    val chartTooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = {
                            RichTooltip(title = { Text("日期详情") }) {
                                Text(pointSummary)
                            }
                        },
                        state = chartTooltipState,
                    ) {
                        Surface(color = Color(0xFF28222D), contentColor = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                                Spacer(Modifier.width(10.dp))
                                Text(pointSummary, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        item {
            val total = rangeSessions.sumOf { it.questionCount }
            val correct = rangeSessions.sumOf { it.correctCount }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("完成试卷", rangeSessions.size.toString(), MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
                StatTile("累计题数", total.toString(), MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
                StatTile("总正答率", if (total == 0) "0%" else "${correct * 100 / total}%", MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
                }
        }
        if (rangeSessions.isEmpty()) item { EmptyState("这段时间没有数据", "完成一份试卷后，折线图会从对应日期开始记录。", Icons.Rounded.BarChart) }
    }
    }
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val startMillis = pickerState.selectedStartDateMillis
                    val endMillis = pickerState.selectedEndDateMillis ?: startMillis
                    if (startMillis != null && endMillis != null) {
                        val selectedEnd = Instant.ofEpochMilli(endMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        val selectedStart = Instant.ofEpochMilli(startMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        customEnd = selectedEnd
                        customStart = if (ChronoUnit.DAYS.between(selectedStart, selectedEnd) > 89) selectedEnd.minusDays(89) else selectedStart
                    }
                    showDatePicker = false
                }) { Text("应用") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.fillMaxHeight(.82f))
        }
    }
}

@Composable
private fun AccuracyChart(points: List<DailyAccuracy>, selected: Int, animationsEnabled: Boolean, onSelected: (Int) -> Unit) {
    val lineColor = MaterialTheme.colorScheme.tertiary
    val pointColor = Color.White
    val grid = Color.White.copy(alpha = .12f)
    val noData = Color.White.copy(alpha = .22f)
    val motion = rememberMaterialMotionTokens()
    val reveal = remember(points) { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(points, animationsEnabled) {
        if (animationsEnabled) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(motion.expressive, easing = motion.emphasizedEasing))
        } else reveal.snapTo(1f)
    }
    val selectedPulse by animateFloatAsState(
        targetValue = if (selected in points.indices) 1f else 0f,
        animationSpec = tween(motion.quick, easing = motion.emphasizedEasing),
        label = "selected chart point",
    )
    val summary = points.joinToString("；") { "${it.date.monthValue}月${it.date.dayOfMonth}日${it.percent?.let { p -> "$p%" } ?: "无练习"}" }
    Column(Modifier.fillMaxWidth().semantics {
        contentDescription = "每日正答率折线图：$summary"
        customActions = listOf(
            CustomAccessibilityAction("选择前一天") { if (selected > 0) { onSelected(selected - 1); true } else false },
            CustomAccessibilityAction("选择后一天") { if (selected < points.lastIndex) { onSelected(selected + 1); true } else false },
        )
    }) {
    Row(Modifier.fillMaxWidth()) {
    Column(Modifier.width(40.dp).height(220.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
        listOf("100", "50", "0").forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .62f)) }
    }
    Canvas(
        Modifier.weight(1f).height(220.dp)
            .pointerInput(points) { detectTapGestures { offset -> if (points.isNotEmpty()) onSelected(((offset.x / size.width) * points.lastIndex).toInt().coerceIn(points.indices)) } },
    ) {
        val left = 10.dp.toPx(); val right = 10.dp.toPx(); val top = 12.dp.toPx(); val bottom = 12.dp.toPx()
        val w = size.width - left - right; val h = size.height - top - bottom
        repeat(3) { step ->
            val y = top + h * step / 2f
            drawLine(grid, Offset(left, y), Offset(left + w, y), 1.dp.toPx())
        }
        val locations = points.mapIndexed { index, point ->
            val x = if (points.size == 1) left + w / 2 else left + w * index / points.lastIndex
            point.percent?.let { percent ->
                val targetY = top + h * (100 - percent) / 100f
                Offset(x, top + h + (targetY - (top + h)) * reveal.value)
            }
        }
        if (selected in points.indices) {
            val selectedX = if (points.size == 1) left + w / 2 else left + w * selected / points.lastIndex
            drawLine(
                color = lineColor.copy(alpha = .22f * selectedPulse),
                start = Offset(selectedX, top),
                end = Offset(selectedX, top + h),
                strokeWidth = 1.dp.toPx(),
            )
        }
        var segmentPoints = mutableListOf<Offset>()
        fun drawSegment() {
            if (segmentPoints.size > 1) {
                val path = Path().apply { moveTo(segmentPoints.first().x, segmentPoints.first().y) }
                for (i in 1 until segmentPoints.size) {
                    val previous = segmentPoints[i - 1]
                    val current = segmentPoints[i]
                    val midpoint = (previous.x + current.x) / 2f
                    path.cubicTo(midpoint, previous.y, midpoint, current.y, current.x, current.y)
                }
                drawPath(path, lineColor, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            }
            segmentPoints = mutableListOf()
        }
        locations.forEachIndexed { index, location ->
            if (location == null) {
                drawSegment()
                val x = if (points.size == 1) left + w / 2 else left + w * index / points.lastIndex
                drawCircle(noData, 3.dp.toPx(), Offset(x, top + h))
            } else {
                segmentPoints += location
                if (index == selected) {
                    drawCircle(lineColor, 11.dp.toPx(), location)
                    drawCircle(pointColor, 5.dp.toPx(), location)
                } else {
                    drawCircle(Color(0xFF121014), 6.dp.toPx(), location)
                    drawCircle(lineColor, 6.dp.toPx(), location, style = Stroke(3.dp.toPx()))
                }
            }
        }
        drawSegment()
    }
    }
    if (points.isNotEmpty()) {
        val labelIndices = if (points.size <= 7) points.indices.toList() else listOf(0, points.lastIndex / 4, points.lastIndex / 2, points.lastIndex * 3 / 4, points.lastIndex).distinct()
        Row(Modifier.fillMaxWidth().padding(start = 40.dp, top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            labelIndices.forEach { index -> Text("${points[index].date.monthValue}/${points[index].date.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .68f)) }
        }
    }
    }
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = color, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun buildDailyAccuracy(sessions: List<ExamSessionEntity>, start: LocalDate, end: LocalDate): List<DailyAccuracy> {
    val zone = ZoneId.systemDefault()
    val days = ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(0)
    return (0..days).map { offset ->
        val date = start.plusDays(offset.toLong())
        val sameDay = sessions.filter { Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate() == date }
        DailyAccuracy(date, sameDay.sumOf { it.correctCount }, sameDay.sumOf { it.questionCount })
    }
}

private fun formatDuration(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)
private fun formatDateTime(epoch: Long): String = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"))
