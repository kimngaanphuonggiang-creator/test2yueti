package com.yueti.app.ui

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.yueti.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface AiGateway {
    val configured: Boolean
    suspend fun chat(prompt: String, mode: AssistantMode): Result<String>
    fun streamChat(messages: List<AiPromptMessage>, mode: AssistantMode): Flow<ChatChunk>
    suspend fun graph(prompt: String): Result<List<String>>
    suspend fun vocabularyExample(request: VocabularyExampleRequest): Result<GeneratedVocabularyExample>
}

internal data class AiPromptMessage(val role: String, val content: String)
internal sealed interface ChatChunk {
    data class Reasoning(val text: String) : ChatChunk
    data class Content(val text: String) : ChatChunk
    data object Done : ChatChunk
}

internal data class VocabularyExampleRequest(
    val word: String,
    val phonetic: String,
    val translation: String,
    val definition: String,
    val instruction: String,
)

internal data class GeneratedVocabularyExample(
    val example: String,
    val translation: String,
    val usageNote: String,
    val model: String = "deepseek-v4-flash",
)

/**
 * Direct DeepSeek transport for the private build. The key may come from a local build override
 * or the encrypted user-entry flow; it is never logged or bundled as an APK resource. Web search
 * is deliberately not implemented.
 */
internal object DeepSeekAiGateway : AiGateway {
    @Volatile private var deviceApiKey: String = ""
    private val apiKey: String get() = deviceApiKey.ifBlank { BuildConfig.DEEPSEEK_API_KEY }
    override val configured: Boolean get() = apiKey.isNotBlank()

    fun configure(key: String) { deviceApiKey = key.trim() }

    override suspend fun chat(prompt: String, mode: AssistantMode): Result<String> = runCatching {
        require(configured) { "AI 服务未配置，请在构建环境中设置新的 DeepSeek 密钥" }
        require(mode != AssistantMode.WebSearch) { "联网搜索已关闭" }
        require(prompt.isNotBlank()) { "请输入问题" }
        val deep = mode == AssistantMode.DeepThinking
        val request = JSONObject()
            .put("model", if (deep) "deepseek-v4-pro" else "deepseek-v4-flash")
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", "你是跃跃，一位简洁、耐心的数学学习伙伴。用中文回答，不联网搜索，不声称访问实时信息。"))
                .put(JSONObject().put("role", "user").put("content", prompt.take(8_000))))
            .put("thinking", JSONObject().put("type", if (deep) "enabled" else "disabled"))
            .put("reasoning_effort", "high")
            .put("max_tokens", 2_048)
            .put("stream", false)
        val message = post(request).getJSONArray("choices").getJSONObject(0).getJSONObject("message")
        message.optString("content").trim().ifEmpty { error("AI 没有返回可显示内容") }
    }

    override fun streamChat(messages: List<AiPromptMessage>, mode: AssistantMode): Flow<ChatChunk> = channelFlow {
        require(configured) { "AI 服务未配置，请输入 DeepSeek API 密钥" }
        require(mode != AssistantMode.WebSearch) { "联网搜索已关闭" }
        require(messages.any { it.role == "user" && it.content.isNotBlank() }) { "请输入问题" }
        withContext(Dispatchers.IO) {
            val deep = mode == AssistantMode.DeepThinking
            val system = """
                你是跃跃，一位简洁、耐心的学习伙伴。用中文回答，不联网搜索，不声称访问实时信息。
                每次最终回答的第一行必须严格写成 [[emotion:ID]]，随后换行再写正文。
                ID 从完整表情集合中按语气选择：00,01,02,03,04,05,06,07,10,11,12,13,14,15,16,17,18,19,20,21,30,31,32,33,34,35,36,37,38,39,40,41。
                情绪标记属于界面控制信息，不要在正文中解释或重复它。
            """.trimIndent()
            val payload = JSONArray().put(JSONObject().put("role", "system").put("content", system))
            messages.forEach { payload.put(JSONObject().put("role", it.role).put("content", it.content.take(120_000))) }
            val request = JSONObject()
                .put("model", if (deep) "deepseek-v4-pro" else "deepseek-v4-flash")
                .put("messages", payload)
                .put("thinking", JSONObject().put("type", if (deep) "enabled" else "disabled"))
                .put("reasoning_effort", if (deep) "high" else "medium")
                .put("max_tokens", 4_096)
                .put("stream", true)
            val connection = openConnection(request)
            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    val response = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    val detail = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull()
                    error(detail?.take(240) ?: "AI 请求失败（$status）")
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        if (!line.startsWith("data:")) return@forEach
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") { trySend(ChatChunk.Done); return@forEach }
                        val delta = runCatching { JSONObject(data).getJSONArray("choices").getJSONObject(0).getJSONObject("delta") }.getOrNull() ?: return@forEach
                        delta.optString("reasoning_content").takeIf { it.isNotEmpty() }?.let { trySend(ChatChunk.Reasoning(it)) }
                        delta.optString("content").takeIf { it.isNotEmpty() }?.let { trySend(ChatChunk.Content(it)) }
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun graph(prompt: String): Result<List<String>> = runCatching {
        require(configured) { "AI 服务未配置，请在构建环境中设置新的 DeepSeek 密钥" }
        require(prompt.isNotBlank()) { "请描述要绘制的函数" }
        val request = JSONObject()
            .put("model", "deepseek-v4-flash")
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", "把用户的中文需求转换为二维函数表达式。只返回 JSON：{\"expressions\":[\"sin(x)\"]}。最多 8 条，只能使用 x、数字、pi、e、+ - * / ^、括号和 sin cos tan abs sqrt log exp。不得返回代码或解释。"))
                .put(JSONObject().put("role", "user").put("content", prompt.take(1_000))))
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("max_tokens", 512)
            .put("stream", false)
        val content = post(request).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
            .trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val values = JSONObject(content).getJSONArray("expressions")
        buildList {
            for (index in 0 until minOf(values.length(), 8)) {
                values.optString(index).trim().takeIf(String::isNotEmpty)?.let(::add)
            }
        }.also { require(it.isNotEmpty()) { "AI 未生成有效函数" } }
    }

    override suspend fun vocabularyExample(request: VocabularyExampleRequest): Result<GeneratedVocabularyExample> = runCatching {
        require(configured) { "AI 服务未配置，请先输入 DeepSeek API 密钥" }
        require(request.word.isNotBlank()) { "单词不能为空" }
        val payload = JSONObject()
            .put("model", "deepseek-v4-flash")
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject().put("role", "system").put(
                            "content",
                            """
                            你是严谨的雅思词汇教练。根据用户提供的词条和要求生成一个真实、自然、便于记忆的例句。
                            只能返回 JSON 对象，字段固定为 example、translation、usageNote。
                            example 必须包含目标单词或其正确词形，长度 12 到 28 个英文单词；translation 为准确中文翻译；usageNote 说明搭配、语法或语域，不超过 60 个汉字。
                            不得联网搜索，不得编造目标词不存在的词义，不得返回 Markdown。
                            """.trimIndent(),
                        ),
                    )
                    .put(
                        JSONObject().put("role", "user").put(
                            "content",
                            """
                            目标词：${request.word}
                            音标：${request.phonetic}
                            中文释义：${request.translation.take(600)}
                            英文释义：${request.definition.take(800)}
                            自定义要求：${request.instruction.take(2_000)}
                            """.trimIndent(),
                        ),
                    ),
            )
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("max_tokens", 512)
            .put("stream", false)
        val raw = post(payload).getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            .getString("content").trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsed = JSONObject(raw)
        val example = parsed.optString("example").trim()
        val translation = parsed.optString("translation").trim()
        val usage = parsed.optString("usageNote").trim()
        require(example.isNotBlank() && translation.isNotBlank() && usage.isNotBlank()) { "AI 返回的例句格式不完整" }
        require(example.split(Regex("\\s+")).size in 6..36) { "AI 返回的例句长度不合适" }
        GeneratedVocabularyExample(example, translation, usage)
    }

    private suspend fun post(body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val connection = openConnection(body)
        try {
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull()
                error(detail?.take(240) ?: "AI 请求失败（$status）")
            }
            JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(body: JSONObject): HttpURLConnection =
        (URL("https://api.deepseek.com/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
            outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
        }
}

/** Stores a user-entered API key encrypted by an app-scoped Android Keystore AES key. */
internal object AiCredentialStore {
    private const val Alias = "yueti_deepseek_key_v1"
    private const val Preferences = "yueti_ai_credentials"
    private const val Ciphertext = "deepseek_ciphertext"

    fun save(context: Context, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val encrypted = cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
        val payload = ByteArray(cipher.iv.size + encrypted.size).also {
            cipher.iv.copyInto(it)
            encrypted.copyInto(it, cipher.iv.size)
        }
        context.getSharedPreferences(Preferences, Context.MODE_PRIVATE).edit()
            .putString(Ciphertext, Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
    }

    fun load(context: Context): String = runCatching {
        val encoded = context.getSharedPreferences(Preferences, Context.MODE_PRIVATE).getString(Ciphertext, null)
            ?: return ""
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > 12)
        val iv = payload.copyOfRange(0, 12)
        val encrypted = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        }
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }.getOrDefault("")

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(Alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(Alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }
}
