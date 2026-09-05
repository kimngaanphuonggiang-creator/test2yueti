package com.yueti.app.music

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.math.BigInteger
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject

interface NeteaseGateway {
    suspend fun account(cookie: String): Result<MusicAuthState.LoggedIn>
    suspend fun playlists(cookie: String, userId: String): Result<List<MusicPlaylist>>
    suspend fun playlistTracks(cookie: String, playlist: MusicPlaylist): Result<List<MusicTrack>>
    suspend fun search(cookie: String, query: String): Result<List<MusicTrack>>
    suspend fun resolveUrls(cookie: String, trackIds: List<String>): Result<Map<String, String>>
    suspend fun artwork(cookie: String, trackId: String): Result<String?>
}

class PrivateNeteaseGateway(
    private val callFactory: Call.Factory = OkHttpClient(),
) : NeteaseGateway {
    override suspend fun account(cookie: String): Result<MusicAuthState.LoggedIn> = request(
        "/weapi/w/nuser/account/get",
        JSONObject(),
        cookie,
    ).mapCatching { root ->
        val profile = root.optJSONObject("profile") ?: error("登录状态已失效，请重新扫码")
        MusicAuthState.LoggedIn(profile.optLong("userId").toString(), profile.optString("nickname", "网易云用户"))
    }

    override suspend fun playlists(cookie: String, userId: String): Result<List<MusicPlaylist>> = request(
        "/weapi/user/playlist",
        JSONObject().put("uid", userId).put("offset", 0).put("limit", 100).put("includeVideo", true),
        cookie,
    ).mapCatching { root ->
        root.optJSONArray("playlist").objects().map { item ->
            MusicPlaylist(
                id = item.optLong("id").toString(),
                name = item.optString("name"),
                coverUrl = normalizeNeteaseMediaUrl(item.optString("coverImgUrl")).orEmpty(),
                trackCount = item.optInt("trackCount"),
                subscribed = item.optBoolean("subscribed"),
            )
        }
    }

    override suspend fun playlistTracks(cookie: String, playlist: MusicPlaylist): Result<List<MusicTrack>> = request(
        "/weapi/v6/playlist/detail",
        JSONObject().put("id", playlist.id).put("n", 1000).put("s", 8),
        cookie,
    ).mapCatching { root ->
        root.optJSONObject("playlist")?.optJSONArray("tracks").objects().map { it.toTrack(playlist) }
    }

    override suspend fun search(cookie: String, query: String): Result<List<MusicTrack>> = request(
        "/weapi/cloudsearch/get/web",
        JSONObject().put("s", query.trim()).put("type", 1).put("limit", 40).put("offset", 0).put("total", true),
        cookie,
    ).mapCatching { root -> root.optJSONObject("result")?.optJSONArray("songs").objects().map { it.toTrack() } }

    override suspend fun resolveUrls(cookie: String, trackIds: List<String>): Result<Map<String, String>> {
        if (trackIds.isEmpty()) return Result.success(emptyMap())
        val ids = JSONArray(trackIds.map { it.toLongOrNull() ?: it })
        return request(
            "/weapi/song/enhance/player/url/v1",
            JSONObject().put("ids", ids).put("level", "standard").put("encodeType", "aac"),
            cookie,
        ).mapCatching { root -> parseNeteasePlaybackUrls(root.toString()) }
    }

    override suspend fun artwork(cookie: String, trackId: String): Result<String?> {
        val id = trackId.toLongOrNull() ?: return Result.success(null)
        return request(
            "/weapi/v3/song/detail",
            JSONObject().put("c", JSONArray().put(JSONObject().put("id", id)).toString()),
            cookie,
        ).mapCatching { root ->
            val song = root.optJSONArray("songs")?.optJSONObject(0)
            val album = song?.optJSONObject("al") ?: song?.optJSONObject("album")
            normalizeNeteaseMediaUrl(album?.optString("picUrl").orEmpty())
        }
    }

    private suspend fun request(path: String, payload: JSONObject, cookie: String): Result<JSONObject> = runCatching {
        withContext(Dispatchers.IO) {
            val csrf = cookie.split(';').firstNotNullOfOrNull { part ->
                part.trim().takeIf { it.startsWith("__csrf=") }?.substringAfter('=')
            }.orEmpty()
            payload.put("csrf_token", csrf)
            val encrypted = WeApiCrypto.encrypt(payload.toString())
            val url = BaseUrl.toHttpUrl().newBuilder()
                .addPathSegments(path.removePrefix("/"))
                .addQueryParameter("csrf_token", csrf)
                .build()
            val body = FormBody.Builder()
                .add("params", encrypted.params)
                .add("encSecKey", encrypted.encSecKey)
                .build()
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Cookie", cookie)
                .header("Origin", BaseUrl)
                .header("Referer", "$BaseUrl/")
                .header("User-Agent", DesktopUserAgent)
                .build()
            callFactory.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                require(response.isSuccessful) { "网易云服务暂时不可用（${response.code}）" }
                val root = JSONObject(text)
                val code = root.optInt("code", 200)
                require(code in 200..299) {
                    when (code) {
                        301 -> "登录状态已失效，请重新扫码"
                        8821 -> "网易云要求完成行为验证，请重新打开登录页"
                        else -> root.optString("message").ifBlank { "网易云请求失败（$code）" }
                    }
                }
                root
            }
        }
    }

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else buildList {
        repeat(length()) { index -> optJSONObject(index)?.let(::add) }
    }

    private fun JSONObject.toTrack(playlist: MusicPlaylist? = null): MusicTrack {
        val albumObject = optJSONObject("al") ?: optJSONObject("album") ?: JSONObject()
        val artists = optJSONArray("ar") ?: optJSONArray("artists")
        val artist = artists.objects().joinToString(" / ") { it.optString("name") }.ifBlank { "未知歌手" }
        val privilege = optJSONObject("privilege")
        return MusicTrack(
            id = optLong("id").toString(),
            title = optString("name", "未知歌曲"),
            artist = artist,
            album = albumObject.optString("name"),
            artworkUrl = normalizeNeteaseMediaUrl(albumObject.optString("picUrl")).orEmpty(),
            playlistId = playlist?.id.orEmpty(),
            playlistName = playlist?.name.orEmpty(),
            durationMs = optLong("dt", optLong("duration")),
            playable = privilege?.optInt("st", 0) != -200,
        )
    }

    private companion object {
        const val BaseUrl = "https://music.163.com"
        const val DesktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36"
    }
}

private object WeApiCrypto {
    private const val Nonce = "0CoJUm6Qyw8W8jud"
    private const val Iv = "0102030405060708"
    private const val PublicExponent = "010001"
    private const val Modulus = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7"

    data class Payload(val params: String, val encSecKey: String)

    fun encrypt(text: String): Payload {
        val secret = buildString(16) {
            val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            val random = SecureRandom()
            repeat(16) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
        val first = aes(text, Nonce)
        val second = aes(first, secret)
        val reversed = secret.reversed().toByteArray(Charsets.UTF_8)
        val encrypted = BigInteger(1, reversed).modPow(BigInteger(PublicExponent, 16), BigInteger(Modulus, 16))
            .toString(16).padStart(256, '0')
        return Payload(second, encrypted)
    }

    private fun aes(value: String, key: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(), "AES"), IvParameterSpec(Iv.toByteArray()))
        return Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
}

object MusicCredentialStore {
    private const val Alias = "yueti_netease_cookie_v1"
    private const val Preference = "music_credentials"

    fun save(context: Context, cookie: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        context.getSharedPreferences(Preference, Context.MODE_PRIVATE).edit()
            .putString("cipher", Base64.encodeToString(cipher.doFinal(cookie.toByteArray()), Base64.NO_WRAP))
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun load(context: Context): String = runCatching {
        val prefs = context.getSharedPreferences(Preference, Context.MODE_PRIVATE)
        val encrypted = Base64.decode(prefs.getString("cipher", ""), Base64.NO_WRAP)
        val iv = Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)
        if (encrypted.isEmpty() || iv.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        }
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }.getOrDefault("")

    fun clear(context: Context) {
        context.getSharedPreferences(Preference, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(Alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(Alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
