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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.objecthunter.exp4j.ExpressionBuilder
import org.opencv.android.OpenCVLoader
import java.io.File
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToInt

private val AllowedEmotionIds = setOf("00", "01", "02", "20", "21", "30", "31", "32", "39")

@Composable
internal fun EmotionBallView(
    emotion: BotEmotion,
    active: Boolean,
    lite: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTap by rememberUpdatedState(onTap)
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(36.dp)),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                settings.blockNetworkLoads = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
                }
                addJavascriptInterface(object {
                    @JavascriptInterface fun onTap() = post { currentTap() }
                }, "YuetiBot")
                loadUrl("file:///android_asset/emotion-ball/bot.html")
            }
        },
        update = { web ->
            val id = emotion.id.takeIf(AllowedEmotionIds::contains) ?: BotEmotion.Idle.id
            web.evaluateJavascript("window.YuetiEmotion&&window.YuetiEmotion.set('$id')", null)
            web.evaluateJavascript("window.YuetiEmotion&&window.YuetiEmotion.active(${active})", null)
            web.evaluateJavascript("window.YuetiEmotion&&window.YuetiEmotion.lite(${lite})", null)
        },
        onRelease = { it.removeJavascriptInterface("YuetiBot"); it.destroy() },
    )
}

@Composable
internal fun AssistantScreen(onBack: () -> Unit, onNavigate: (AppPage) -> Unit) {
    var mode by remember { mutableStateOf(AssistantMode.Chat) }
    var draft by remember { mutableStateOf("") }
    Scaffold(
        topBar = { ToolTopBar("跃题助手", onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.navigationBars).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            EmotionBallView(BotEmotion.Idle, active = true, lite = false, onTap = {}, Modifier.fillMaxWidth().weight(.8f))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistantMode.entries.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        enabled = item == AssistantMode.Chat,
                        onClick = { mode = item },
                        label = { Text(when (item) { AssistantMode.Chat -> "对话"; AssistantMode.DeepThinking -> "深度思考"; AssistantMode.WebSearch -> "联网搜索" }) },
                        leadingIcon = { Icon(when (item) { AssistantMode.Chat -> Icons.Rounded.Chat; AssistantMode.DeepThinking -> Icons.Rounded.Psychology; AssistantMode.WebSearch -> Icons.Rounded.TravelExplore }, null) },
                    )
                }
            }
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("离线助手已就绪", style = MaterialTheme.typography.titleMedium)
                    Text("机器人交互和本地学习工具可以使用。AI 对话、深度思考和联网搜索将在配置安全代理后开放。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { onNavigate(AppPage.Scanner) }, label = { Text("扫描文稿") }, leadingIcon = { Icon(Icons.Rounded.DocumentScanner, null) })
                        AssistChip(onClick = { onNavigate(AppPage.Graph) }, label = { Text("函数图像") }, leadingIcon = { Icon(Icons.Rounded.ShowChart, null) })
                    }
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("输入问题") },
                supportingText = { Text("AI 服务未配置，本版不会生成虚假回答") },
                trailingIcon = { IconButton(enabled = false, onClick = {}) { Icon(Icons.Rounded.Send, "发送") } },
            )
        }
    }
}

@Composable
internal fun ScannerScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pages by remember { mutableStateOf<List<File>>(emptyList()) }
    var currentCapture by remember { mutableStateOf<Pair<Uri, File>?>(null) }
    var processing by remember { mutableStateOf(false) }

    suspend fun importUri(uri: Uri) {
        processing = true
        val file = withContext(Dispatchers.IO) { processScan(context, uri) }
        pages = pages + file
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

    Scaffold(topBar = { ToolTopBar("文档扫描", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        Text("全部处理都在本机完成，不上传图片。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(pages, key = { _, f -> f.absolutePath }) { index, file ->
                    val bitmap = remember(file, file.lastModified()) { BitmapFactory.decodeFile(file.absolutePath) }
                    Card(shape = RoundedCornerShape(24.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, Modifier.size(92.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(12.dp)) {
                                Text("第 ${index + 1} 页", style = MaterialTheme.typography.titleMedium)
                                Text("白底黑字 · 本地文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Button(onClick = { scope.launch { sharePdf(context, pages); onNotice("PDF 已生成") } }, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                    Icon(Icons.Rounded.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text("导出并分享 PDF")
                }
            }
        }
    }
}

@Composable
internal fun GraphScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    var formula by remember { mutableStateOf("sin(x)") }
    var formulas by remember { mutableStateOf(listOf("sin(x)")) }
    var scale by remember { mutableFloatStateOf(42f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val colors = listOf(Color(0xFF7C3AED), Color(0xFF00A884), Color(0xFFFF5A7D), Color(0xFF2979FF), Color(0xFFFF9800))
    Scaffold(topBar = { ToolTopBar("函数图像", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = formula, onValueChange = { formula = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("y = f(x)") }, leadingIcon = { Icon(Icons.Rounded.Functions, null) },
                trailingIcon = { IconButton(onClick = {
                    if (formulas.size >= 8) onNotice("最多同时绘制 8 条函数")
                    else if (compileExpression(formula) == null) onNotice("表达式无法解析，请检查函数和括号")
                    else { formulas = (formulas + formula.trim()).distinct(); formula = "" }
                }) { Icon(Icons.Rounded.Add, "添加函数") } },
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formulas.forEachIndexed { index, value -> InputChip(selected = true, onClick = { formula = value }, label = { Text("y=$value") }, avatar = { Box(Modifier.size(12.dp).background(colors[index % colors.size], RoundedCornerShape(6.dp))) }, trailingIcon = { IconButton(onClick = { formulas = formulas - value }, Modifier.size(28.dp)) { Icon(Icons.Rounded.Close, "移除") } }) }
            }
            GraphCanvas(formulas, colors, scale, pan, onTransform = { zoom, delta -> scale = (scale * zoom).coerceIn(12f, 180f); pan += delta }, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainer))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { scale = 42f; pan = Offset.Zero }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.CenterFocusStrong, null); Spacer(Modifier.width(6.dp)); Text("复位") }
                Button(enabled = false, onClick = {}, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text("AI 生成") }
            }
            Text("支持 sin、cos、tan、abs、sqrt、log、exp 与 π/e。AI 服务未配置。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.navigationBarsPadding())
        }
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
        formulas.forEachIndexed { index, source ->
            val expression = compileExpression(source) ?: return@forEachIndexed
            var previous: Offset? = null
            var px = 0f
            while (px <= size.width) {
                val xv = (px - origin.x) / scale
                val value = runCatching { expression.setVariable("x", xv.toDouble()).evaluate() }.getOrNull()
                val py = value?.takeIf { it.isFinite() }?.let { origin.y - (it * scale).toFloat() }
                val point = py?.takeIf { it in -size.height..size.height * 2 }?.let { Offset(px, it) }
                if (point != null && previous != null && kotlin.math.abs(point.y - previous!!.y) < size.height) drawLine(colors[index % colors.size], previous!!, point, 4f, StrokeCap.Round)
                previous = point; px += 2f
            }
        }
    }
}

private fun compileExpression(source: String) = runCatching {
    val clean = source.trim().replace("π", "pi")
    require(clean.length in 1..200 && clean.matches(Regex("[0-9a-zA-Z_+\\-*/^()., ]+")))
    ExpressionBuilder(clean).variable("x").build()
}.getOrNull()

@Composable
internal fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val license = remember { context.assets.open("licenses/emotion-ball-LICENSE.txt").bufferedReader().use { it.readText() } }
    Scaffold(topBar = { ToolTopBar("开源许可", onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("私人非商业版本", style = MaterialTheme.typography.headlineSmall); Text("Emotion Ball 仅供个人学习研究，禁止商业使用。本 APK 不得上传应用商店或用于商业分发。", color = MaterialTheme.colorScheme.error) }
            item { Text("Emotion Ball · sam70361", style = MaterialTheme.typography.titleLarge); Text(license, style = MaterialTheme.typography.bodySmall) }
            item { Text("OSS Document Scanner", style = MaterialTheme.typography.titleLarge); Text("扫描交互参考项目，MIT License。跃题使用原生 Compose 实现。") }
            item { Text("SimplyGraph", style = MaterialTheme.typography.titleLarge); Text("仅参考功能方向；因仓库未声明许可证，未复制其源码。") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolTopBar(title: String, onBack: () -> Unit) = TopAppBar(
    title = { Text(title) },
    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") } },
)

private fun processScan(context: android.content.Context, uri: Uri): File {
    val bitmap = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) } ?: error("无法读取图片")
    val maxSide = 2200
    val ratio = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
    val scaled = if (ratio < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).roundToInt(), (bitmap.height * ratio).roundToInt(), true) else bitmap
    val output = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(scaled.width * scaled.height)
    scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
    for (i in pixels.indices) {
        val c = pixels[i]; val gray = (AndroidColor.red(c) * .299 + AndroidColor.green(c) * .587 + AndroidColor.blue(c) * .114).roundToInt()
        val bw = if (gray > 168) 255 else 0; pixels[i] = AndroidColor.rgb(bw, bw, bw)
    }
    output.setPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
    val dir = File(context.filesDir, "scans/pages").apply { mkdirs() }
    return File(dir, "scan-${UUID.randomUUID()}.png").also { file -> file.outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) } }
}

private fun rotateScan(file: File) {
    val source = BitmapFactory.decodeFile(file.absolutePath) ?: return
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(90f) }, true)
    file.outputStream().use { rotated.compress(Bitmap.CompressFormat.PNG, 100, it) }
    if (rotated !== source) source.recycle()
}

private suspend fun sharePdf(context: android.content.Context, pages: List<File>) = withContext(Dispatchers.IO) {
    val pdf = PdfDocument()
    pages.forEachIndexed { index, file ->
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create())
        page.canvas.drawBitmap(bitmap, 0f, 0f, null); pdf.finishPage(page)
    }
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "跃题扫描-${System.currentTimeMillis()}.pdf")
    file.outputStream().use(pdf::writeTo); pdf.close()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "分享扫描件").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
