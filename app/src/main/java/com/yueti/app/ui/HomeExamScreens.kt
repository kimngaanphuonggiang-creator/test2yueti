package com.yueti.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FlagCircle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SplitButton
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.SecureFlagPolicy
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yueti.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: AppUiState,
    animationsEnabled: Boolean,
    pageActive: Boolean,
    expanded: Boolean,
    onBank: (BankType) -> Unit,
    onQuantity: (Int) -> Unit,
    onDailyGoal: (Int) -> Unit,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onAssistant: () -> Unit,
    onScanner: () -> Unit,
    onGraph: () -> Unit,
) {
    val motion = rememberMaterialMotionTokens()
    var goalSheetVisible by remember { mutableStateOf(false) }
    var heroReady by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(Unit) { heroReady = true }
    val heroProgress by animateFloatAsState(
        targetValue = if (heroReady) 1f else 0f,
        animationSpec = tween(motion.expressive, easing = motion.emphasizedEasing),
        label = "home hero entrance",
    )
    val activeExam = state.exam?.takeIf { state.result == null }
    val hasActiveExam = activeExam != null
    val displayedBank = activeExam?.bank ?: state.selectedBank
    val displayedQuantity = activeExam?.questions?.size ?: state.selectedQuantity
    val primaryLabel = if (hasActiveExam) "继续答题" else "开始刷题"
    val primaryAction = if (hasActiveExam) onContinue else onStart
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HomeOverviewCard(
            state = state,
            animationsEnabled = animationsEnabled && pageActive,
            onGoal = { goalSheetVisible = true },
            modifier = Modifier.widthIn(max = if (expanded) 920.dp else 720.dp).fillMaxWidth().padding(horizontal = if (expanded) 24.dp else 18.dp),
        )
        LazyColumn(
            Modifier.widthIn(max = if (expanded) 920.dp else 720.dp).fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = if (expanded) 24.dp else 18.dp, end = if (expanded) 24.dp else 18.dp, top = 14.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(if (expanded) 390.dp else 330.dp),
                    shape = RoundedCornerShape(36.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Box {
                        EmotionBallView(
                            emotion = if (hasActiveExam) BotEmotion.Working else BotEmotion.Idle,
                            active = pageActive,
                            lite = !animationsEnabled,
                            onTap = onAssistant,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                        )
                        Surface(onClick = onAssistant, modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .9f)) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Chat, null); Spacer(Modifier.width(8.dp)); Text("和跃跃互动", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onScanner, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.DocumentScanner, null); Spacer(Modifier.width(6.dp)); Text("扫描文稿") }
                    OutlinedButton(onClick = onGraph, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.ShowChart, null); Spacer(Modifier.width(6.dp)); Text("函数图像") }
                }
            }
            item {
                PracticeHero(
                    background = null,
                    heroProgress = heroProgress,
                    animationsEnabled = false,
                    displayedQuantity = displayedQuantity,
                    displayedBank = displayedBank,
                    primaryLabel = primaryLabel,
                    description = if (hasActiveExam) "继续未完成的练习" else "开始${state.selectedQuantity}题${state.selectedBank.label}",
                    primaryAction = primaryAction,
                    quantityOptions = QuestionBank.quantities(displayedBank),
                    quantityOptionsEnabled = !hasActiveExam,
                    onQuantity = onQuantity,
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                )
            }
            item { PracticeSelectors(displayedBank, displayedQuantity, hasActiveExam, onBank, onQuantity) }
        }
    }
    if (goalSheetVisible) {
        DailyGoalSheet(
            selected = state.profile.dailyGoal,
            onDismiss = { goalSheetVisible = false },
            onSelect = {
                onDailyGoal(it)
                goalSheetVisible = false
            },
        )
    }
}

@Composable
private fun HomeOverviewCard(state: AppUiState, animationsEnabled: Boolean, onGoal: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(state.profile.avatarPath, modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(state.profile.school, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("跃题", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HomeMetric("累计答题", state.totalAnswered.toString())
                Box(Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outlineVariant))
                HomeMetric("累计正答率", "${state.totalAccuracy}%")
                Box(Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outlineVariant))
                HomeMetric("连续学习", "${state.streakDays}天")
            }
            Surface(onClick = onGoal, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text("今日 ${state.todayAnswered}/${state.profile.dailyGoal} 题 · 连续 ${state.streakDays} 天", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Icon(Icons.Rounded.Edit, "调整目标", Modifier.size(20.dp))
                    }
                    LinearWavyProgressIndicator(progress = { state.todayProgress }, modifier = Modifier.fillMaxWidth().height(8.dp), amplitude = { if (animationsEnabled) .55f else 0f })
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(state: AppUiState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Avatar(state.profile.avatarPath, modifier = Modifier.size(58.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("你好，${state.profile.name}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.School, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(4.dp))
                Text(state.profile.school, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .72f))
            }
        }
        Text("跃题", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun HomeMetricsCard(state: AppUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(32.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            HomeMetric("累计答题", state.totalAnswered.toString())
            Box(Modifier.width(1.dp).height(44.dp).background(MaterialTheme.colorScheme.outlineVariant))
            HomeMetric("累计正答率", "${state.totalAccuracy}%")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DailyGoalCard(state: AppUiState, animationsEnabled: Boolean, onEdit: () -> Unit) {
    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("今日节奏", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(
                        "${state.todayAnswered} / ${state.profile.dailyGoal} 题 · 连续 ${state.streakDays} 天",
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .76f),
                    )
                }
                Icon(Icons.Rounded.Edit, "调整每日目标")
            }
            LinearWavyProgressIndicator(
                progress = { state.todayProgress },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .16f),
                amplitude = { if (animationsEnabled) .68f else 0f },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyGoalSheet(selected: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val goals = listOf(10, 20, 30, 50)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("设置每日目标", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("完成任意整份试卷后，今日进度会自动累计。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                goals.forEachIndexed { index, goal ->
                    SegmentedButton(
                        selected = selected == goal,
                        onClick = { onSelect(goal) },
                        shape = SegmentedButtonDefaults.itemShape(index, goals.size),
                        label = { Text("$goal") },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PracticeHero(
    background: com.airbnb.lottie.LottieComposition?,
    heroProgress: Float,
    animationsEnabled: Boolean,
    displayedQuantity: Int,
    displayedBank: BankType,
    primaryLabel: String,
    description: String,
    primaryAction: () -> Unit,
    quantityOptions: List<Int>,
    quantityOptionsEnabled: Boolean,
    onQuantity: (Int) -> Unit,
    modifier: Modifier,
) {
    var optionsExpanded by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val useStableControls = requiresStableMaterialControls(
        screenWidthDp = configuration.screenWidthDp,
        fontScale = LocalDensity.current.fontScale,
    )
    Card(
        modifier = modifier.graphicsLayer {
            alpha = heroProgress
            translationY = (1f - heroProgress) * 42f
            scaleX = .97f + .03f * heroProgress
            scaleY = .97f + .03f * heroProgress
        },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF110F12)),
        shape = RoundedCornerShape(34.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            LottieAnimation(
                background,
                iterations = if (animationsEnabled) com.airbnb.lottie.compose.LottieConstants.IterateForever else 1,
                isPlaying = animationsEnabled,
                modifier = Modifier.fillMaxSize(),
            )
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.Start) {
                Text("今晚练 $displayedQuantity 题", style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.semantics { heading() })
                Text("${displayedBank.label} · 0–100", color = Color(0xFFD8C2FF))
                Spacer(Modifier.weight(1f))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val leadingWidth = (maxWidth - 76.dp).coerceAtLeast(180.dp)
                    if (useStableControls) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = primaryAction,
                                modifier = Modifier.weight(1f).height(72.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary,
                                ),
                            ) {
                                Text(primaryLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(10.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, description)
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = { optionsExpanded = !optionsExpanded },
                                enabled = quantityOptionsEnabled,
                                modifier = Modifier.width(64.dp).height(72.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.ArrowDropDown, "展开开始选项")
                                }
                            }
                        }
                    } else SplitButton(
                        leadingButton = {
                            SplitButtonDefaults.LeadingButton(
                                onClick = primaryAction,
                                modifier = Modifier.height(72.dp).width(leadingWidth),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary,
                                ),
                            ) {
                                Text(primaryLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(10.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, description)
                            }
                        },
                        trailingButton = {
                            SplitButtonDefaults.TrailingButton(
                                checked = optionsExpanded,
                                onCheckedChange = { optionsExpanded = it },
                                modifier = Modifier.height(72.dp),
                                enabled = quantityOptionsEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary,
                                ),
                            ) {
                                Icon(Icons.Rounded.ArrowDropDown, "展开开始选项")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = optionsExpanded,
                        onDismissRequest = { optionsExpanded = false },
                        modifier = Modifier.align(Alignment.BottomEnd),
                    ) {
                        Text(
                            "切换题量",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        quantityOptions.forEach { quantity ->
                            DropdownMenuItem(
                                text = { Text("$quantity 题") },
                                trailingIcon = { if (quantity == displayedQuantity) Icon(Icons.Rounded.Check, null) },
                                onClick = { onQuantity(quantity); optionsExpanded = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PracticeSelectors(
    displayedBank: BankType,
    displayedQuantity: Int,
    hasActiveExam: Boolean,
    onBank: (BankType) -> Unit,
    onQuantity: (Int) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val useStableControls = requiresStableMaterialControls(
        screenWidthDp = configuration.screenWidthDp,
        fontScale = LocalDensity.current.fontScale,
    )
    val quickQuantities = QuestionBank.quantities(displayedBank).let { values ->
        if (LocalDensity.current.fontScale > 1.15f) values.take(3) else values
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BankDropdown(displayedBank, onBank, Modifier.weight(1.25f), enabled = !hasActiveExam)
            QuantityDropdown(displayedQuantity, QuestionBank.quantities(displayedBank), onQuantity, Modifier.weight(.75f), enabled = !hasActiveExam)
        }
        Text("快捷题量", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .78f))
        if (useStableControls) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickQuantities.forEach { quantity ->
                    FilterChip(
                        selected = quantity == displayedQuantity,
                        onClick = { if (!hasActiveExam) onQuantity(quantity) },
                        label = { Text("$quantity 题") },
                        leadingIcon = if (quantity == displayedQuantity) {
                            { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                        } else null,
                        enabled = !hasActiveExam,
                    )
                }
            }
        } else {
            ButtonGroup(
                overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                quickQuantities.forEach { quantity ->
                    toggleableItem(
                        checked = quantity == displayedQuantity,
                        label = "$quantity 题",
                        onCheckedChange = { checked -> if (checked && !hasActiveExam) onQuantity(quantity) },
                        enabled = !hasActiveExam,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankDropdown(value: BankType, onChange: (BankType) -> Unit, modifier: Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { if (enabled) expanded = !expanded }, modifier) {
        Surface(
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth().clickable(enabled = enabled) { expanded = true },
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value.label, Modifier.weight(1f), maxLines = 1)
                Icon(if (enabled) Icons.Rounded.ArrowDropDown else Icons.Rounded.Lock, if (enabled) null else "进行中的试卷不可修改")
            }
        }
        DropdownMenu(expanded, { expanded = false }) {
            BankType.entries.forEach { item ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = { onChange(item); expanded = false },
                    trailingIcon = { if (item == value) Icon(Icons.Rounded.Check, null) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuantityDropdown(value: Int, values: List<Int>, onChange: (Int) -> Unit, modifier: Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { if (enabled) expanded = !expanded }, modifier) {
        Surface(
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth().clickable(enabled = enabled) { expanded = true },
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$value 题", Modifier.weight(1f), maxLines = 1)
                Icon(if (enabled) Icons.Rounded.ArrowDropDown else Icons.Rounded.Lock, if (enabled) null else "进行中的试卷不可修改")
            }
        }
        DropdownMenu(expanded, { expanded = false }) {
            values.forEach { item ->
                androidx.compose.material3.DropdownMenuItem(text = { Text("$item 题") }, onClick = { onChange(item); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExamScreen(
    exam: ExamState,
    animationsEnabled: Boolean,
    onNavigate: (AppPage) -> Unit,
    onSelect: (String, Int) -> Unit,
    onQuestion: (Int) -> Unit,
    onToggleFlag: () -> Unit,
    onSubmit: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = exam.index, pageCount = { exam.questions.size })
    var answerSheet by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var transitionPhase by remember { mutableStateOf(ExamTransitionPhase.Entering) }
    var autoAdvanceJob by remember { mutableStateOf<Job?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val elapsed = ((now - exam.startedAt) / 1000).coerceAtLeast(0)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        delay(if (animationsEnabled) 520 else 0)
        transitionPhase = ExamTransitionPhase.Answering
    }
    LaunchedEffect(exam.startedAt) {
        while (true) { delay(1000); now = System.currentTimeMillis() }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page -> onQuestion(page) }
    }

    fun moveToQuestion(index: Int) {
        autoAdvanceJob?.cancel()
        scope.launch {
            pagerState.animateScrollToPage(index.coerceIn(exam.questions.indices))
        }
    }

    fun submitFromSheet() {
        scope.launch {
            sheetState.hide()
            answerSheet = false
            if (exam.answeredCount < exam.questions.size) {
                confirmSubmit = true
            } else {
                transitionPhase = ExamTransitionPhase.Submitting
                onSubmit()
            }
        }
    }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { confirmExit = true }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "退出练习", tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(exam.bank.label, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    Text("第 ${exam.index + 1} / ${exam.questions.size} 题", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .75f))
                }
                TimerCircle(elapsed, animationsEnabled)
            }
            LinearProgressIndicator(
                progress = { (exam.index + 1f) / exam.questions.size },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .18f),
            )
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { exam.questions[it].id },
                modifier = Modifier.weight(1f).widthIn(max = 840.dp).fillMaxWidth(),
            ) { page ->
                val question = exam.questions[page]
                QuestionPane(
                    question = question,
                    selected = exam.selections[question.id],
                    animationsEnabled = animationsEnabled,
                    entering = transitionPhase == ExamTransitionPhase.Entering,
                    onSelect = { answer ->
                        onSelect(question.id, answer)
                        autoAdvanceJob?.cancel()
                        autoAdvanceJob = scope.launch {
                            delay(350)
                            if (pagerState.settledPage != page || pagerState.isScrollInProgress) return@launch
                            if (page < exam.questions.lastIndex) {
                                pagerState.animateScrollToPage(page + 1)
                            } else {
                                answerSheet = true
                            }
                        }
                    },
                )
            }
        }

        ExamDock(
            canPrevious = pagerState.currentPage > 0,
            canNext = pagerState.currentPage < exam.questions.lastIndex,
            answered = exam.answeredCount,
            total = exam.questions.size,
            isFlagged = exam.questions[pagerState.currentPage].id in exam.flaggedQuestionIds,
            onPrevious = { moveToQuestion(pagerState.currentPage - 1) },
            onSheet = { answerSheet = true },
            onFlag = onToggleFlag,
            onNext = { moveToQuestion(pagerState.currentPage + 1) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        DelayedLoading(exam.submitting, animationsEnabled)
    }

    if (answerSheet) {
        ModalBottomSheet(
            onDismissRequest = { answerSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            properties = ModalBottomSheetProperties(
                isAppearanceLightStatusBars = false,
                isAppearanceLightNavigationBars = true,
                securePolicy = SecureFlagPolicy.Inherit,
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = true,
            ),
        ) {
            AnswerSheet(
                exam = exam,
                onQuestion = { moveToQuestion(it); answerSheet = false },
                onNextUnanswered = { exam.nextUnansweredIndex()?.let { moveToQuestion(it); answerSheet = false } },
                onSubmit = ::submitFromSheet,
            )
        }
    }
    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            icon = { Icon(Icons.AutoMirrored.Rounded.Assignment, null) },
            title = { Text("还有 ${exam.questions.size - exam.answeredCount} 题未作答") },
            text = { Text("未作答题将计入成绩，但会在结果页标记出来。") },
            confirmButton = { Button(onClick = { confirmSubmit = false; transitionPhase = ExamTransitionPhase.Submitting; onSubmit() }) { Text("仍然交卷") } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("继续答题") } },
        )
    }
    BackHandler { confirmExit = true }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("退出本次练习？") },
            text = { Text("已选择的答案会暂时保留；返回首页后可继续作答。") },
            confirmButton = { Button(onClick = { confirmExit = false; onNavigate(AppPage.Home) }) { Text("退出练习") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("继续答题") } },
        )
    }
}

@Composable
private fun QuestionPane(
    question: ArithmeticQuestion,
    selected: Int?,
    animationsEnabled: Boolean,
    entering: Boolean,
    onSelect: (Int) -> Unit,
) {
    val motion = rememberMaterialMotionTokens()
    val entranceAlpha by animateFloatAsState(
        if (entering && animationsEnabled) .72f else 1f,
        tween(motion.standard, easing = motion.emphasizedEasing),
        label = "question entrance alpha",
    )
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
            .graphicsLayer { alpha = entranceAlpha; translationY = (1f - entranceAlpha) * 48f }
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp).padding(bottom = 112.dp),
    ) {
        Text("计算下面的结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Text(question.prompt, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(30.dp))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.forEachIndexed { index, value ->
                val isSelected = selected == value
                var optionEntered by remember(question.id, index) { mutableStateOf(!entering || !animationsEnabled) }
                LaunchedEffect(question.id, entering, animationsEnabled) {
                    if (entering && animationsEnabled) delay(index * 50L)
                    optionEntered = true
                }
                val optionEntrance by animateFloatAsState(
                    if (optionEntered) 1f else 0f,
                    tween(motion.standard, easing = motion.emphasizedEasing),
                    label = "answer option entrance",
                )
                val optionColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    tween(if (animationsEnabled) motion.quick else 0),
                    label = "answer color",
                )
                val corner by animateDpAsState(
                    if (isSelected) 18.dp else 24.dp,
                    tween(if (animationsEnabled) motion.quick else 0, easing = motion.emphasizedEasing),
                    label = "answer shape",
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                        .graphicsLayer { alpha = optionEntrance; translationX = (1f - optionEntrance) * 36f }
                        .selectable(selected = isSelected, onClick = { onSelect(value) }, role = Role.RadioButton),
                    color = optionColor,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(corner),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface, modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(('A' + index).toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(value.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        AnimatedVisibility(isSelected, enter = fadeIn(tween(motion.quick)), exit = fadeOut(tween(motion.quick))) {
                            Icon(Icons.Rounded.Check, null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TimerCircle(elapsedSeconds: Long, animationsEnabled: Boolean) {
    val minute = elapsedSeconds / 60
    val second = elapsedSeconds % 60
    val tooltipState = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { RichTooltip { Text("仅记录累计用时，本练习不限时") } },
        state = tooltipState,
    ) {
    Box(Modifier.size(58.dp).semantics { contentDescription = "已用时 ${minute}分${second}秒，无时间限制" }, contentAlignment = Alignment.Center) {
        if (animationsEnabled) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize().padding(3.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = Color.Transparent,
                strokeWidth = 3.dp,
            )
        } else {
            Box(Modifier.fillMaxSize().border(3.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .62f), CircleShape))
        }
        AnimatedContent(
            targetState = "%02d:%02d".format(minute, second),
            transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(80)) },
            label = "elapsed time",
        ) { value ->
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExamDock(
    canPrevious: Boolean,
    canNext: Boolean,
    answered: Int,
    total: Int,
    isFlagged: Boolean,
    onPrevious: () -> Unit,
    onSheet: () -> Unit,
    onFlag: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(14.dp).widthIn(max = 520.dp),
    ) {
        IconButton(onClick = onPrevious, enabled = canPrevious) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "上一题")
        }
        IconButton(onClick = onFlag) {
            Icon(
                if (isFlagged) Icons.Rounded.FlagCircle else Icons.Rounded.Flag,
                if (isFlagged) "取消稍后检查" else "标记稍后检查",
                tint = if (isFlagged) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
        Button(onClick = onSheet, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Icon(Icons.AutoMirrored.Rounded.Assignment, null)
            Spacer(Modifier.width(6.dp))
            Text("答题卡 $answered/$total")
        }
        IconButton(onClick = onNext, enabled = canNext) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, "下一题")
        }
    }
}

private enum class AnswerSheetFilter { All, Unanswered, Flagged }

@Composable
private fun AnswerSheet(
    exam: ExamState,
    onQuestion: (Int) -> Unit,
    onNextUnanswered: () -> Unit,
    onSubmit: () -> Unit,
) {
    var filter by remember { mutableStateOf(AnswerSheetFilter.All) }
    val visibleQuestions = exam.questions.withIndex().filter { indexed ->
        when (filter) {
            AnswerSheetFilter.All -> true
            AnswerSheetFilter.Unanswered -> indexed.value.id !in exam.selections
            AnswerSheetFilter.Flagged -> indexed.value.id in exam.flaggedQuestionIds
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        Text("答题卡", style = MaterialTheme.typography.headlineMedium)
        Text("已答 ${exam.answeredCount} 题 · 未答 ${exam.questions.size - exam.answeredCount} 题", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                AnswerSheetFilter.All to "全部",
                AnswerSheetFilter.Unanswered to "未答 ${exam.questions.size - exam.answeredCount}",
                AnswerSheetFilter.Flagged to "已标记 ${exam.flaggedQuestionIds.size}",
            ).forEach { (value, label) ->
                FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.heightIn(max = 360.dp).height((((visibleQuestions.size.coerceAtLeast(1)) + 4) / 5 * 58).dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(visibleQuestions, key = { _, item -> item.value.id }) { _, indexed ->
                val index = indexed.index
                val question = indexed.value
                val answered = question.id in exam.selections
                Surface(
                    onClick = { onQuestion(index) },
                    modifier = Modifier.size(48.dp).semantics {
                        stateDescription = when {
                            index == exam.index -> "当前题"
                            answered -> "已作答"
                            else -> "未作答"
                        }
                        contentDescription = "第 ${index + 1} 题"
                    },
                    shape = if (question.id in exam.flaggedQuestionIds) RoundedCornerShape(15.dp) else CircleShape,
                    color = when { index == exam.index -> MaterialTheme.colorScheme.primary; answered -> MaterialTheme.colorScheme.tertiaryContainer; else -> MaterialTheme.colorScheme.surfaceVariant },
                    contentColor = when { index == exam.index -> MaterialTheme.colorScheme.onPrimary; answered -> MaterialTheme.colorScheme.onTertiaryContainer; else -> MaterialTheme.colorScheme.onSurfaceVariant },
                ) { Box(contentAlignment = Alignment.Center) { Text((index + 1).toString(), fontWeight = FontWeight.Bold) } }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (exam.answeredCount < exam.questions.size) {
            OutlinedButton(onClick = onNextUnanswered, Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.SkipNext, null)
                Spacer(Modifier.width(8.dp))
                Text("跳到下一道未答题")
            }
            Spacer(Modifier.height(10.dp))
        }
        Button(onClick = onSubmit, Modifier.fillMaxWidth().height(56.dp)) { Text("提交整份试卷") }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DelayedLoading(show: Boolean, animationsEnabled: Boolean) {
    val configuration = LocalConfiguration.current
    val useStableControls = requiresStableMaterialControls(
        screenWidthDp = configuration.screenWidthDp,
        fontScale = LocalDensity.current.fontScale,
    )
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(show) {
        visible = false
        if (show) { delay(150); visible = true }
    }
    AnimatedVisibility(visible && show, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (animationsEnabled && !useStableControls) {
                        LoadingIndicator(modifier = Modifier.size(48.dp))
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp), strokeWidth = 4.dp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("正在生成成绩…")
                }
            }
        }
    }
}
