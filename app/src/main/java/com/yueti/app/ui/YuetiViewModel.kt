package com.yueti.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yueti.app.data.ExamAnswerEntity
import com.yueti.app.data.ExamSessionEntity
import com.yueti.app.data.ProfileStore
import com.yueti.app.data.WrongRecordEntity
import com.yueti.app.data.YuetiDatabase
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YuetiViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = YuetiDatabase.get(application).dao()
    private val profileStore = ProfileStore(application)
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(profileStore.profile, dao.observeSessions(), dao.observeWrongRecords()) { profile, sessions, wrong ->
                Triple(profile, sessions, wrong)
            }.collect { (profile, sessions, wrong) ->
                _uiState.update { state ->
                    state.copy(
                        profile = profile,
                        sessions = sessions,
                        wrongRecords = wrong,
                        page = if (!profile.completed) AppPage.Onboarding else if (state.page == AppPage.Onboarding) AppPage.Home else state.page,
                    )
                }
            }
        }
    }

    fun navigate(page: AppPage) {
        if (page != AppPage.Exam || _uiState.value.exam != null) {
            _uiState.update { it.copy(page = page) }
        }
    }

    fun setBank(bank: BankType) {
        _uiState.update { state ->
            val quantities = QuestionBank.quantities(bank)
            state.copy(selectedBank = bank, selectedQuantity = state.selectedQuantity.coerceAtMost(quantities.last()))
        }
    }

    fun setQuantity(quantity: Int) {
        _uiState.update { it.copy(selectedQuantity = quantity) }
    }

    fun saveProfile(name: String, school: String, avatarUri: Uri?, currentAvatarPath: String = "") {
        viewModelScope.launch {
            var avatarReadFailed = false
            val avatarPath = avatarUri?.let { uri ->
                runCatching { copyAvatar(uri) }.getOrElse {
                    avatarReadFailed = true
                    currentAvatarPath
                }
            } ?: currentAvatarPath
            runCatching { profileStore.save(name, school, avatarPath) }
                .onSuccess {
                    if (avatarReadFailed) {
                        _uiState.update { it.copy(notice = "头像读取失败，其他资料已保存") }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(notice = "资料保存失败，请重试") }
                }
        }
    }

    fun consumeNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun postNotice(message: String) {
        _uiState.update { it.copy(notice = message) }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            runCatching { profileStore.setDailyGoal(goal) }
                .onSuccess { _uiState.update { it.copy(notice = "每日目标已更新为 ${goal.coerceIn(10, 50)} 题") } }
                .onFailure { _uiState.update { it.copy(notice = "每日目标保存失败，请重试") } }
        }
    }

    private suspend fun copyAvatar(uri: Uri): String = withContext(Dispatchers.IO) {
        val target = File(getApplication<Application>().filesDir, "profile-avatar")
        getApplication<Application>().contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选头像" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target.absolutePath
    }

    fun startExam() {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val questions = QuestionBank.createExam(state.selectedBank, state.selectedQuantity, (now % Int.MAX_VALUE).toInt())
        _uiState.update {
            it.copy(
                page = AppPage.Exam,
                exam = ExamState(
                    id = UUID.randomUUID().toString(),
                    bank = state.selectedBank,
                    questions = questions,
                    startedAt = now,
                ),
                result = null,
            )
        }
    }

    fun selectAnswer(questionId: String, answer: Int) {
        _uiState.update { state ->
            val exam = state.exam ?: return@update state
            if (exam.questions.none { it.id == questionId }) return@update state
            state.copy(exam = exam.copy(selections = exam.selections + (questionId to answer)))
        }
    }

    fun goToQuestion(index: Int) {
        _uiState.update { state ->
            val exam = state.exam ?: return@update state
            state.copy(exam = exam.copy(index = index.coerceIn(exam.questions.indices)))
        }
    }

    fun toggleQuestionFlag() {
        _uiState.update { state ->
            val exam = state.exam ?: return@update state
            val questionId = exam.current.id
            val flags = if (questionId in exam.flaggedQuestionIds) {
                exam.flaggedQuestionIds - questionId
            } else {
                exam.flaggedQuestionIds + questionId
            }
            state.copy(exam = exam.copy(flaggedQuestionIds = flags))
        }
    }

    fun startReviewExam() {
        val result = _uiState.value.result ?: return
        val questions = buildReviewItems(result.exam.questions, result.exam.selections)
            .filter { it.kind == ReviewItemKind.Wrong }
            .map { it.question }
        if (questions.isEmpty()) {
            _uiState.update { it.copy(notice = "本次没有需要重练的错题") }
            return
        }
        _uiState.update { state ->
            state.copy(
                page = AppPage.Exam,
                exam = ExamState(
                    id = UUID.randomUUID().toString(),
                    bank = result.exam.bank,
                    questions = questions,
                    startedAt = System.currentTimeMillis(),
                ),
                result = null,
            )
        }
    }

    fun submitExam() {
        val snapshot = _uiState.value.exam ?: return
        if (snapshot.submitting) return
        val transitionStartedAt = System.currentTimeMillis()
        _uiState.update { it.copy(exam = snapshot.copy(submitting = true)) }
        viewModelScope.launch {
            try {
            val completedAt = System.currentTimeMillis()
            val elapsedSeconds = ((completedAt - snapshot.startedAt) / 1000).coerceAtLeast(0)
            val correct = snapshot.questions.count { snapshot.selections[it.id] == it.answer }
            val unanswered = snapshot.questions.count { it.id !in snapshot.selections }
            val wrong = snapshot.questions.size - correct - unanswered
            val answers = snapshot.questions.map { question ->
                val selected = snapshot.selections[question.id]
                ExamAnswerEntity(
                    sessionId = snapshot.id,
                    questionId = question.id,
                    prompt = question.prompt,
                    selectedAnswer = selected,
                    correctAnswer = question.answer,
                    isCorrect = selected == question.answer,
                )
            }
            val wrongRecords = snapshot.questions.mapNotNull { question ->
                val selected = snapshot.selections[question.id]
                if (selected == null || selected == question.answer) null else WrongRecordEntity(
                    sessionId = snapshot.id,
                    questionId = question.id,
                    prompt = question.prompt,
                    selectedAnswer = selected,
                    correctAnswer = question.answer,
                    explanation = question.explanation,
                    wrongAt = completedAt,
                )
            }
            dao.saveCompletedExam(
                session = ExamSessionEntity(
                    id = snapshot.id,
                    bank = snapshot.bank.name,
                    questionCount = snapshot.questions.size,
                    startedAt = snapshot.startedAt,
                    completedAt = completedAt,
                    elapsedSeconds = elapsedSeconds,
                    correctCount = correct,
                    wrongCount = wrong,
                    unansweredCount = unanswered,
                ),
                answers = answers,
                wrongRecords = wrongRecords,
            )
            delay((480L - (System.currentTimeMillis() - transitionStartedAt)).coerceAtLeast(0L))
            val result = ExamResult(snapshot.copy(submitting = false), completedAt, elapsedSeconds, correct, wrong, unanswered)
            _uiState.update { it.copy(page = AppPage.Results, exam = snapshot.copy(submitting = false), result = result) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        exam = state.exam?.copy(submitting = false),
                        notice = "交卷保存失败，请检查设备存储后重试",
                    )
                }
            }
        }
    }

    fun togglePinned(id: Long) = viewModelScope.launch { dao.togglePinned(id, System.currentTimeMillis()) }
    fun deleteWrong(id: Long) = viewModelScope.launch { dao.deleteWrong(id) }
    fun restoreWrong(record: WrongRecordEntity) = viewModelScope.launch { dao.restoreWrongRecord(record) }
}
