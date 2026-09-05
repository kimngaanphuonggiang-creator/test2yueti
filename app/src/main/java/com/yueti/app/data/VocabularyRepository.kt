package com.yueti.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Html
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.zip.GZIPInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class VocabularyRepository(private val context: Context) {
    val dao: VocabularyDao = YuetiDatabase.get(context).vocabularyDao()

    suspend fun ensureImported() = ImportMutex.withLock {
        withContext(Dispatchers.IO) {
            val preferences = context.getSharedPreferences("vocabulary_dataset", Context.MODE_PRIVATE)
            if (preferences.getString("revision", null) == ECDICT_REVISION && dao.countWords() > 0) return@withContext
            // AAPT recognizes a real gzip stream, expands it, and exposes it without the
            // trailing `.gz` inside the packaged APK. Keep the generated source compressed in
            // the repository, but consume AAPT's runtime asset name here.
            val payload = context.assets.open("vocabulary/ielts_words.json").use { raw ->
                raw.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            val rows = JSONArray(payload)
            val batch = ArrayList<VocabularyWordEntity>(300)
            for (index in 0 until rows.length()) {
                val item = rows.getJSONObject(index)
                batch += VocabularyWordEntity(
                    wordKey = item.getString("k"),
                    word = item.getString("w"),
                    phonetic = item.getString("p"),
                    definition = item.optString("d"),
                    translation = item.getString("t"),
                    partOfSpeech = item.optString("o"),
                    exchange = item.optString("e"),
                    collins = item.optInt("c"),
                    bnc = item.optInt("b"),
                    frequency = item.optInt("f"),
                )
                if (batch.size == 300) {
                    dao.upsertWords(batch.toList())
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) dao.upsertWords(batch)
            preferences.edit().putString("revision", ECDICT_REVISION).apply()
        }
    }

    suspend fun loadWords(
        group: VocabularyGroup,
        settings: VocabularySettings,
        now: Long = System.currentTimeMillis(),
    ): List<VocabularyWordEntity> = withContext(Dispatchers.IO) {
        val limit = settings.groupSize.coerceIn(5, 50)
        when (group) {
            VocabularyGroup.Today -> {
                val due = dao.getDueWords(now, settings.includeMastered, limit)
                val fresh = dao.getNewWords((limit - due.size).coerceAtLeast(0))
                mergeVocabularyQueue(due, fresh, limit)
            }
            VocabularyGroup.Due -> dao.getDueWords(now, settings.includeMastered, limit)
            VocabularyGroup.New -> dao.getNewWords(limit)
            VocabularyGroup.Learning -> dao.getLearningWords(limit)
            VocabularyGroup.Mastered -> dao.getMasteredWords(limit)
            VocabularyGroup.Starred -> dao.getStarredWords(limit)
            VocabularyGroup.All -> dao.getAllWords(limit)
        }
    }

    suspend fun search(query: String, limit: Int = 80): List<VocabularyWordEntity> = withContext(Dispatchers.IO) {
        dao.searchWords(query.trim().lowercase(), limit)
    }

    suspend fun progressFor(words: List<VocabularyWordEntity>): Map<String, VocabularyProgressEntity> =
        withContext(Dispatchers.IO) {
            if (words.isEmpty()) emptyMap()
            else dao.getProgress(words.map(VocabularyWordEntity::wordKey)).associateBy(VocabularyProgressEntity::wordKey)
        }

    suspend fun rate(wordKey: String, rating: MemoryRating, now: Long = System.currentTimeMillis()): VocabularyProgressEntity =
        withContext(Dispatchers.IO) {
            val progress = nextVocabularyProgress(dao.getProgress(wordKey), wordKey, rating, now)
            dao.upsertProgress(progress)
            progress
        }

    suspend fun toggleStar(wordKey: String): VocabularyProgressEntity = withContext(Dispatchers.IO) {
        val current = dao.getProgress(wordKey) ?: VocabularyProgressEntity(wordKey)
        val progress = current.copy(isStarred = !current.isStarred)
        dao.upsertProgress(progress)
        progress
    }

    suspend fun saveSession(
        startedAt: Long,
        remembered: Int,
        fuzzy: Int,
        forgotten: Int,
        mastered: Int,
    ) = withContext(Dispatchers.IO) {
        val completedAt = System.currentTimeMillis()
        dao.insertSession(
            VocabularySessionEntity(
                id = UUID.randomUUID().toString(),
                startedAt = startedAt,
                completedAt = completedAt,
                reviewedCount = remembered + fuzzy + forgotten + mastered,
                rememberedCount = remembered,
                fuzzyCount = fuzzy,
                forgottenCount = forgotten,
                masteredCount = mastered,
            ),
        )
    }

    companion object {
        private val ImportMutex = Mutex()
    }
}

internal fun vocabularyPromptHash(template: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(("v1\n" + template.trim()).toByteArray(Charsets.UTF_8))
    return digest.take(12).joinToString("") { "%02x".format(it) }
}

internal fun expandVocabularyPrompt(template: String, word: VocabularyWordEntity): String =
    template.ifBlank { DefaultVocabularyPrompt }
        .replace("{word}", word.word)
        .replace("{phonetic}", word.phonetic)
        .replace("{translation}", word.translation.replace("\\n", " ").replace('\n', ' ').take(320))
        .replace("{definition}", word.definition.replace("\\n", " ").replace('\n', ' ').take(480))
        .replace("{level}", "IELTS")

internal fun Context.isUnmeteredNetwork(): Boolean {
    val manager = getSystemService(ConnectivityManager::class.java) ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}

class CommonsImageRepository(private val context: Context) {
    suspend fun search(word: VocabularyWordEntity, selectedIndex: Int = 0): Result<VocabularyImageEntity> = runCatching {
        withContext(Dispatchers.IO) {
            val hint = word.definition.replace("\\n", " ").lineSequence().firstOrNull().orEmpty().take(90)
            val queries = listOf(
                listOf(word.word, hint, "filetype:bitmap").filter(String::isNotBlank).joinToString(" "),
                "${word.word} filetype:bitmap",
                word.word,
            ).distinct()
            val candidates = queries.firstNotNullOfOrNull { query ->
                fetchCandidates(word, query, selectedIndex).takeIf(List<VocabularyImageEntity>::isNotEmpty)
            }
                ?: error("没有找到可验证许可的单词配图，请换一个词或稍后重试")
            candidates[selectedIndex.mod(candidates.size)]
        }
    }

    private fun fetchCandidates(
        word: VocabularyWordEntity,
        query: String,
        selectedIndex: Int,
    ): List<VocabularyImageEntity> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val endpoint = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&redirects=1" +
            "&gsrsearch=$encoded&gsrnamespace=6&gsrlimit=20&prop=imageinfo" +
            "&iiprop=url%7Cmime%7Cextmetadata&iiurlwidth=1200&format=json&origin=*"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 18_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "gzip")
            setRequestProperty("User-Agent", "Yueti/0.10.1 (private Android study app; https://github.com/skywind3000/ECDICT)")
        }
        return try {
            val status = connection.responseCode
            val rawStream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = rawStream?.let { source ->
                val decoded = if (connection.contentEncoding?.contains("gzip", ignoreCase = true) == true) GZIPInputStream(source) else source
                decoded.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.orEmpty()
            if (status !in 200..299) error("图片服务暂时不可用（$status）")
            val root = runCatching { JSONObject(response) }.getOrElse { error("图片服务返回异常，请稍后重试") }
            root.optJSONObject("error")?.let { apiError ->
                error(apiError.optString("info").ifBlank { "图片服务暂时繁忙，请重试" })
            }
            val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
            buildList {
                val keys = pages.keys()
                while (keys.hasNext()) {
                    val page = pages.optJSONObject(keys.next()) ?: continue
                    val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                    if (info.optString("mime") !in setOf("image/jpeg", "image/png", "image/webp")) continue
                    val metadata = info.optJSONObject("extmetadata") ?: JSONObject()
                    val license = metadata.value("LicenseShortName")
                    val allowed = license.contains("CC BY", ignoreCase = true) || license.contains("CC0", ignoreCase = true) ||
                        license.contains("public domain", ignoreCase = true) || license.startsWith("PD", ignoreCase = true)
                    if (!allowed) continue
                    val thumb = info.optString("thumburl").takeIf { it.startsWith("https://") } ?: continue
                    add(
                        VocabularyImageEntity(
                            wordKey = word.wordKey,
                            fileTitle = page.optString("title").removePrefix("File:"),
                            thumbnailUrl = thumb,
                            sourcePageUrl = info.optString("descriptionurl").takeIf { it.startsWith("https://") }
                                ?: "https://commons.wikimedia.org/wiki/${URLEncoder.encode(page.optString("title"), Charsets.UTF_8.name())}",
                            artist = metadata.value("Artist").plainText().ifBlank { "Wikimedia Commons contributor" },
                            licenseName = license,
                            licenseUrl = metadata.value("LicenseUrl").takeIf { it.startsWith("http") }.orEmpty(),
                            searchQuery = query,
                            selectedIndex = selectedIndex,
                            fetchedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }.sortedBy { if (it.fileTitle.contains(word.word, ignoreCase = true)) 0 else 1 }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.value(name: String): String = optJSONObject(name)?.optString("value").orEmpty()

    private fun String.plainText(): String = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}

class VocabularyImagePrefetchWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val wordKey = inputData.getString(WordKey) ?: return Result.failure()
        val repository = VocabularyRepository(applicationContext)
        return runCatching {
            repository.ensureImported()
            if (repository.dao.getImage(wordKey) != null) return Result.success()
            val word = repository.dao.getWord(wordKey) ?: return Result.failure()
            val image = CommonsImageRepository(applicationContext).search(word).getOrThrow()
            repository.dao.upsertImage(image)
            Result.success()
        }.getOrElse { if (runAttemptCount < 3) Result.retry() else Result.failure() }
    }

    companion object {
        const val WordKey = "word_key"
    }
}

internal fun enqueueVocabularyImagePrefetch(context: Context, wordKeys: List<String>) {
    val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()
    wordKeys.distinct().take(2).forEach { wordKey ->
        val request = OneTimeWorkRequestBuilder<VocabularyImagePrefetchWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(VocabularyImagePrefetchWorker.WordKey, wordKey).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "vocabulary-image-$wordKey",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
