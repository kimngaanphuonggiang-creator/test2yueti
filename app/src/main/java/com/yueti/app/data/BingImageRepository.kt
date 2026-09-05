package com.yueti.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BingImageSelection(
    val wordKey: String,
    val query: String,
    val imageUrl: String,
    val resultPageUrl: String,
    val sourceDomain: String,
)

class BingImageRepository(private val context: Context) {
    suspend fun save(selection: BingImageSelection): Result<VocabularyImageEntity> = runCatching {
        withContext(Dispatchers.IO) {
            val uri = URI(selection.imageUrl.trim())
            require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank()) { "仅支持 HTTPS 图片" }
            val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 18_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "image/avif,image/webp,image/png,image/jpeg")
                setRequestProperty("User-Agent", "Yueti/0.11.0 Android private study app")
                setRequestProperty("Referer", selection.resultPageUrl.takeIf { it.startsWith("https://") } ?: "https://www.bing.com/")
            }
            try {
                val status = connection.responseCode
                require(status in 200..299) { "图片服务器拒绝访问（$status）" }
                require(connection.url.protocol.equals("https", ignoreCase = true)) { "图片跳转到了不安全地址" }
                val mime = connection.contentType.orEmpty().substringBefore(';').lowercase()
                require(mime in setOf("image/jpeg", "image/png", "image/webp", "image/avif")) { "搜索结果不是可用图片" }
                val declaredLength = connection.contentLengthLong
                require(declaredLength <= MaxBytes || declaredLength < 0) { "图片超过 8MB，请换一张" }
                val bytes = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MaxBytes) { "图片超过 8MB，请换一张" }
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth >= 160 && bounds.outHeight >= 120) { "图片尺寸过小或无法解码，请换一张" }
                val largest = maxOf(bounds.outWidth, bounds.outHeight)
                var sample = 1
                while (largest / sample > 1800) sample *= 2
                val bitmap = BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 },
                ) ?: error("图片无法解码，请换一张")
                val directory = File(context.filesDir, "vocabulary-images").apply { mkdirs() }
                val digest = MessageDigest.getInstance("SHA-256")
                    .digest((selection.wordKey + selection.imageUrl).toByteArray())
                    .take(10).joinToString("") { "%02x".format(it) }
                val destination = File(directory, "${selection.wordKey.filter(Char::isLetterOrDigit).take(32)}-$digest.webp")
                destination.outputStream().use { output ->
                    require(bitmap.compress(Bitmap.CompressFormat.WEBP, 88, output)) { "图片缓存失败" }
                }
                bitmap.recycle()
                VocabularyImageEntity(
                    wordKey = selection.wordKey,
                    fileTitle = selection.sourceDomain.ifBlank { uri.host.orEmpty() },
                    thumbnailUrl = selection.imageUrl,
                    sourcePageUrl = selection.resultPageUrl,
                    artist = selection.sourceDomain.ifBlank { uri.host.orEmpty() },
                    licenseName = "Bing 搜索结果 · 版权归原作者",
                    licenseUrl = selection.resultPageUrl,
                    searchQuery = selection.query,
                    selectedIndex = 0,
                    fetchedAt = System.currentTimeMillis(),
                    provider = "BING_WEB_PICKED",
                    originalUrl = selection.imageUrl,
                    sourceDomain = selection.sourceDomain.ifBlank { uri.host.orEmpty() },
                    localCachePath = destination.absolutePath,
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        private const val MaxBytes = 8 * 1024 * 1024
    }
}
