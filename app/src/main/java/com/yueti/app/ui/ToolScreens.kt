package com.yueti.app.ui

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfDocument
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.objecthunter.exp4j.ExpressionBuilder
import org.opencv.android.OpenCVLoader
import com.yueti.scanner.core.DocumentFilter
import com.yueti.scanner.core.DocumentPoint
import com.yueti.scanner.core.DocumentScannerCore
import java.io.File
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun EmotionBallView(
    emotion: BotEmotion,
    active: Boolean,
    lite: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTap by rememberUpdatedState(onTap)
    val currentEmotion by rememberUpdatedState(emotion)
    val currentActive by rememberUpdatedState(active)
    val currentLite by rememberUpdatedState(lite)
    fun syncState(web: WebView) {
        val id = currentEmotion.id.takeIf(EmotionBallIds.all::contains) ?: BotEmotion.Idle.id
        web.evaluateJavascript("window.YuetiEmotion&&window.YuetiEmotion.set('$id')", null)
        web.evaluateJavascript("window.YuetiEmotion&&window.YuetiEmotion.active($currentActive)", null)
        web.evaluateJavascript("window.YuetiEmotion&&window.YuetiEmotion.lite($currentLite)", null)
    }
    AndroidView(
        // The upstream blob deliberately deforms outside its resting outline.  Clipping the
        // AndroidView cut off eyes, rings and the bounce animation on compact home tiles.
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                settings.blockNetworkLoads = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.post { view.takeIf { it.isAttachedToWindow }?.let(::syncState) }
                    }
                }
                addJavascriptInterface(object {
                    @JavascriptInterface fun onTap() = post { currentTap() }
                }, "YuetiBot")
                // Inline the repository's packaged modules into one document. Some Flyme WebView
                // builds fail to resolve file:// subresources even when the parent page loads.
                // This path uses no server and performs no network request.
                loadDataWithBaseURL(
                    "https://yueti.local/emotion-ball/",
                    bundledEmotionBallHtml(context),
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
        update = ::syncState,
        onRelease = { it.removeJavascriptInterface("YuetiBot"); it.destroy() },
    )
}

internal fun bundledEmotionBallHtml(context: android.content.Context): String {
    fun asset(path: String) = context.assets.open("emotion-ball/$path").bufferedReader(Charsets.UTF_8).use { it.readText() }
    return listOf("rings.js", "emotions.js", "ball.js", "engine.js").fold(asset("bot.html")) { html, script ->
        html.replace("<script src=\"js/$script\"></script>", "<script>\n${asset("js/$script")}\n</script>")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AssistantScreen(onBack: () -> Unit, onNavigate: (AppPage) -> Unit) {
    val gateway = DeepSeekAiGateway
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(AssistantMode.Chat) }
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Pair<Boolean, String>>>(emptyList()) }
    var thinking by remember { mutableStateOf(false) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var apiKeyDraft by remember { mutableStateOf("") }
    var keyStatus by remember { mutableStateOf<String?>(null) }
    var aiConfigured by remember { mutableStateOf(gateway.configured) }
    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { AiCredentialStore.load(context) }
        if (saved.isNotBlank()) DeepSeekAiGateway.configure(saved)
        aiConfigured = gateway.configured
    }
    LaunchedEffect(keyStatus) {
        if (keyStatus != null) {
            delay(2600)
            keyStatus = null
        }
    }
    val botEmotion = when {
        thinking -> BotEmotion.Thinking
        messages.lastOrNull()?.first == false -> BotEmotion.Happy
        else -> BotEmotion.Idle
    }
    Scaffold(
        topBar = { ToolTopBar("跃题助手", onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.navigationBars).imePadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            EmotionBallView(botEmotion, active = true, lite = false, onTap = {}, Modifier.fillMaxWidth().height(220.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistantMode.entries.forEach { item -> FilterChip(
                        selected = mode == item,
                        enabled = item != AssistantMode.WebSearch,
                        onClick = { mode = item },
                        label = { Text(when (item) { AssistantMode.Chat -> "对话"; AssistantMode.DeepThinking -> "深度思考"; AssistantMode.WebSearch -> "联网搜索" }) },
                        leadingIcon = { Icon(when (item) { AssistantMode.Chat -> Icons.Rounded.Chat; AssistantMode.DeepThinking -> Icons.Rounded.Psychology; AssistantMode.WebSearch -> Icons.Rounded.TravelExplore }, null) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF342A3D),
                            labelColor = Color.White,
                            iconColor = Color.White,
                            disabledContainerColor = Color(0xFF262129),
                            disabledLabelColor = Color.White.copy(alpha = .54f),
                            selectedContainerColor = Color(0xFFA77BFF),
                            selectedLabelColor = Color(0xFF171318),
                            selectedLeadingIconColor = Color(0xFF171318),
                        ),
                    ) }
                }
                IconButton(onClick = { apiKeyDraft = ""; showKeyDialog = true }) { Icon(Icons.Rounded.Key, "配置 DeepSeek API") }
            }
            if (!aiConfigured) {
                Surface(
                    onClick = { apiKeyDraft = ""; showKeyDialog = true },
                    shape = RoundedCornerShape(16.dp), color = Color(0xFFA77BFF), contentColor = Color(0xFF171318), modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Key, null); Spacer(Modifier.width(10.dp))
                        Text("输入 DeepSeek API 密钥", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
            keyStatus?.let { Text(it, color = Color(0xFFD7FF72), style = MaterialTheme.typography.bodySmall) }
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (messages.isEmpty()) item {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (aiConfigured) "可以直接提问，也可以切换到深度思考。联网搜索保持关闭。" else "请先在上方输入 DeepSeek API 密钥；机器人表情和本地工具无需密钥。",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                itemsIndexed(messages) { _, message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.first) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (message.first) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (message.first) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(.88f),
                        ) { Text(message.second, Modifier.padding(14.dp)) }
                    }
                }
                if (thinking) item { LoadingIndicator(Modifier.size(42.dp)) }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("输入问题") },
                supportingText = { Text(if (mode == AssistantMode.DeepThinking) "DeepSeek V4 Pro · 深度思考" else "DeepSeek V4 Flash · 普通对话") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFD8C2FF),
                    unfocusedContainerColor = Color(0xFFD8C2FF),
                    focusedTextColor = Color(0xFF171318),
                    unfocusedTextColor = Color(0xFF171318),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White,
                    focusedPlaceholderColor = Color(0xFF3B006F),
                    unfocusedPlaceholderColor = Color(0xFF3B006F),
                    focusedSupportingTextColor = Color.White,
                    unfocusedSupportingTextColor = Color.White,
                    focusedTrailingIconColor = Color(0xFF3B006F),
                    unfocusedTrailingIconColor = Color(0xFF3B006F),
                    disabledTrailingIconColor = Color(0xFF5A2B9F),
                ),
                trailingIcon = {
                    IconButton(enabled = aiConfigured && draft.isNotBlank() && !thinking, onClick = {
                        val prompt = draft.trim()
                        draft = ""
                        messages = messages + (true to prompt)
                        thinking = true
                        scope.launch {
                            gateway.chat(prompt, mode).fold(
                                onSuccess = { messages = messages + (false to it) },
                                onFailure = { messages = messages + (false to (it.message ?: "AI 请求失败")) },
                            )
                            thinking = false
                        }
                    }) { Icon(Icons.Rounded.Send, "发送") }
                },
            )
        }
    }
    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            icon = { Icon(Icons.Rounded.Key, null) },
            title = { Text("配置 DeepSeek API") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("密钥会由 Android Keystore 加密后保存在本机，不写入聊天记录或导出文件。")
                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it.trim() },
                        singleLine = true,
                        label = { Text("API Key") },
                        placeholder = { Text("sk-…") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE7D8FF),
                            unfocusedContainerColor = Color(0xFFE7D8FF),
                            focusedTextColor = Color(0xFF171318),
                            unfocusedTextColor = Color(0xFF171318),
                        ),
                    )
                }
            },
            confirmButton = {
                Button(enabled = apiKeyDraft.startsWith("sk-") && apiKeyDraft.length >= 20, onClick = {
                    AiCredentialStore.save(context, apiKeyDraft)
                    DeepSeekAiGateway.configure(apiKeyDraft)
                    aiConfigured = true
                    apiKeyDraft = ""
                    keyStatus = "API 密钥已安全保存，可以开始对话或生成函数。"
                    showKeyDialog = false
                }) { Text("保存并启用") }
            },
            dismissButton = { TextButton(onClick = { showKeyDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
internal fun ScannerScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pages by remember { mutableStateOf<List<File>>(emptyList()) }
    var currentCapture by remember { mutableStateOf<Pair<Uri, File>?>(null) }
    var processing by remember { mutableStateOf(false) }
    var pendingScans by remember { mutableStateOf<List<Pair<Bitmap, List<DocumentPoint>>>>(emptyList()) }
    var pdfQuality by remember { mutableFloatStateOf(85f) }
    var a4Pages by remember { mutableStateOf(true) }

    suspend fun importUri(uri: Uri) {
        processing = true
        val bitmap = withContext(Dispatchers.IO) { loadScanBitmap(context, uri) }
        val corners = withContext(Dispatchers.Default) { DocumentScannerCore.detect(bitmap) }
        pendingScans = pendingScans + (bitmap to corners)
        processing = false
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        scope.launch { uris.forEach { importUri(it) } }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val captured = currentCapture
        if (ok && captured != null) scope.launch { importUri(captured.first) }
    }
    fun launchCamera() {
        val dir = File(context.filesDir, "scans/source").apply { mkdirs() }
        val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        currentCapture = uri to file
        camera.launch(uri)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else onNotice("未获得相机权限，仍可从相册导入")
    }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { OpenCVLoader.initLocal() } }

    Scaffold(
        containerColor = ToolScreenBackground,
        contentColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.navigationBars)) {
            ToolScreenHeader("文档扫描", onBack, subtitle = "本机处理 · 多页 PDF")
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { permission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("拍摄") }
                OutlinedButton(onClick = { picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text("导入") }
            }
            if (processing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (pages.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.DocumentScanner, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("拍一页纸，自动生成白底黑字扫描件", style = MaterialTheme.typography.titleMedium)
                        Text("全部处理都在本机完成，不上传图片。", color = Color.White.copy(alpha = .68f))
                    }
                }
            } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(pages, key = { _, f -> f.absolutePath }) { index, file ->
                    val bitmap = remember(file, file.lastModified()) { BitmapFactory.decodeFile(file.absolutePath) }
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF29252C), contentColor = Color.White),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, Modifier.size(92.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(12.dp)) {
                                Text("第 ${index + 1} 页", style = MaterialTheme.typography.titleMedium)
                                Text("白底黑字 · 本地文件", color = Color.White.copy(alpha = .66f))
                                Row {
                                    IconButton(enabled = index > 0, onClick = { pages = pages.toMutableList().also { list -> val item = list.removeAt(index); list.add(index - 1, item) } }) { Icon(Icons.Rounded.KeyboardArrowUp, "上移") }
                                    IconButton(enabled = index < pages.lastIndex, onClick = { pages = pages.toMutableList().also { list -> val item = list.removeAt(index); list.add(index + 1, item) } }) { Icon(Icons.Rounded.KeyboardArrowDown, "下移") }
                                    IconButton(onClick = { scope.launch { withContext(Dispatchers.IO) { rotateScan(file) }; pages = pages.toList() } }) { Icon(Icons.Rounded.RotateRight, "旋转") }
                                }
                            }
                            IconButton(onClick = { pages = pages - file; file.delete() }) { Icon(Icons.Rounded.Delete, "删除") }
                        }
                    }
                }
            }
            AnimatedVisibility(pages.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PDF 质量 ${pdfQuality.roundToInt()}%")
                    Slider(value = pdfQuality, onValueChange = { pdfQuality = it }, valueRange = 50f..100f)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = a4Pages, onClick = { a4Pages = true }, label = { Text("A4 自适应") })
                        FilterChip(selected = !a4Pages, onClick = { a4Pages = false }, label = { Text("原始比例") })
                    }
                    Button(onClick = { scope.launch { sharePdf(context, pages, pdfQuality.roundToInt(), a4Pages); onNotice("PDF 已生成") } }, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                        Icon(Icons.Rounded.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text("导出并分享 PDF")
                    }
                }
            }
        }
        }
    }
    pendingScans.firstOrNull()?.let { (bitmap, corners) ->
        DocumentCropEditor(
            bitmap = bitmap,
            initialCorners = corners,
            onDismiss = { pendingScans = pendingScans.drop(1) },
            onApply = { adjustedCorners, filter ->
                processing = true
                scope.launch {
                    val file = withContext(Dispatchers.Default) { processScan(context, bitmap, adjustedCorners, filter) }
                    pages = pages + file
                    pendingScans = pendingScans.drop(1)
                    processing = false
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun GraphScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    val gateway = DeepSeekAiGateway
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var formulaField by remember { mutableStateOf(TextFieldValue("sin(x)", selection = TextRange(6))) }
    val formula = formulaField.text
    var formulas by remember { mutableStateOf(listOf("sin(x)")) }
    var scale by remember { mutableFloatStateOf(42f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var generating by remember { mutableStateOf(false) }
    var aiConfigured by remember { mutableStateOf(gateway.configured) }
    var keyboardVisible by remember { mutableStateOf(false) }
    val mathKeys = listOf(
        "x" to "x", "y" to "y", "θ" to "theta", "t" to "t", "π" to "pi", "e" to "e",
        "+" to "+", "−" to "-", "×" to "*", "÷" to "/", "^" to "^", "=" to "=", "<" to "<", ">" to ">",
        "(" to "(", ")" to ")", "," to ",", "sin" to "sin(", "cos" to "cos(", "tan" to "tan(",
        "asin" to "asin(", "acos" to "acos(", "atan" to "atan(", "√" to "sqrt(",
        "|x|" to "abs(", "ln" to "log(", "log₁₀" to "log10(", "exp" to "exp(",
        "←" to "LEFT", "→" to "RIGHT", "⌫" to "BACK", "AC" to "CLEAR",
    )
    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { AiCredentialStore.load(context) }
        if (saved.isNotBlank()) DeepSeekAiGateway.configure(saved)
        aiConfigured = gateway.configured
    }
    val colors = listOf(Color(0xFF7C3AED), Color(0xFF00A884), Color(0xFFFF5A7D), Color(0xFF2979FF), Color(0xFFFF9800))
    Scaffold(
        containerColor = ToolScreenBackground,
        contentColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.navigationBars)) {
            ToolScreenHeader("函数图像", onBack, subtitle = "显式 · 隐式 · 极坐标 · 参数方程")
            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp)) {
                GraphCanvas(
                    formulas,
                    colors,
                    scale,
                    pan,
                    onTransform = { zoom, delta -> scale = (scale * zoom).coerceIn(12f, 180f); pan += delta },
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(Color(0xFF211C24)),
                )
                if (formulas.isNotEmpty()) {
                    Row(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(10.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        formulas.forEachIndexed { index, value ->
                            InputChip(
                                selected = true,
                                onClick = { formulaField = TextFieldValue(value, selection = TextRange(value.length)) },
                                label = { Text(graphLabel(value)) },
                                avatar = { Box(Modifier.size(12.dp).background(colors[index % colors.size], RoundedCornerShape(6.dp))) },
                                trailingIcon = { IconButton(onClick = { formulas = formulas - value }, Modifier.size(28.dp)) { Icon(Icons.Rounded.Close, "移除") } },
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = ToolChromeColor,
                    contentColor = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .12f)),
                ) {
                    Row(Modifier.padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scale = 42f; pan = Offset.Zero }) { Icon(Icons.Rounded.CenterFocusStrong, "复位视图") }
                        IconButton(onClick = { keyboardVisible = true }) { Icon(Icons.Rounded.Keyboard, "打开数学键盘") }
                        Text("${formulas.size}/8", color = Color.White.copy(alpha = .68f), modifier = Modifier.padding(horizontal = 9.dp))
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFF211C24),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = formulaField,
                        onValueChange = { formulaField = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("表达式或中文描述") },
                        leadingIcon = { Icon(Icons.Rounded.Functions, null) },
                        trailingIcon = { IconButton(onClick = { keyboardVisible = true }) { Icon(Icons.Rounded.Keyboard, "数学键盘") } },
                        placeholder = { Text("例如 sin(x) 或 绘制一条开口向上的抛物线") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE7D8FF), unfocusedContainerColor = Color(0xFFE7D8FF),
                            focusedTextColor = Color(0xFF171318), unfocusedTextColor = Color(0xFF171318),
                            focusedLabelColor = Color(0xFF3B006F), unfocusedLabelColor = Color(0xFF3B006F),
                            focusedLeadingIconColor = Color(0xFF3B006F), unfocusedLeadingIconColor = Color(0xFF3B006F),
                            focusedTrailingIconColor = Color(0xFF3B006F), unfocusedTrailingIconColor = Color(0xFF3B006F),
                            focusedPlaceholderColor = Color(0xFF5A3574), unfocusedPlaceholderColor = Color(0xFF5A3574),
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (formulas.size >= 8) onNotice("最多同时绘制 8 条函数")
                        else if (!isGraphFormulaValid(formula)) onNotice("表达式无法解析；支持 y=f(x)、f(x,y)=g(x,y)、r=f(θ) 和 (x(t),y(t))")
                        else { formulas = (formulas + formula.trim()).distinct(); formulaField = TextFieldValue("") }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD7FF72), contentColor = Color(0xFF171318)),
                ) { Icon(Icons.Rounded.ShowChart, null); Spacer(Modifier.width(6.dp)); Text("绘制函数") }
                Button(
                    enabled = formula.isNotBlank() && !generating,
                    onClick = {
                        if (!aiConfigured) { onNotice("请先到跃跃助手中输入 DeepSeek API 密钥"); return@Button }
                        generating = true
                        scope.launch {
                            gateway.graph(formula).fold(
                                onSuccess = { generated ->
                                    val valid = generated.filter(::isGraphFormulaValid)
                                    if (valid.isEmpty()) onNotice("AI 没有生成可绘制的函数")
                                    else { formulas = (formulas + valid).distinct().takeLast(8); formulaField = TextFieldValue("") }
                                },
                                onFailure = { onNotice(it.message ?: "AI 生成失败") },
                            )
                            generating = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA77BFF), contentColor = Color(0xFF171318)),
                ) { if (generating) LoadingIndicator(Modifier.size(24.dp)) else Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text("AI 生成") }
                    }
                }
            }
        }
    }
    if (keyboardVisible) {
        ModalBottomSheet(
            onDismissRequest = { keyboardVisible = false },
            containerColor = Color(0xFF211C24),
            contentColor = Color.White,
        ) {
            Column(
                // Give the weighted key list a real height constraint so the fixed action row can
                // never be measured below the gesture area on tall-density phones.
                Modifier.fillMaxWidth().height(460.dp).windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp).padding(bottom = 52.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("专业数学键盘", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                OutlinedTextField(
                    value = formulaField,
                    onValueChange = { formulaField = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true,
                    label = { Text("当前表达式") },
                    leadingIcon = { Icon(Icons.Rounded.Functions, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE7D8FF), unfocusedContainerColor = Color(0xFFE7D8FF),
                        focusedTextColor = Color(0xFF171318), unfocusedTextColor = Color(0xFF171318),
                        focusedLabelColor = Color(0xFF3B006F), unfocusedLabelColor = Color(0xFF3B006F),
                        focusedLeadingIconColor = Color(0xFF3B006F), unfocusedLeadingIconColor = Color(0xFF3B006F),
                    ),
                )
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        mathKeys.forEach { (label, token) ->
                            Surface(
                                onClick = { formulaField = applyMathKey(formulaField, token) },
                                modifier = Modifier.widthIn(min = 50.dp).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (label in listOf("⌫", "AC")) Color(0xFF5C486A) else Color(0xFF342A3D),
                                contentColor = Color.White,
                            ) { Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text(label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) } }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { keyboardVisible = false }, modifier = Modifier.weight(1f)) { Text("完成输入") }
                    Button(
                        onClick = {
                            when {
                                formulas.size >= 8 -> onNotice("最多同时绘制 8 条函数")
                                !isGraphFormulaValid(formula) -> onNotice("当前表达式无法解析")
                                else -> {
                                    formulas = (formulas + formula.trim()).distinct()
                                    keyboardVisible = false
                                }
                            }
                        },
                        enabled = formula.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Rounded.ShowChart, null); Spacer(Modifier.width(6.dp)); Text("绘制函数") }
                }
            }
        }
    }
}

internal fun applyMathKey(value: TextFieldValue, token: String): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    return when (token) {
        "LEFT" -> value.copy(selection = TextRange((start - 1).coerceAtLeast(0)))
        "RIGHT" -> value.copy(selection = TextRange((end + 1).coerceAtMost(value.text.length)))
        "CLEAR" -> TextFieldValue("")
        "BACK" -> when {
            start != end -> value.copy(text = value.text.removeRange(start, end), selection = TextRange(start))
            start > 0 -> value.copy(text = value.text.removeRange(start - 1, start), selection = TextRange(start - 1))
            else -> value
        }
        else -> value.copy(
            text = value.text.replaceRange(start, end, token),
            selection = TextRange(start + token.length),
        )
    }
}

@Composable
private fun GraphCanvas(formulas: List<String>, colors: List<Color>, scale: Float, pan: Offset, onTransform: (Float, Offset) -> Unit, modifier: Modifier) {
    Canvas(modifier.pointerInput(Unit) { detectTransformGestures { _, delta, zoom, _ -> onTransform(zoom, delta) } }) {
        val origin = center + pan
        val grid = scale.coerceAtLeast(12f)
        val gridColor = Color.Gray.copy(alpha = .18f)
        var x = origin.x % grid
        while (x < size.width) { drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f); x += grid }
        var y = origin.y % grid
        while (y < size.height) { drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f); y += grid }
        drawLine(Color.Gray.copy(alpha = .7f), Offset(0f, origin.y), Offset(size.width, origin.y), 2f)
        drawLine(Color.Gray.copy(alpha = .7f), Offset(origin.x, 0f), Offset(origin.x, size.height), 2f)
        formulas.forEachIndexed { index, raw ->
            val color = colors[index % colors.size]
            val source = raw.trim().replace("π", "pi").replace("θ", "theta")
            when {
                source.startsWith("r=", true) -> {
                    val expression = compileExpression(source.substringAfter('=')) ?: return@forEachIndexed
                    var previous: Offset? = null
                    var theta = 0.0
                    while (theta <= Math.PI * 2.0 + .01) {
                        val radius = runCatching { expression.setVariable("theta", theta).evaluate() }.getOrNull()
                        val point = radius?.takeIf(Double::isFinite)?.let { Offset(origin.x + (it * kotlin.math.cos(theta) * scale).toFloat(), origin.y - (it * kotlin.math.sin(theta) * scale).toFloat()) }
                        if (point != null && previous != null && point.isNear(previous!!, size.height)) drawLine(color, previous!!, point, 4f, StrokeCap.Round)
                        previous = point; theta += .012
                    }
                }
                source.startsWith("(") && source.endsWith(")") && splitTopLevel(source.drop(1).dropLast(1)).size == 2 -> {
                    val parts = splitTopLevel(source.drop(1).dropLast(1))
                    val xExpression = compileExpression(parts[0]) ?: return@forEachIndexed
                    val yExpression = compileExpression(parts[1]) ?: return@forEachIndexed
                    var previous: Offset? = null
                    var t = -Math.PI * 4
                    while (t <= Math.PI * 4) {
                        val xv = runCatching { xExpression.setVariable("t", t).evaluate() }.getOrNull()
                        val yv = runCatching { yExpression.setVariable("t", t).evaluate() }.getOrNull()
                        val point = if (xv?.isFinite() == true && yv?.isFinite() == true) Offset(origin.x + (xv * scale).toFloat(), origin.y - (yv * scale).toFloat()) else null
                        if (point != null && previous != null && point.isNear(previous!!, size.height)) drawLine(color, previous!!, point, 4f, StrokeCap.Round)
                        previous = point; t += .018
                    }
                }
                source.contains('=') || source.contains('<') || source.contains('>') -> {
                    val separator = when { source.contains('=') -> '='; source.contains('<') -> '<'; else -> '>' }
                    val left = compileExpression(source.substringBefore(separator)) ?: return@forEachIndexed
                    val right = compileExpression(source.substringAfter(separator)) ?: return@forEachIndexed
                    val step = 7f
                    var py = 0f
                    while (py < size.height - step) {
                        var px = 0f
                        while (px < size.width - step) {
                            fun value(sx: Float, sy: Float): Double? = runCatching {
                                val xv = (sx - origin.x) / scale; val yv = (origin.y - sy) / scale
                                left.setVariable("x", xv.toDouble()).setVariable("y", yv.toDouble()).evaluate() - right.setVariable("x", xv.toDouble()).setVariable("y", yv.toDouble()).evaluate()
                            }.getOrNull()?.takeIf(Double::isFinite)
                            val a = value(px, py); val b = value(px + step, py); val c = value(px, py + step)
                            if (a != null && b != null && a.signDiffers(b)) drawLine(color, Offset(px + step / 2, py), Offset(px + step / 2, py + step), 2.5f)
                            if (a != null && c != null && a.signDiffers(c)) drawLine(color, Offset(px, py + step / 2), Offset(px + step, py + step / 2), 2.5f)
                            px += step
                        }
                        py += step
                    }
                }
                else -> {
                    val expression = compileExpression(source.substringAfter("y=", source)) ?: return@forEachIndexed
                    var previous: Offset? = null
                    var px = 0f
                    while (px <= size.width) {
                        val xv = (px - origin.x) / scale
                        val value = runCatching { expression.setVariable("x", xv.toDouble()).evaluate() }.getOrNull()
                        val py = value?.takeIf { it.isFinite() }?.let { origin.y - (it * scale).toFloat() }
                        val point = py?.takeIf { it in -size.height..size.height * 2 }?.let { Offset(px, it) }
                        if (point != null && previous != null && point.isNear(previous!!, size.height)) drawLine(color, previous!!, point, 4f, StrokeCap.Round)
                        previous = point; px += 2f
                    }
                }
            }
        }
    }
}

private fun compileExpression(source: String) = runCatching {
    val clean = source.trim().replace("π", "pi").replace("θ", "theta")
    require(clean.length in 1..240 && clean.matches(Regex("[0-9a-zA-Z_+\\-*/^()., ]+")))
    ExpressionBuilder(clean).variables("x", "y", "theta", "t").build()
}.getOrNull()

private fun isGraphFormulaValid(raw: String): Boolean {
    val source = raw.trim().replace("π", "pi").replace("θ", "theta")
    if (source.startsWith("r=", true)) return compileExpression(source.substringAfter('=')) != null
    if (source.startsWith("y=", true)) return compileExpression(source.substringAfter('=')) != null
    if (source.startsWith("(") && source.endsWith(")")) return splitTopLevel(source.drop(1).dropLast(1)).let { it.size == 2 && it.all { part -> compileExpression(part) != null } }
    val separator = listOf('=', '<', '>').firstOrNull(source::contains)
    return if (separator != null) compileExpression(source.substringBefore(separator)) != null && compileExpression(source.substringAfter(separator)) != null else compileExpression(source) != null
}

private fun splitTopLevel(source: String): List<String> {
    var depth = 0
    val split = source.indexOfFirst { char -> when (char) { '(' -> depth++; ')' -> depth--; ',' -> if (depth == 0) return@indexOfFirst true }; false }
    return if (split > 0) listOf(source.substring(0, split), source.substring(split + 1)) else emptyList()
}

private fun graphLabel(source: String) = when {
    source.trim().startsWith("r=", true) -> "极坐标 $source"
    source.trim().startsWith("(") -> "参数 $source"
    source.any { it == '=' || it == '<' || it == '>' } -> "关系 $source"
    else -> "y=$source"
}

private fun Offset.isNear(other: Offset, limit: Float) = kotlin.math.abs(y - other.y) < limit && kotlin.math.abs(x - other.x) < limit
private fun Double.signDiffers(other: Double) = (this <= 0.0 && other >= 0.0) || (this >= 0.0 && other <= 0.0)

@Composable
internal fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val license = remember { context.assets.open("licenses/emotion-ball-LICENSE.txt").bufferedReader().use { it.readText() } }
    val scannerNotice = remember { context.assets.open("licenses/oss-document-scanner-NOTICE.txt").bufferedReader().use { it.readText() } }
    val neteaseNotice = remember { context.assets.open("licenses/netease-cloud-music-NOTICE.txt").bufferedReader().use { it.readText() } }
    Scaffold(
        containerColor = ToolScreenBackground,
        contentColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.navigationBars)) {
        ToolScreenHeader("开源许可", onBack, subtitle = "来源、许可与本地修改")
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("私人非商业版本", style = MaterialTheme.typography.headlineSmall); Text("Emotion Ball 仅供个人学习研究，禁止商业使用。本 APK 不得上传应用商店或用于商业分发。", color = Color(0xFFFF9CB9)) }
            item { Text("Emotion Ball · sam70361", style = MaterialTheme.typography.titleLarge); Text(license, style = MaterialTheme.typography.bodySmall) }
            item { Text("OSS Document Scanner", style = MaterialTheme.typography.titleLarge); Text(scannerNotice, style = MaterialTheme.typography.bodySmall) }
            item { Text("ECDICT 雅思词库", style = MaterialTheme.typography.titleLarge); Text(remember { context.assets.open("licenses/ecdict-LICENSE.txt").bufferedReader().use { it.readText() } }, style = MaterialTheme.typography.bodySmall) }
            item { Text("netease-cloud-music 协议参考", style = MaterialTheme.typography.titleLarge); Text(neteaseNotice, style = MaterialTheme.typography.bodySmall) }
            item { Text("Bing 图片网页", style = MaterialTheme.typography.titleLarge); Text("由用户在 Bing 官方 HTTPS 图片页面中主动选择。应用只保存用户确认后的本地副本和来源信息，不调用已停用的 Bing Search API，也不自动抓取搜索首图。") }
            item { Text("SimplyGraph", style = MaterialTheme.typography.titleLarge); Text("仅参考功能方向；因仓库未声明许可证，未复制其源码。") }
        }
        }
    }
}

/**
 * Compatibility wrapper for the retired prototype assistant that still lives in this file.
 * Active tool destinations use [ToolScreenHeader] directly.
 */
@Composable
private fun ToolTopBar(title: String, onBack: () -> Unit) {
    ToolScreenHeader(title = title, onBack = onBack)
}

private fun loadScanBitmap(context: android.content.Context, uri: Uri): Bitmap {
    val bitmap = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) } ?: error("无法读取图片")
    val maxSide = 2200
    val ratio = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
    return if (ratio < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).roundToInt(), (bitmap.height * ratio).roundToInt(), true) else bitmap
}

private fun processScan(context: android.content.Context, bitmap: Bitmap, corners: List<DocumentPoint>, filter: DocumentFilter): File {
    val output = DocumentScannerCore.process(bitmap, corners, filter)
    val dir = File(context.filesDir, "scans/pages").apply { mkdirs() }
    return File(dir, "scan-${UUID.randomUUID()}.png").also { file -> file.outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) } }
}

private fun rotateScan(file: File) {
    val source = BitmapFactory.decodeFile(file.absolutePath) ?: return
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(90f) }, true)
    file.outputStream().use { rotated.compress(Bitmap.CompressFormat.PNG, 100, it) }
    if (rotated !== source) source.recycle()
}

private suspend fun sharePdf(context: android.content.Context, pages: List<File>, quality: Int, a4: Boolean) = withContext(Dispatchers.IO) {
    val pdf = PdfDocument()
    pages.forEachIndexed { index, file ->
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val pageWidth = if (a4) 1240 else bitmap.width
        val pageHeight = if (a4) 1754 else bitmap.height
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
        val scale = minOf(pageWidth.toFloat() / bitmap.width, pageHeight.toFloat() / bitmap.height) * (quality / 100f).coerceAtLeast(.5f)
        val targetWidth = bitmap.width * scale
        val targetHeight = bitmap.height * scale
        val left = (pageWidth - targetWidth) / 2f
        val top = (pageHeight - targetHeight) / 2f
        page.canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + targetWidth, top + targetHeight), null); pdf.finishPage(page)
    }
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "跃题扫描-${System.currentTimeMillis()}.pdf")
    file.outputStream().use(pdf::writeTo); pdf.close()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "分享扫描件").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
