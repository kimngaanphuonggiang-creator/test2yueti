package com.yueti.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.net.URI
import java.net.URLEncoder

data class BingImagePickResult(
    val imageUrl: String,
    val resultPageUrl: String,
    val sourceDomain: String,
    val query: String,
)

internal fun buildBingImageSearchUrl(word: String, firstMeaning: String): String {
    val query = listOf(word.trim(), firstMeaning.substringBefore('；').substringBefore(';').trim())
        .filter(String::isNotBlank)
        .joinToString(" ")
    val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
    return "https://www.bing.com/images/search?q=$encoded&safeSearch=Strict&setlang=zh-Hans"
}

internal fun validateBingImageUrl(value: String): Result<URI> = runCatching {
    val uri = URI(value.trim())
    require(uri.scheme.equals("https", ignoreCase = true)) { "仅支持 HTTPS 图片" }
    require(!uri.host.isNullOrBlank()) { "图片地址缺少有效域名" }
    require(uri.userInfo == null) { "图片地址不能包含账号信息" }
    uri
}

private class BingPickerBridge(
    private val onCandidate: (BingImagePickResult) -> Unit,
    private val query: String,
) {
    @JavascriptInterface
    fun candidate(imageUrl: String?, sourceUrl: String?) {
        val image = imageUrl.orEmpty()
        val imageUri = validateBingImageUrl(image).getOrNull() ?: return
        val source = sourceUrl.orEmpty().takeIf { validateBingImageUrl(it).isSuccess }
            ?: "https://www.bing.com/images/search"
        onCandidate(BingImagePickResult(image, source, imageUri.host.orEmpty(), query))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
internal fun BingImagePicker(
    word: String,
    translation: String,
    onDismiss: () -> Unit,
    onPick: (BingImagePickResult) -> Unit,
) {
    val context = LocalContext.current
    val meaning = translation.replace("\\n", "\n").lineSequence().firstOrNull().orEmpty()
    val query = listOf(word, meaning.substringBefore('；').substringBefore(';')).filter(String::isNotBlank).joinToString(" ")
    val searchUrl = remember(word, meaning) { buildBingImageSearchUrl(word, meaning) }
    var candidate by remember { mutableStateOf<BingImagePickResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val bridge = remember(query) { BingPickerBridge({ candidate = it }, query) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF151216), contentColor = Color.White) {
            Column(Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xF228222A),
                    contentColor = Color.White,
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回词卡") }
                        Spacer(Modifier.width(4.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Bing 记忆配图", style = MaterialTheme.typography.titleMedium)
                            Text(query, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD8C2FF), maxLines = 1)
                        }
                        Button(onClick = { candidate?.let(onPick) }, enabled = candidate != null) {
                            Icon(Icons.Rounded.Check, null)
                            Spacer(Modifier.width(6.dp))
                            Text("使用")
                        }
                    }
                }
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    AndroidView(
                        factory = { browserContext ->
                            WebView(browserContext).apply {
                                webView = this
                                setBackgroundColor(android.graphics.Color.WHITE)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.setSupportMultipleWindows(false)
                                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                settings.safeBrowsingEnabled = true
                                addJavascriptInterface(bridge, "YuetiImagePicker")
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val uri = request?.url ?: return true
                                        return uri.scheme != "https" || !uri.host.orEmpty().endsWith("bing.com", ignoreCase = true)
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { loading = true }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        loading = false
                                        view?.evaluateJavascript(SelectionScript, null)
                                    }

                                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                        handler?.cancel()
                                        loading = false
                                    }
                                }
                                setOnLongClickListener {
                                    val hit = hitTestResult
                                    val url = hit.extra.orEmpty()
                                    val uri = validateBingImageUrl(url).getOrNull()
                                    if (uri != null && hit.type in setOf(WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                                        candidate = BingImagePickResult(url, this.url ?: searchUrl, uri.host.orEmpty(), query)
                                        true
                                    } else false
                                }
                                loadUrl(searchUrl)
                            }
                        },
                        update = { browser -> if (browser.url.isNullOrBlank()) browser.loadUrl(searchUrl) },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (loading) {
                        Surface(
                            Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xEA211C24),
                            contentColor = Color.White,
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                LoadingIndicator(Modifier.height(24.dp))
                                Text("正在打开 Bing 图片", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    candidate?.let {
                        Surface(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(14.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xF228222A),
                            contentColor = Color.White,
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ImageSearch, null, tint = Color(0xFFD7FF72))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("已选中图片", style = MaterialTheme.typography.titleSmall)
                                    Text(it.sourceDomain, color = Color.White.copy(.68f), style = MaterialTheme.typography.bodySmall)
                                }
                                Text("点右上角“使用”保存", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD8C2FF))
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { WebView.setWebContentsDebuggingEnabled(false) }
    DisposableEffect(Unit) {
        onDispose {
            webView?.run { stopLoading(); removeJavascriptInterface("YuetiImagePicker"); destroy() }
            webView = null
        }
    }
}

private const val SelectionScript = """
(function(){
  if (window.__yuetiBingPickerInstalled) return;
  window.__yuetiBingPickerInstalled = true;
  document.addEventListener('click', function(event){
    var image = event.target && event.target.closest ? event.target.closest('img') : null;
    if (!image) return;
    setTimeout(function(){
      try {
        var card = image.closest('.iusc');
        var meta = card ? JSON.parse(card.getAttribute('m') || '{}') : {};
        var preview = document.querySelector('.imgContainer img.nofocus, .mainImage img, img[src^="https://tse"]');
        var imageUrl = meta.murl || (preview && (preview.currentSrc || preview.src)) || image.currentSrc || image.src || '';
        var sourceUrl = meta.purl || location.href;
        if (imageUrl) YuetiImagePicker.candidate(imageUrl, sourceUrl);
      } catch (_) {}
    }, 180);
  }, true);
})();
"""
