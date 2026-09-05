package com.yueti.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yueti.app.data.CommonsImageRepository
import com.yueti.app.data.BingImageRepository
import com.yueti.app.data.BingImageSelection
import com.yueti.app.data.DefaultVocabularyPrompt
import com.yueti.app.data.MemoryRating
import com.yueti.app.data.VocabularyCounts
import com.yueti.app.data.VocabularyExampleEntity
import com.yueti.app.data.VocabularyGroup
import com.yueti.app.data.VocabularyImageEntity
import com.yueti.app.data.VocabularyProgressEntity
import com.yueti.app.data.VocabularyRepository
import com.yueti.app.data.VocabularySettings
import com.yueti.app.data.VocabularySettingsStore
import com.yueti.app.data.VocabularyWordEntity
import com.yueti.app.data.enqueueVocabularyImagePrefetch
import com.yueti.app.data.expandVocabularyPrompt
import com.yueti.app.data.isUnmeteredNetwork
import com.yueti.app.data.vocabularyPromptHash
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VocabularyCardState(
    val word: VocabularyWordEntity,
    val progress: VocabularyProgressEntity = VocabularyProgressEntity(word.wordKey),
    val example: VocabularyExampleEntity? = null,
    val image: VocabularyImageEntity? = null,
)

internal data class VocabularyDeckCursor(
    val revision: Long = 0L,
    val group: VocabularyGroup = VocabularyGroup.Today,
    val currentIndex: Int = 0,
) {
    fun switchTo(nextGroup: VocabularyGroup): VocabularyDeckCursor = copy(
        revision = revision + 1L,
        group = nextGroup,
        currentIndex = 0,
    )
}

data class VocabularyUiState(
    val loading: Boolean = true,
    val group: VocabularyGroup = VocabularyGroup.Today,
    val cards: List<VocabularyCardState> = emptyList(),
    val currentIndex: Int = 0,
    val deckRevision: Long = 0L,
    val loadToken: Long = 0L,
    val targetInitialPage: Int = 0,
    val counts: VocabularyCounts = VocabularyCounts(0, 0, 0, 0, 0, 0),
    val settings: VocabularySettings = VocabularySettings(),
    val generatingExamples: Set<String> = emptySet(),
    val loadingImages: Set<String> = emptySet(),
    val imageErrors: Map<String, String> = emptyMap(),
    val libraryWords: List<VocabularyWordEntity> = emptyList(),
    val ratingCounts: Map<MemoryRating, Int> = emptyMap(),
    val aiConfigured: Boolean = false,
    val promptVersion: String = vocabularyPromptHash(VocabularySettings().promptTemplate),
    val notice: String? = null,
)

class VocabularyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VocabularyRepository(application)
    private val settingsStore = VocabularySettingsStore(application)
    private val imageRepository = CommonsImageRepository(application)
    private val bingImageRepository = BingImageRepository(application)
    private val _state = MutableStateFlow(VocabularyUiState())
    val state: StateFlow<VocabularyUiState> = _state.asStateFlow()
    private var visibleCardJob: Job? = null
    private var loadGroupJob: Job? = null
    private var nextLoadToken = 0L
    private val sessionStartedAt = System.currentTimeMillis()
    private val sessionSaved = AtomicBoolean(false)
    private val requeuedWords = linkedSetOf<String>()

    init {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            val savedKey = withContext(Dispatchers.IO) { AiCredentialStore.load(application) }
            if (savedKey.isNotBlank()) DeepSeekAiGateway.configure(savedKey)
            _state.update {
                it.copy(
                    settings = settings,
                    aiConfigured = DeepSeekAiGateway.configured,
                    promptVersion = vocabularyPromptHash(settings.promptTemplate),
                )
            }
            loadGroup(VocabularyGroup.Today)
        }
    }

    fun loadGroup(group: VocabularyGroup, forceCurrentExample: Boolean = false) {
        visibleCardJob?.cancel()
        loadGroupJob?.cancel()
        val token = ++nextLoadToken
        loadGroupJob = viewModelScope.launch {
            _state.update { previous ->
                val deck = VocabularyDeckCursor(previous.deckRevision, previous.group, previous.currentIndex).switchTo(group)
                previous.copy(
                    loading = true,
                    group = deck.group,
                    currentIndex = deck.currentIndex,
                    deckRevision = deck.revision,
                    loadToken = token,
                    targetInitialPage = 0,
                    cards = emptyList(),
                    imageErrors = emptyMap(),
                )
            }
            runCatching {
                repository.ensureImported()
                val settings = _state.value.settings
                val words = repository.loadWords(group, settings)
                val progress = repository.progressFor(words)
                val promptHash = vocabularyPromptHash(settings.promptTemplate)
                val cards = withContext(Dispatchers.IO) {
                    words.map { word ->
                        VocabularyCardState(
                            word = word,
                            progress = progress[word.wordKey] ?: VocabularyProgressEntity(word.wordKey),
                            example = repository.dao.getExample(word.wordKey, promptHash),
                            image = repository.dao.getImage(word.wordKey),
                        )
                    }
                }
                val counts = withContext(Dispatchers.IO) { repository.dao.getCounts(System.currentTimeMillis()) }
                cards to counts
            }.onSuccess { (cards, counts) ->
                if (_state.value.loadToken != token) return@onSuccess
                requeuedWords.clear()
                _state.update { it.copy(loading = false, cards = cards, counts = counts, ratingCounts = emptyMap()) }
                if (forceCurrentExample) {
                    cards.firstOrNull()?.word?.let { word ->
                        viewModelScope.launch { ensureExample(word, force = true) }
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                if (_state.value.loadToken != token) return@onFailure
                _state.update { it.copy(loading = false, notice = error.message ?: "雅思词库加载失败") }
            }
        }
    }

    fun onCardSettled(index: Int, deckRevision: Long = _state.value.deckRevision) {
        val snapshot = _state.value
        if (snapshot.deckRevision != deckRevision) return
        val card = snapshot.cards.getOrNull(index) ?: return
        _state.update { it.copy(currentIndex = index) }
        visibleCardJob?.cancel()
        visibleCardJob = viewModelScope.launch {
            delay(450)
            if (_state.value.deckRevision != deckRevision) return@launch
            if (_state.value.cards.getOrNull(_state.value.currentIndex)?.word?.wordKey != card.word.wordKey) return@launch
            if (_state.value.settings.autoExample && card.example == null) ensureExample(card.word)
        }
    }

    private suspend fun ensureExample(word: VocabularyWordEntity, force: Boolean = false, promptOverride: String? = null) {
        val settings = _state.value.settings
        val template = promptOverride?.takeIf(String::isNotBlank) ?: settings.promptTemplate
        val promptHash = vocabularyPromptHash(template)
        if (!force && repository.dao.getExample(word.wordKey, promptHash) != null) return
        if (!DeepSeekAiGateway.configured) {
            val saved = withContext(Dispatchers.IO) { AiCredentialStore.load(getApplication()) }
            if (saved.isNotBlank()) DeepSeekAiGateway.configure(saved)
        }
        if (!DeepSeekAiGateway.configured) {
            _state.update { it.copy(aiConfigured = false) }
            return
        }
        _state.update { it.copy(aiConfigured = true, generatingExamples = it.generatingExamples + word.wordKey) }
        try {
            val generated = DeepSeekAiGateway.vocabularyExample(
                VocabularyExampleRequest(
                    word = word.word,
                    phonetic = word.phonetic,
                    translation = word.translation,
                    definition = word.definition,
                    instruction = expandVocabularyPrompt(template, word),
                ),
            ).getOrThrow()
            val entity = VocabularyExampleEntity(
                wordKey = word.wordKey,
                promptHash = promptHash,
                example = generated.example,
                translation = generated.translation,
                usageNote = generated.usageNote,
                model = generated.model,
                generatedAt = System.currentTimeMillis(),
            )
            repository.dao.upsertExample(entity)
            _state.update { state ->
                state.copy(cards = state.cards.map { if (it.word.wordKey == word.wordKey) it.copy(example = entity) else it })
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _state.update { it.copy(notice = error.message ?: "跃跃例句生成失败") }
        } finally {
            _state.update { it.copy(generatingExamples = it.generatingExamples - word.wordKey) }
        }
    }

    private suspend fun ensureImage(word: VocabularyWordEntity, selectedIndex: Int) {
        _state.update {
            it.copy(
                loadingImages = it.loadingImages + word.wordKey,
                imageErrors = it.imageErrors - word.wordKey,
            )
        }
        try {
            val image = imageRepository.search(word, selectedIndex).getOrThrow()
            repository.dao.upsertImage(image)
            _state.update { state ->
                state.copy(
                    cards = state.cards.map { if (it.word.wordKey == word.wordKey) it.copy(image = image) else it },
                    imageErrors = state.imageErrors - word.wordKey,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val notice = when (error) {
                is UnknownHostException -> "当前网络不可用，记忆配图稍后再试"
                is SocketTimeoutException -> "记忆配图连接超时，稍后再试"
                else -> error.message?.takeIf { it.isNotBlank() } ?: "单词配图加载失败"
            }
            _state.update { it.copy(notice = notice, imageErrors = it.imageErrors + (word.wordKey to notice)) }
        } finally {
            _state.update { it.copy(loadingImages = it.loadingImages - word.wordKey) }
        }
    }

    fun loadImage(wordKey: String, next: Boolean = false) {
        val card = _state.value.cards.firstOrNull { it.word.wordKey == wordKey } ?: return
        viewModelScope.launch { ensureImage(card.word, if (next) (card.image?.selectedIndex ?: 0) + 1 else 0) }
    }

    fun retryImage(wordKey: String) {
        val card = _state.value.cards.firstOrNull { it.word.wordKey == wordKey } ?: return
        viewModelScope.launch {
            repository.dao.deleteImage(wordKey)
            _state.update { state ->
                state.copy(
                    cards = state.cards.map { if (it.word.wordKey == wordKey) it.copy(image = null) else it },
                    imageErrors = state.imageErrors - wordKey,
                )
            }
            ensureImage(card.word, selectedIndex = 0)
        }
    }

    fun saveBingImage(wordKey: String, result: BingImagePickResult) {
        val card = _state.value.cards.firstOrNull { it.word.wordKey == wordKey } ?: return
        viewModelScope.launch {
            _state.update { it.copy(loadingImages = it.loadingImages + wordKey, imageErrors = it.imageErrors - wordKey) }
            bingImageRepository.save(
                BingImageSelection(
                    wordKey = wordKey,
                    query = result.query,
                    imageUrl = result.imageUrl,
                    resultPageUrl = result.resultPageUrl,
                    sourceDomain = result.sourceDomain,
                ),
            ).onSuccess { image ->
                repository.dao.upsertImage(image)
                _state.update { state ->
                    state.copy(
                        cards = state.cards.map { if (it.word.wordKey == card.word.wordKey) it.copy(image = image) else it },
                        loadingImages = state.loadingImages - wordKey,
                        notice = "已保存 Bing 记忆配图",
                    )
                }
            }.onFailure { error ->
                val message = error.message ?: "Bing 图片保存失败，请换一张"
                _state.update { it.copy(loadingImages = it.loadingImages - wordKey, imageErrors = it.imageErrors + (wordKey to message), notice = message) }
            }
        }
    }

    fun clearImage(wordKey: String) {
        viewModelScope.launch {
            val old = _state.value.cards.firstOrNull { it.word.wordKey == wordKey }?.image
            repository.dao.deleteImage(wordKey)
            old?.localCachePath?.takeIf(String::isNotBlank)?.let { path -> withContext(Dispatchers.IO) { runCatching { java.io.File(path).delete() } } }
            _state.update { state ->
                state.copy(cards = state.cards.map { if (it.word.wordKey == wordKey) it.copy(image = null) else it }, imageErrors = state.imageErrors - wordKey)
            }
        }
    }

    fun regenerateExample(wordKey: String, promptOverride: String? = null) {
        val word = _state.value.cards.firstOrNull { it.word.wordKey == wordKey }?.word ?: return
        viewModelScope.launch { ensureExample(word, force = true, promptOverride = promptOverride) }
    }

    fun configureApiKey(value: String) {
        val key = value.trim()
        if (!key.startsWith("sk-") || key.length < 20) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { AiCredentialStore.save(getApplication(), key) }
            DeepSeekAiGateway.configure(key)
            _state.update { it.copy(aiConfigured = true, notice = "API 密钥已加密保存") }
            _state.value.cards.getOrNull(_state.value.currentIndex)?.word?.let { ensureExample(it, force = true) }
        }
    }

    fun toggleStar(wordKey: String) {
        viewModelScope.launch {
            val progress = repository.toggleStar(wordKey)
            _state.update { state ->
                state.copy(cards = state.cards.map { if (it.word.wordKey == wordKey) it.copy(progress = progress) else it })
            }
        }
    }

    fun rate(wordKey: String, rating: MemoryRating) {
        viewModelScope.launch {
            val progress = repository.rate(wordKey, rating)
            _state.update { state ->
                val updatedCards = state.cards.map { if (it.word.wordKey == wordKey) it.copy(progress = progress) else it }.toMutableList()
                if (rating == MemoryRating.Forgot && requeuedWords.add(wordKey)) {
                    updatedCards.firstOrNull { it.word.wordKey == wordKey }?.let(updatedCards::add)
                }
                state.copy(
                    cards = updatedCards,
                    ratingCounts = state.ratingCounts + (rating to ((state.ratingCounts[rating] ?: 0) + 1)),
                )
            }
            refreshCounts()
        }
    }

    fun updateSettings(settings: VocabularySettings) {
        viewModelScope.launch {
            val safe = settings.copy(
                groupSize = settings.groupSize.coerceIn(5, 50),
                promptTemplate = settings.promptTemplate.ifBlank { DefaultVocabularyPrompt },
            )
            val nextPromptVersion = vocabularyPromptHash(safe.promptTemplate)
            val promptChanged = nextPromptVersion != _state.value.promptVersion
            settingsStore.save(safe)
            val currentGroup = _state.value.group
            _state.update {
                it.copy(
                    settings = safe,
                    promptVersion = nextPromptVersion,
                    notice = if (promptChanged) "自定义提示词已应用，正在更新当前例句" else "词卡设置已保存",
                )
            }
            loadGroup(currentGroup, forceCurrentExample = promptChanged)
        }
    }

    fun searchLibrary(query: String) {
        viewModelScope.launch {
            val words = if (query.isBlank()) repository.dao.getAllWords(80) else repository.search(query)
            _state.update { it.copy(libraryWords = words) }
        }
    }

    fun refreshCounts() {
        viewModelScope.launch {
            val counts = withContext(Dispatchers.IO) { repository.dao.getCounts(System.currentTimeMillis()) }
            _state.update { it.copy(counts = counts) }
        }
    }

    fun saveSession() {
        if (!sessionSaved.compareAndSet(false, true)) return
        val counts = _state.value.ratingCounts
        viewModelScope.launch {
            repository.saveSession(
                startedAt = sessionStartedAt,
                remembered = counts[MemoryRating.Remembered] ?: 0,
                fuzzy = counts[MemoryRating.Fuzzy] ?: 0,
                forgotten = counts[MemoryRating.Forgot] ?: 0,
                mastered = counts[MemoryRating.Mastered] ?: 0,
            )
        }
    }

    fun consumeNotice() = _state.update { it.copy(notice = null) }
}
