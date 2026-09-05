package com.yueti.app.ui

import com.yueti.app.data.ExamSessionEntity
import com.yueti.app.data.UserProfile
import com.yueti.app.data.WrongRecordEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class AppPage { Onboarding, Home, Exam, Results, WrongBook, Stats, Profile, Assistant, Scanner, Graph, Vocabulary, Licenses }

/**
 * The complete emotion id catalogue shipped by sam70361/emotion-ball.  Keeping the ids in one
 * place is important: the WebView bridge, persisted chat messages and result screen must all
 * agree on the same value instead of silently falling back to idle.
 */
enum class BotEmotion(val id: String) {
    Sleeping("00"), Waking("01"), Idle("02"), Curious("03"), SpacingOut("04"), Booting("05"),
    Dormant("06"), ShakeAwake("07"), Happy("10"), Puzzled("11"), Disappointed("12"),
    Surprised("13"), Shy("14"), Tired("15"), Focused("16"), Panicked("17"), Resigned("18"),
    Satisfied("19"), Confused("20"), Angry("21"), Thinking("30"), Receiving("31"),
    Working("32"), Celebrating("33"), Error("34"), WaitingInput("35"), NetworkLoading("36"),
    Recall("37"), Restricted("38"), Replying("39"), Searching("40"), Stopped("41");

    companion object {
        fun fromId(id: String?): BotEmotion = entries.firstOrNull { it.id == id } ?: Idle
    }
}

internal object EmotionBallIds {
    val all: Set<String> = BotEmotion.entries.mapTo(linkedSetOf()) { it.id }
}

enum class AssistantMode { Chat, DeepThinking, WebSearch }

data class GraphExpression(
    val formula: String,
    val color: Long,
    val visible: Boolean = true,
)

enum class ExamTransitionPhase { Entering, Answering, Submitting, Revealing }

enum class BankType(val label: String) {
    Addition("加法题库"),
    Subtraction("减法题库"),
    Mixed("加减混合"),
}

data class ArithmeticQuestion(
    val id: String,
    val left: Int,
    val right: Int,
    val operator: Char,
    val answer: Int,
    val options: List<Int>,
    val explanation: String,
) {
    val prompt: String get() = "$left $operator $right = ?"
}

object QuestionBank {
    val additions: List<ArithmeticQuestion> = buildQuestions(true)
    val subtractions: List<ArithmeticQuestion> = buildQuestions(false)
    val all: List<ArithmeticQuestion> = buildList {
        repeat(25) { index ->
            add(additions[index])
            add(subtractions[index])
        }
    }

    init {
        check(additions.size == 25 && subtractions.size == 25 && all.size == 50)
        check(all.map { it.id }.distinct().size == 50)
        check(subtractions.all { it.answer >= 0 })
    }

    fun quantities(bank: BankType): List<Int> = when (bank) {
        BankType.Mixed -> listOf(10, 20, 30, 50)
        else -> listOf(10, 20, 25)
    }

    fun createExam(bank: BankType, quantity: Int, seed: Int): List<ArithmeticQuestion> {
        val source = when (bank) {
            BankType.Addition -> additions
            BankType.Subtraction -> subtractions
            BankType.Mixed -> all
        }
        return source.shuffled(Random(seed)).take(quantity.coerceAtMost(source.size))
    }

    private fun buildQuestions(addition: Boolean): List<ArithmeticQuestion> {
        val random = Random(if (addition) 20260817 else 20260818)
        val pairs = linkedSetOf<Pair<Int, Int>>()
        while (pairs.size < 25) {
            if (addition) {
                val left = random.nextInt(0, 101)
                val right = random.nextInt(0, 101 - left)
                pairs += left to right
            } else {
                val left = random.nextInt(0, 101)
                val right = random.nextInt(0, left + 1)
                pairs += left to right
            }
        }
        return pairs.mapIndexed { index, (left, right) ->
            val answer = if (addition) left + right else left - right
            val operator = if (addition) '+' else '−'
            ArithmeticQuestion(
                id = "${if (addition) "ADD" else "SUB"}_${(index + 1).toString().padStart(2, '0')}",
                left = left,
                right = right,
                operator = operator,
                answer = answer,
                options = buildOptions(answer, index),
                explanation = if (addition) {
                    "把两个数合在一起：$left + $right = $answer。"
                } else {
                    "从 $left 中去掉 $right：$left − $right = $answer。"
                },
            )
        }
    }

    private fun buildOptions(answer: Int, index: Int): List<Int> {
        val values = linkedSetOf(answer)
        val deltas = listOf(-10, 10, -2, 2, -1, 1, -5, 5, -3, 3)
        for (delta in deltas.drop(index % 4) + deltas.take(index % 4)) {
            values += min(100, max(0, answer + delta))
            if (values.size == 4) break
        }
        var fallback = 0
        while (values.size < 4) values += fallback++
        return values.shuffled(Random(9000 + index))
    }
}

internal fun questionQuantityRange(bank: BankType): IntRange = when (bank) {
    BankType.Addition, BankType.Subtraction -> 1..QuestionBank.additions.size
    BankType.Mixed -> 1..QuestionBank.all.size
}

data class ExamState(
    val id: String,
    val bank: BankType,
    val questions: List<ArithmeticQuestion>,
    val selections: Map<String, Int> = emptyMap(),
    val index: Int = 0,
    val startedAt: Long,
    val submitting: Boolean = false,
    val flaggedQuestionIds: Set<String> = emptySet(),
) {
    val current: ArithmeticQuestion get() = questions[index]
    val answeredCount: Int get() = selections.size

    fun nextUnansweredIndex(fromIndex: Int = index): Int? {
        if (questions.isEmpty()) return null
        return (1..questions.size)
            .map { (fromIndex + it) % questions.size }
            .firstOrNull { questions[it].id !in selections }
    }
}

enum class ReviewItemKind { Wrong, Unanswered }

data class ReviewItem(
    val question: ArithmeticQuestion,
    val selectedAnswer: Int?,
    val kind: ReviewItemKind,
)

fun buildReviewItems(
    questions: List<ArithmeticQuestion>,
    selections: Map<String, Int>,
): List<ReviewItem> = questions.mapNotNull { question ->
    val selected = selections[question.id]
    when {
        selected == question.answer -> null
        selected == null -> ReviewItem(question, null, ReviewItemKind.Unanswered)
        else -> ReviewItem(question, selected, ReviewItemKind.Wrong)
    }
}

data class ExamResult(
    val exam: ExamState,
    val completedAt: Long,
    val elapsedSeconds: Long,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
) {
    val score: Int get() = (correctCount * 100f / exam.questions.size).toInt()
    val passed: Boolean get() = score >= 60
}

data class AppUiState(
    val page: AppPage = AppPage.Onboarding,
    val profile: UserProfile = UserProfile(),
    val selectedBank: BankType = BankType.Mixed,
    val selectedQuantity: Int = 10,
    val exam: ExamState? = null,
    val result: ExamResult? = null,
    val sessions: List<ExamSessionEntity> = emptyList(),
    val wrongRecords: List<WrongRecordEntity> = emptyList(),
    val notice: String? = null,
) {
    val totalAnswered: Int get() = sessions.sumOf { it.questionCount }
    val totalCorrect: Int get() = sessions.sumOf { it.correctCount }
    val totalAccuracy: Int get() = if (totalAnswered == 0) 0 else totalCorrect * 100 / totalAnswered
    val todayAnswered: Int get() = answeredOn(LocalDate.now())
    val todayProgress: Float get() = (todayAnswered.toFloat() / profile.dailyGoal).coerceIn(0f, 1f)
    val streakDays: Int get() = calculateStreak(sessions.map { it.completedAt }, LocalDate.now())

    private fun answeredOn(date: LocalDate): Int = sessions
        .filter { Instant.ofEpochMilli(it.completedAt).atZone(ZoneId.systemDefault()).toLocalDate() == date }
        .sumOf { it.questionCount }
}

internal fun calculateStreak(
    completedAt: List<Long>,
    today: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Int {
    val activeDays = completedAt.mapTo(mutableSetOf()) {
        Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
    }
    var cursor = if (today in activeDays) today else today.minusDays(1)
    var streak = 0
    while (cursor in activeDays) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}
