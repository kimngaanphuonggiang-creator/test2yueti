package com.yueti.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyDataTest {
    private val now = 1_800_000_000_000L

    @Test
    fun forgottenWordReturnsToBoxOneAndReappearsAfterTenMinutes() {
        val result = nextVocabularyProgress(
            current = VocabularyProgressEntity("adapt", box = 4, reviewCount = 7, lapseCount = 2),
            wordKey = "adapt",
            rating = MemoryRating.Forgot,
            now = now,
        )

        assertEquals(1, result.box)
        assertEquals(now + 10 * 60_000L, result.nextReviewAt)
        assertEquals(8, result.reviewCount)
        assertEquals(3, result.lapseCount)
        assertFalse(result.isMastered)
    }

    @Test
    fun rememberedWordAdvancesThroughLeitnerIntervalsWithoutImplicitMastery() {
        val fromBoxTwo = nextVocabularyProgress(
            current = VocabularyProgressEntity("coherent", box = 2),
            wordKey = "coherent",
            rating = MemoryRating.Remembered,
            now = now,
        )
        val fromBoxFour = nextVocabularyProgress(
            current = VocabularyProgressEntity("coherent", box = 4),
            wordKey = "coherent",
            rating = MemoryRating.Remembered,
            now = now,
        )

        assertEquals(3, fromBoxTwo.box)
        assertEquals(now + 7L * 24L * 60L * 60_000L, fromBoxTwo.nextReviewAt)
        assertEquals(5, fromBoxFour.box)
        assertFalse(fromBoxFour.isMastered)
    }

    @Test
    fun explicitMasteryKeepsStarAndSchedulesThirtyDayReview() {
        val result = nextVocabularyProgress(
            current = VocabularyProgressEntity("derive", box = 3, isStarred = true),
            wordKey = "derive",
            rating = MemoryRating.Mastered,
            now = now,
        )

        assertEquals(5, result.box)
        assertTrue(result.isMastered)
        assertTrue(result.isStarred)
        assertEquals(now + 30L * 24L * 60L * 60_000L, result.nextReviewAt)
    }

    @Test
    fun todayQueuePrioritizesDueWordsAndRemovesDuplicates() {
        val due = listOf(word("a"), word("b"))
        val fresh = listOf(word("b"), word("c"), word("d"), word("e"), word("f"))

        assertEquals(listOf("a", "b", "c", "d", "e"), mergeVocabularyQueue(due, fresh, 5).map { it.wordKey })
    }

    @Test
    fun promptHashIsStableButChangesWithCustomInstruction() {
        val first = vocabularyPromptHash("Use {word} in an IELTS example")
        val second = vocabularyPromptHash("  Use {word} in an IELTS example  ")
        val changed = vocabularyPromptHash("Explain {word} with a contrast")

        assertEquals(first, second)
        assertEquals(24, first.length)
        assertTrue(first != changed)
    }

    private fun word(value: String) = VocabularyWordEntity(
        wordKey = value,
        word = value,
        phonetic = "",
        definition = "",
        translation = value,
        partOfSpeech = "",
        exchange = "",
        collins = 0,
        bnc = 0,
        frequency = 0,
    )
}
