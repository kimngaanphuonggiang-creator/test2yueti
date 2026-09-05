package com.yueti.app.music

import org.json.JSONObject
import java.net.URI

/** Kept separate from transport so CDN response variants can be regression-tested. */
internal fun normalizeNeteaseMediaUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val secure = when {
        trimmed.startsWith("//") -> "https:$trimmed"
        trimmed.startsWith("http://", ignoreCase = true) -> "https://${trimmed.substringAfter("://")}" 
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        else -> return null
    }
    val uri = runCatching { URI(secure) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    val allowed = host == "music.163.com" ||
        host.endsWith(".music.163.com") ||
        host.endsWith(".music.126.net") ||
        host.endsWith(".music.127.net")
    return secure.takeIf {
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.userInfo == null && uri.port == -1 && allowed
    }
}

internal fun parseNeteasePlaybackUrls(response: String): Map<String, String> {
    val data = JSONObject(response).optJSONArray("data") ?: return emptyMap()
    val items = buildList {
        repeat(data.length()) { index ->
            val item = data.optJSONObject(index) ?: return@repeat
            add(item.optLong("id").toString() to item.optString("url"))
        }
    }
    return normalizeNeteasePlaybackItems(items)
}

internal fun normalizeNeteasePlaybackItems(items: List<Pair<String, String>>): Map<String, String> =
    items.mapNotNull { (id, rawUrl) -> normalizeNeteaseMediaUrl(rawUrl)?.let { id to it } }.toMap()
