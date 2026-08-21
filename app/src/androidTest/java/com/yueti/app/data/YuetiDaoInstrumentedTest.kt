package com.yueti.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YuetiDaoInstrumentedTest {
    private lateinit var database: YuetiDatabase
    private lateinit var dao: YuetiDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, YuetiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.dao()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun mostRecentlyPinnedRecordComesFirstWithoutUnpinningOthers() = runBlocking {
        dao.insertWrongRecords(
            listOf(
                wrong(questionId = "newer", wrongAt = 200L),
                wrong(questionId = "older", wrongAt = 100L),
            ),
        )
        val initial = dao.observeWrongRecords().first()
        dao.togglePinned(initial.first { it.questionId == "newer" }.id, pinnedAt = 1_000L)
        dao.togglePinned(initial.first { it.questionId == "older" }.id, pinnedAt = 2_000L)

        val pinned = dao.observeWrongRecords().first().filter { it.isPinned }
        assertEquals(listOf("older", "newer"), pinned.map { it.questionId })
    }

    private fun wrong(questionId: String, wrongAt: Long) = WrongRecordEntity(
        sessionId = "session",
        questionId = questionId,
        prompt = "$questionId = ?",
        selectedAnswer = 0,
        correctAnswer = 1,
        explanation = "解析",
        wrongAt = wrongAt,
    )
}
