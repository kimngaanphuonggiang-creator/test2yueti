package com.yueti.app.ui

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.yueti.app.data.DefaultVocabularyPrompt
import com.yueti.app.data.MemoryRating
import com.yueti.app.data.VocabularyCounts
import com.yueti.app.data.VocabularyGroup
import com.yueti.app.data.VocabularyImageEntity
import com.yueti.app.data.VocabularySettings
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val WordPaper = Color(0xFFF8F2FA)
private val WordInk = Color(0xFF201A22)
private val WordLavender = Color(0xFFE7D8FF)
private val WordLime = Color(0xFFD7FF72)
private val WordCoral = Color(0xFFFFA2BE)

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
internal fun VocabularyScreen(
    animationsEnabled: Boolean,
    onBack: () -> Unit,
    onNotice: (String) -> Unit,
    viewModel: VocabularyViewModel = viewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val largeFont = LocalDensity.current.fontScale > 1.1f
    val scope = rememberCoroutineScope()
    val pagerState = key(ui.deckRevision) {
        rememberPagerState(initialPage = ui.targetInitialPage, pageCount = { ui.cards.size })
    }
    var libraryVisible by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }
    var completionVisible by remember { mutableStateOf(false) }
    var attribution by remember { mutableStateOf<VocabularyImageEntity?>(null) }
    var detailCard by remember { mutableStateOf<VocabularyCardState?>(null) }
    var bingCard by remember { mutableStateOf<VocabularyCardState?>(null) }
    var keyDialogVisible by remember { mutableStateOf(false) }
    var keyDraft by remember { mutableStateOf("") }
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS }
    }

    DisposableEffect(tts) {
        if (ttsReady) tts.language = Locale.UK
        onDispose {
            viewModel.saveSession()
            tts.stop()
            tts.shutdown()
        }
    }
    LaunchedEffect(ttsReady) { if (ttsReady) tts.language = Locale.UK }
    LaunchedEffect(ui.notice) {
        ui.notice?.let {
            onNotice(it)
            viewModel.consumeNotice()
        }
    }
    LaunchedEffect(pagerState, ui.deckRevision, ui.cards.size) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page -> viewModel.onCardSettled(page, ui.deckRevision) }
    }
    LaunchedEffect(ui.deckRevision, ui.cards.firstOrNull()?.word?.wordKey) {
        if (ui.cards.isNotEmpty()) {
            if (pagerState.currentPage != 0) pagerState.scrollToPage(0)
            viewModel.onCardSettled(0, ui.deckRevision)
        }
    }

    fun speak(word: String) {
        if (!ttsReady) onNotice("设备暂时没有可用的英文语音")
        else tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "vocabulary-$word")
    }

    fun rateAndAdvance(wordKey: String, rating: MemoryRating) {
        viewModel.rate(wordKey, rating)
        scope.launch {
            if (pagerState.currentPage < ui.cards.lastIndex) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            } else {
                viewModel.saveSession()
                completionVisible = true
            }
        }
    }

    val density = LocalDensity.current
    val navigationBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    Box(Modifier.fillMaxSize().background(ToolScreenBackground)) {
        when {
            ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LoadingIndicator(Modifier.size(54.dp))
                    Text("正在准备离线雅思词库…", color = Color.White.copy(alpha = .72f))
                }
            }
            ui.cards.isEmpty() -> EmptyVocabularyGroup(ui.group) {
                viewModel.loadGroup(VocabularyGroup.Today)
            }
            else -> {
                VerticalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(top = 142.dp, bottom = navigationBottom + 26.dp),
                    pageSpacing = 14.dp,
                    beyondViewportPageCount = 1,
                    key = { index -> "${ui.cards[index].word.wordKey}-$index" },
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val card = ui.cards[page]
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        .coerceIn(-1.4f, 1.4f)
                    val distance = pageOffset.absoluteValue
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        WordMemoryCard(
                            card = card,
                            compact = false,
                            active = page == pagerState.settledPage,
                            generating = card.word.wordKey in ui.generatingExamples,
                            loadingImage = card.word.wordKey in ui.loadingImages,
                            imageError = ui.imageErrors[card.word.wordKey],
                            aiConfigured = ui.aiConfigured || DeepSeekAiGateway.configured,
                            onSpeak = { speak(card.word.word) },
                            onStar = { viewModel.toggleStar(card.word.wordKey) },
                            onImage = { bingCard = card },
                            onImageRetry = { bingCard = card },
                            onNextImage = { bingCard = card },
                            onAttribution = { card.image?.let { attribution = it } },
                            onDetails = { detailCard = card },
                            onGenerate = {
                                if (ui.aiConfigured || DeepSeekAiGateway.configured) viewModel.regenerateExample(card.word.wordKey)
                                else keyDialogVisible = true
                            },
                            onRate = { rateAndAdvance(card.word.wordKey, it) },
                            modifier = Modifier
                                .widthIn(max = 760.dp)
                                .fillMaxSize()
                                .padding(horizontal = 15.dp)
                                .graphicsLayer {
                                    if (animationsEnabled) {
                                        translationX = -distance * 18.dp.toPx()
                                        rotationZ = pageOffset * 1.25f
                                        scaleX = 1f - distance * .04f
                                        scaleY = 1f - distance * .04f
                                        alpha = .62f + (1f - distance.coerceIn(0f, 1f)) * .38f
                                    }
                                },
                        )
                    }
                }
            }
        }

        Box(
            Modifier.fillMaxWidth().height(154.dp).background(
                Brush.verticalGradient(
                    listOf(ToolScreenBackground, ToolScreenBackground.copy(alpha = .96f), ToolScreenBackground.copy(alpha = 0f)),
                ),
            ),
        )
        ToolScreenHeader(
            title = "雅思词卡",
            subtitle = ui.group.label,
            onBack = {
                viewModel.saveSession()
                onBack()
            },
        ) {
            IconButton(onClick = { viewModel.searchLibrary(""); libraryVisible = true }) {
                Icon(Icons.Rounded.CollectionsBookmark, "查看词库分类", tint = Color.White)
            }
            IconButton(onClick = { settingsVisible = true }) {
                Icon(Icons.Rounded.Settings, "词卡设置", tint = Color.White)
            }
        }
        if (ui.cards.isNotEmpty()) {
            val continuousProgress = vocabularyPagerProgress(
                pagerState.currentPage,
                pagerState.currentPageOffsetFraction,
                ui.cards.size,
            )
            Surface(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(top = if (largeFont) 80.dp else 72.dp, start = 14.dp, end = 14.dp),
                shape = RoundedCornerShape(16.dp),
                color = ToolChromeColor.copy(alpha = .94f),
                contentColor = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .1f)),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RollingNumber(
                        value = (pagerState.currentPage + 1).coerceAtMost(ui.cards.size),
                        animationsEnabled = animationsEnabled,
                        suffix = " / ${ui.cards.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(12.dp))
                    LinearWavyProgressIndicator(
                        progress = { continuousProgress },
                        color = WordLime,
                        trackColor = Color.White.copy(alpha = .13f),
                        amplitude = { if (animationsEnabled) .34f else 0f },
                        modifier = Modifier.weight(1f).height(7.dp),
                    )
                }
            }
        }
    }

    if (libraryVisible) VocabularyLibrarySheet(
        ui = ui,
        onDismiss = { libraryVisible = false },
        onSearch = viewModel::searchLibrary,
        onGroup = { viewModel.loadGroup(it); libraryVisible = false },
    )
    if (settingsVisible) VocabularySettingsSheet(
        settings = ui.settings,
        onDismiss = { settingsVisible = false },
        onSave = { viewModel.updateSettings(it); settingsVisible = false },
    )
    if (completionVisible) VocabularyCompletionDialog(
        ui = ui,
        onDismiss = { completionVisible = false },
        onNextGroup = { completionVisible = false; viewModel.loadGroup(ui.group) },
    )
    attribution?.let { image ->
        VocabularyAttributionSheet(
            image = image,
            onDismiss = { attribution = null },
            onClear = {
                viewModel.clearImage(image.wordKey)
                attribution = null
            },
        )
    }
    detailCard?.let { card ->
        VocabularyDetailsSheet(card = card, onDismiss = { detailCard = null })
    }
    bingCard?.let { card ->
        BingImagePicker(
            word = card.word.word,
            translation = card.word.translation,
            onDismiss = { bingCard = null },
            onPick = { result ->
                viewModel.saveBingImage(card.word.wordKey, result)
                bingCard = null
            },
        )
    }
    if (keyDialogVisible) AlertDialog(
        onDismissRequest = { keyDialogVisible = false },
        icon = { Icon(Icons.Rounded.Key, null) },
        title = { Text("配置 DeepSeek API") },
        text = {
            OutlinedTextField(
                value = keyDraft,
                onValueChange = { keyDraft = it.trim() },
                singleLine = true,
                label = { Text("API 密钥") },
                supportingText = { Text("由 Android Keystore 加密保存在本机") },
            )
        },
        confirmButton = {
            Button(
                enabled = keyDraft.startsWith("sk-") && keyDraft.length >= 20,
                onClick = {
                    viewModel.configureApiKey(keyDraft)
                    keyDraft = ""
                    keyDialogVisible = false
                },
            ) { Text("保存并生成") }
        },
        dismissButton = { TextButton(onClick = { keyDialogVisible = false }) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WordMemoryCard(
    card: VocabularyCardState,
    compact: Boolean,
    active: Boolean,
    generating: Boolean,
    loadingImage: Boolean,
    imageError: String?,
    aiConfigured: Boolean,
    onSpeak: () -> Unit,
    onStar: () -> Unit,
    onImage: () -> Unit,
    onImageRetry: () -> Unit,
    onNextImage: () -> Unit,
    onAttribution: () -> Unit,
    onDetails: () -> Unit,
    onGenerate: () -> Unit,
    onRate: (MemoryRating) -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.semantics {
            contentDescription = "单词 ${card.word.word}，${compactMeaning(card.word.translation)}，记忆箱 ${card.progress.box}"
        },
        shape = RoundedCornerShape(topStart = 38.dp, topEnd = 30.dp, bottomEnd = 38.dp, bottomStart = 30.dp),
        color = WordPaper,
        contentColor = WordInk,
        shadowElevation = if (active) 12.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .78f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val short = compact || maxHeight < 610.dp
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(if (short) 7.dp else 10.dp)) {
                VocabularyImage(card, loadingImage, imageError, onImage, onImageRetry, onNextImage, onAttribution, short)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            card.word.word,
                            style = if (short) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("/${card.word.phonetic.trim('/')} /", color = Color(0xFF6A4A78), style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = onSpeak) { Icon(Icons.Rounded.VolumeUp, "朗读 ${card.word.word}") }
                    IconButton(onClick = onStar) {
                        Icon(
                            if (card.progress.isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            if (card.progress.isStarred) "取消收藏" else "收藏单词",
                            tint = if (card.progress.isStarred) Color(0xFF7C28D7) else WordInk,
                        )
                    }
                }
                Text(
                    compactMeaning(card.word.translation),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (short) 3 else 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (card.word.partOfSpeech.isNotBlank() || card.word.definition.isNotBlank()) {
                    Text(
                        listOf(card.word.partOfSpeech.takeIf(String::isNotBlank), compactDefinition(card.word.definition)).filterNotNull().joinToString(" · "),
                        color = WordInk.copy(alpha = .68f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = if (short) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onDetails, modifier = Modifier.height(38.dp)) {
                    Icon(Icons.Rounded.Info, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("查看完整释义与例句")
                }
                val hasExampleContent = card.example != null || generating
                if (!hasExampleContent) Spacer(Modifier.weight(1f))
                VocabularyExamplePanel(
                    card,
                    active,
                    generating,
                    aiConfigured,
                    onGenerate,
                    if (hasExampleContent) Modifier.weight(1f) else Modifier.height(150.dp),
                )
                RatingRow(onRate)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VocabularyImage(
    card: VocabularyCardState,
    loading: Boolean,
    imageError: String?,
    onImage: () -> Unit,
    onImageRetry: () -> Unit,
    onNextImage: () -> Unit,
    onAttribution: () -> Unit,
    compact: Boolean,
) {
    val context = LocalContext.current
    val imageModel: Any? = card.image?.localCachePath?.takeIf(String::isNotBlank)?.let { java.io.File(it) } ?: card.image?.thumbnailUrl
    var decodeFailed by remember(imageModel) { mutableStateOf(false) }
    val imageRequest = remember(imageModel) {
        imageModel?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .httpHeaders(
                    NetworkHeaders.Builder()
                        .set("User-Agent", "Yueti/0.11.0 (private Android study app)")
                        .set("Accept", "image/avif,image/webp,image/png,image/jpeg,*/*")
                        .build(),
                )
                .build()
        }
    }
    Box(
        Modifier.fillMaxWidth().height(if (compact) 104.dp else 142.dp).clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF2A252C)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            card.image != null -> {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "${card.word.word} 的记忆配图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = { decodeFailed = false },
                    onError = { decodeFailed = true },
                )
                if (decodeFailed) {
                    ImageFailureState("图片解码失败", onImageRetry)
                } else {
                    Row(
                        Modifier.align(Alignment.BottomEnd).padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Surface(onClick = onAttribution, color = Color.Black.copy(.68f), contentColor = Color.White, shape = RoundedCornerShape(11.dp)) {
                            Icon(Icons.Rounded.Info, "图片来源与许可", Modifier.padding(7.dp).size(17.dp))
                        }
                        Surface(onClick = onNextImage, color = Color.Black.copy(.68f), contentColor = Color.White, shape = RoundedCornerShape(11.dp)) {
                            Icon(Icons.Rounded.ImageSearch, "用 Bing 换一张配图", Modifier.padding(7.dp).size(17.dp))
                        }
                    }
                }
            }
            loading -> LoadingIndicator(Modifier.size(42.dp))
            imageError != null -> ImageFailureState(imageError, onImageRetry)
            else -> TextButton(onClick = onImage) {
                Icon(Icons.Rounded.ImageSearch, null)
                Spacer(Modifier.width(7.dp))
                Text("用 Bing 选择记忆配图", color = Color.White)
            }
        }
    }
}

@Composable
private fun ImageFailureState(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(message, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 2)
        TextButton(onClick = onRetry) {
            Icon(Icons.Rounded.Refresh, null)
            Spacer(Modifier.width(5.dp))
            Text("重新获取")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VocabularyExamplePanel(
    card: VocabularyCardState,
    active: Boolean,
    generating: Boolean,
    aiConfigured: Boolean,
    onGenerate: () -> Unit,
    modifier: Modifier,
) {
    Surface(modifier, shape = RoundedCornerShape(22.dp), color = WordLavender, contentColor = WordInk) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (active) {
                EmotionBallView(
                    emotion = when {
                        generating -> BotEmotion.Thinking
                        card.example != null -> BotEmotion.Satisfied
                        aiConfigured -> BotEmotion.WaitingInput
                        else -> BotEmotion.Dormant
                    },
                    active = true,
                    lite = true,
                    onTap = onGenerate,
                    modifier = Modifier.size(68.dp),
                )
                Spacer(Modifier.width(7.dp))
            } else {
                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(24.dp), tint = Color(0xFF6A1BB2))
                Spacer(Modifier.width(10.dp))
            }
            AnimatedContent(
                targetState = when {
                    generating -> "loading"
                    card.example != null -> "ready"
                    aiConfigured -> "waiting"
                    else -> "key"
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "vocabulary example state",
            ) { state ->
                when (state) {
                    "loading" -> Column { Text("跃跃正在写例句", fontWeight = FontWeight.Black); Text("停留在当前卡片即可", style = MaterialTheme.typography.bodySmall) }
                    "ready" -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(card.example?.example.orEmpty(), fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(card.example?.translation.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(card.example?.usageNote.orEmpty(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF5E3A70), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    "waiting" -> Column { Text("跃跃例句", fontWeight = FontWeight.Black); Text("卡片停稳后自动生成", style = MaterialTheme.typography.bodySmall) }
                    else -> Column { Text("跃跃例句未启用", fontWeight = FontWeight.Black); Text("点按设置 API 密钥", style = MaterialTheme.typography.bodySmall) }
                }
            }
            IconButton(onClick = onGenerate) { Icon(if (card.example == null) Icons.Rounded.AutoAwesome else Icons.Rounded.Refresh, "生成例句") }
        }
    }
}

@Composable
private fun RatingRow(onRate: (MemoryRating) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RatingButton("忘记", WordCoral, MemoryRating.Forgot, onRate, Modifier.weight(1f))
        RatingButton("模糊", Color(0xFFD8C2FF), MemoryRating.Fuzzy, onRate, Modifier.weight(1f))
        RatingButton("记住", WordLime, MemoryRating.Remembered, onRate, Modifier.weight(1f))
        RatingButton("熟记", Color(0xFF2B252D), MemoryRating.Mastered, onRate, Modifier.weight(1f), Color.White)
    }
}

@Composable
private fun RatingButton(
    label: String,
    color: Color,
    rating: MemoryRating,
    onRate: (MemoryRating) -> Unit,
    modifier: Modifier,
    contentColor: Color = WordInk,
) {
    Surface(
        onClick = { onRate(rating) },
        modifier = modifier.height(48.dp).semantics { role = Role.Button },
        shape = RoundedCornerShape(15.dp),
        color = color,
        contentColor = contentColor,
    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.Black) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularyLibrarySheet(
    ui: VocabularyUiState,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onGroup: (VocabularyGroup) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) { delay(220); onSearch(query) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF211C24), contentColor = Color.White) {
        Column(Modifier.fillMaxWidth().heightIn(max = 690.dp).padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Text("词库总分类", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("搜索单词或中文释义") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                if (query.isBlank()) {
                    items(VocabularyGroup.entries, key = VocabularyGroup::name) { group ->
                        Surface(
                            onClick = { onGroup(group) },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                        ListItem(
                            headlineContent = { Text(group.label, color = Color.White, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(groupDescription(group), color = Color.White.copy(alpha = .62f)) },
                            trailingContent = { Text(groupCount(group, ui.counts, ui.settings.groupSize).toString(), color = WordLime, fontWeight = FontWeight.Black) },
                            leadingContent = { Icon(groupIcon(group), null, tint = Color(0xFFD8C2FF)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.semantics { role = Role.Button },
                        )
                        }
                    }
                } else {
                    items(ui.libraryWords, key = { it.wordKey }) { word ->
                        ListItem(
                            headlineContent = { Text(word.word, color = Color.White, fontWeight = FontWeight.Black) },
                            supportingContent = { Text(compactMeaning(word.translation), color = Color.White.copy(alpha = .66f), maxLines = 1) },
                            trailingContent = { Text("/${word.phonetic.trim('/')} /", color = Color(0xFFD8C2FF)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularySettingsSheet(
    settings: VocabularySettings,
    onDismiss: () -> Unit,
    onSave: (VocabularySettings) -> Unit,
) {
    var size by remember(settings) { mutableFloatStateOf(settings.groupSize.toFloat()) }
    var includeMastered by remember(settings) { mutableStateOf(settings.includeMastered) }
    var autoExample by remember(settings) { mutableStateOf(settings.autoExample) }
    var wifiImages by remember(settings) { mutableStateOf(settings.wifiAutoImages) }
    var prompt by remember(settings) { mutableStateOf(settings.promptTemplate) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF211C24), contentColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).imePadding().padding(horizontal = 18.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text("词卡设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("每组 ${size.roundToInt()} 词", fontWeight = FontWeight.Bold)
            Slider(
                value = size,
                onValueChange = { size = (it / 5f).roundToInt() * 5f },
                valueRange = 5f..50f,
                steps = 8,
            )
            SettingSwitch("复习时包含已熟记", includeMastered) { includeMastered = it }
            SettingSwitch("卡片停稳后自动生成例句", autoExample) { autoExample = it }
            SettingSwitch("仅在 Wi‑Fi 下自动配图", wifiImages) { wifiImages = it }
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("跃跃例句提示词") },
                supportingText = { Text("保存后立即用于当前词卡；支持 {word}、{phonetic}、{translation}、{definition}、{level}") },
                minLines = 3,
                maxLines = 6,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { prompt = DefaultVocabularyPrompt }, modifier = Modifier.weight(1f)) { Text("恢复默认") }
                Button(
                    onClick = { onSave(settings.copy(groupSize = size.roundToInt(), includeMastered = includeMastered, autoExample = autoExample, wifiAutoImages = wifiImages, promptTemplate = prompt)) },
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(6.dp)); Text("保存") }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun VocabularyCompletionDialog(ui: VocabularyUiState, onDismiss: () -> Unit, onNextGroup: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DoneAll, null) },
        title = { Text("本组完成") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("记住 ${ui.ratingCounts[MemoryRating.Remembered] ?: 0} · 模糊 ${ui.ratingCounts[MemoryRating.Fuzzy] ?: 0}")
                Text("忘记 ${ui.ratingCounts[MemoryRating.Forgot] ?: 0} · 新熟记 ${ui.ratingCounts[MemoryRating.Mastered] ?: 0}")
            }
        },
        confirmButton = { Button(onClick = onNextGroup) { Text("继续下一组"); Icon(Icons.AutoMirrored.Rounded.ArrowForward, null) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("稍后继续") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularyAttributionSheet(image: VocabularyImageEntity, onDismiss: () -> Unit, onClear: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 34.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("图片来源与许可", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(image.fileTitle, fontWeight = FontWeight.Bold)
            Text("作者：${image.artist}")
            Text("许可：${image.licenseName}")
            Button(onClick = { uriHandler.openUri(image.sourcePageUrl) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (image.provider == "BING_WEB_PICKED") "打开原始来源页" else "打开 Wikimedia Commons 来源页")
            }
            if (image.provider == "COMMONS" && image.licenseUrl.isNotBlank()) TextButton(onClick = { uriHandler.openUri(image.licenseUrl) }, modifier = Modifier.fillMaxWidth()) { Text("查看许可证全文") }
            TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("清除这张配图", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularyDetailsSheet(card: VocabularyCardState, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WordPaper,
        contentColor = WordInk,
    ) {
        LazyColumn(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(card.word.word, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text("/${card.word.phonetic.trim('/')} /", color = Color(0xFF6A4A78), style = MaterialTheme.typography.titleMedium)
            }
            item {
                DetailSection("完整中文释义", card.word.translation.normalizedVocabularyLines().joinToString("\n"))
            }
            if (card.word.definition.isNotBlank()) item {
                DetailSection("英文定义与用法", card.word.definition.normalizedVocabularyLines().joinToString("\n"))
            }
            card.example?.let { example ->
                item {
                    DetailSection("跃跃例句", example.example)
                    Spacer(Modifier.height(8.dp))
                    DetailSection("例句翻译", example.translation)
                    Spacer(Modifier.height(8.dp))
                    DetailSection("用法提醒", example.usageNote)
                }
            }
            card.image?.let { image ->
                item {
                    AsyncImage(
                        model = image.thumbnailUrl,
                        contentDescription = "${card.word.word} 的记忆配图",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(22.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${image.artist} · ${image.licenseName}", style = MaterialTheme.typography.bodySmall, color = WordInk.copy(alpha = .66f))
                    TextButton(onClick = { uriHandler.openUri(image.sourcePageUrl) }) { Text("查看图片来源与许可") }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = Color(0xFF6A1BB2), fontWeight = FontWeight.Black)
        Text(body.ifBlank { "暂无内容" }, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyVocabularyGroup(group: VocabularyGroup, onToday: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(30.dp), color = Color(0xFF29252C), contentColor = Color.White) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Bookmarks, null, Modifier.size(50.dp), tint = WordLime)
                Text("${group.label}暂时没有单词", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("可以切回今日卡组，或在总分类中选择其他分组。", color = Color.White.copy(alpha = .68f))
                Button(onClick = onToday) { Text("返回今日卡组") }
            }
        }
    }
}

private fun compactMeaning(raw: String): String = raw.normalizedVocabularyLines().take(3).joinToString("；")

private fun compactDefinition(raw: String): String? = raw.normalizedVocabularyLines().firstOrNull()?.take(180)

private fun String.normalizedVocabularyLines(): Sequence<String> =
    replace("\\n", "\n").lineSequence().map(String::trim).filter(String::isNotBlank)

private fun groupCount(group: VocabularyGroup, counts: VocabularyCounts, groupSize: Int): Int = when (group) {
    VocabularyGroup.Today -> counts.dueCount.coerceAtMost(groupSize) +
        counts.newCount.coerceAtMost((groupSize - counts.dueCount).coerceAtLeast(0))
    VocabularyGroup.Due -> counts.dueCount
    VocabularyGroup.New -> counts.newCount
    VocabularyGroup.Learning -> counts.learningCount
    VocabularyGroup.Mastered -> counts.masteredCount
    VocabularyGroup.Starred -> counts.starredCount
    VocabularyGroup.All -> counts.totalCount
}

private fun groupDescription(group: VocabularyGroup): String = when (group) {
    VocabularyGroup.Today -> "到期词优先，再补充高频新词"
    VocabularyGroup.Due -> "已经到达复习时间的词"
    VocabularyGroup.New -> "尚未开始学习"
    VocabularyGroup.Learning -> "五箱复习中的词"
    VocabularyGroup.Mastered -> "你标记为熟记的词"
    VocabularyGroup.Starred -> "主动收藏的重点词"
    VocabularyGroup.All -> "ECDICT IELTS 完整词表"
}

private fun groupIcon(group: VocabularyGroup) = when (group) {
    VocabularyGroup.Today -> Icons.Rounded.Psychology
    VocabularyGroup.Due -> Icons.Rounded.Refresh
    VocabularyGroup.New -> Icons.Rounded.AutoAwesome
    VocabularyGroup.Learning -> Icons.Rounded.Bookmarks
    VocabularyGroup.Mastered -> Icons.Rounded.DoneAll
    VocabularyGroup.Starred -> Icons.Rounded.Star
    VocabularyGroup.All -> Icons.Rounded.CollectionsBookmark
}

internal fun vocabularyPagerProgress(currentPage: Int, offsetFraction: Float, pageCount: Int): Float =
    ((currentPage + offsetFraction + 1f) / pageCount.coerceAtLeast(1)).coerceIn(0f, 1f)
