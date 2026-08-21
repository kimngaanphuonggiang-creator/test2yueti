package com.yueti.app.ui

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeActionOrderTest {
    @Test
    fun pinGestureSettlesCardBeforeReorderingTheDatabaseList() = runBlocking {
        val events = mutableListOf<String>()

        settleBeforeReorder(
            settle = { events += "settled" },
            reorder = { events += "reordered" },
        )

        assertEquals(listOf("settled", "reordered"), events)
    }
}
