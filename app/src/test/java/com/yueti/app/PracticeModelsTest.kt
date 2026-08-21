package com.yueti.app

import com.yueti.app.ui.BankType
import com.yueti.app.ui.ExamState
import com.yueti.app.ui.QuestionBank
import com.yueti.app.ui.ReviewItemKind
import com.yueti.app.ui.buildReviewItems
import com.yueti.app.ui.calculateStreak
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeModelsTest {
    @Test
    fun bankContainsExactlyFiftyUniqueQuestions() {
        assertEquals(25, QuestionBank.additions.size)
        assertEquals(25, QuestionBank.subtractions.size)
        assertEquals(50, QuestionBank.all.size)
        assertEquals(50, QuestionBank.all.map { it.id }.distinct().size)
    }

    @Test
    fun arithmeticStaysInsideZeroToOneHundred() {
        QuestionBank.all.forEach { question ->
            assertTrue(question.left in 0..100)
            assertTrue(question.right in 0..100)
            assertTrue(question.answer in 0..100)
            assertEquals(4, question.options.distinct().size)
            assertTrue(question.answer in question.options)
        }
    }

    @Test
    fun quantityRulesMatchEachBank() {
        assertEquals(listOf(10, 20, 25), QuestionBank.quantities(BankType.Addition))
        assertEquals(listOf(10, 20, 25), QuestionBank.quantities(BankType.Subtraction))
        assertEquals(listOf(10, 20, 30, 50), QuestionBank.quantities(BankType.Mixed))
        assertEquals(50, QuestionBank.createExam(BankType.Mixed, 50, 7).size)
    }

    @Test
    fun streakCountsConsecutiveLocalStudyDaysAndAllowsYesterdayAsTheLatestDay() {
        val today = LocalDate.of(2026, 8, 21)
        val completed = listOf(
            today.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            today.minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            today.minusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            today.minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )

        assertEquals(3, calculateStreak(completed, today, ZoneOffset.UTC))
    }

    @Test
    fun streakStartsFromTodayWhenThereIsAStudySessionToday() {
        val today = LocalDate.of(2026, 8, 21)
        val completed = listOf(
            today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            today.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            today.minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )

        assertEquals(3, calculateStreak(completed, today, ZoneOffset.UTC))
    }

    @Test
    fun reviewItemsExcludeCorrectAnswersAndDistinguishUnansweredQuestions() {
        val questions = QuestionBank.additions.take(3)
        val selections = mapOf(
            questions[0].id to questions[0].answer,
            questions[1].id to questions[1].options.first { it != questions[1].answer },
        )

        val review = buildReviewItems(questions, selections)

        assertEquals(listOf(questions[1].id, questions[2].id), review.map { it.question.id })
        assertEquals(listOf(ReviewItemKind.Wrong, ReviewItemKind.Unanswered), review.map { it.kind })
    }

    @Test
    fun examFindsTheNextUnansweredQuestionAndKeepsReviewFlagsIndependent() {
        val questions = QuestionBank.subtractions.take(4)
        val exam = ExamState(
            id = "exam",
            bank = BankType.Subtraction,
            questions = questions,
            selections = mapOf(questions[0].id to questions[0].answer),
            index = 0,
            startedAt = 1L,
            flaggedQuestionIds = setOf(questions[2].id),
        )

        assertEquals(1, exam.nextUnansweredIndex())
        assertTrue(exam.current.id !in exam.flaggedQuestionIds)
        assertTrue(questions[2].id in exam.flaggedQuestionIds)
    }
}
