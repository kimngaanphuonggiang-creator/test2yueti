package com.yueti.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutStoreTest {
    @Test
    fun freshInstallEmptyLayoutCompletesEveryModuleWithoutOverlap() {
        val normalized = normalizeHomeLayout(emptyList())
        assertEquals(HomeModuleId.entries.toSet(), normalized.map { it.id }.toSet())
        val cells = normalized.filterNot { it.hidden }.flatMap { item ->
            (item.y until item.y + item.height).flatMap { row ->
                (item.x until item.x + item.width).map { column -> row to column }
            }
        }
        assertEquals(cells.size, cells.distinct().size)
    }

    @Test
    fun overlappingModulesAreMovedToTheNearestFreeGridCells() {
        val source = HomeLayoutDefaults.layouts.map {
            when (it.id) {
                HomeModuleId.Profile -> it.copy(x = 0, y = 0)
                HomeModuleId.Totals -> it.copy(x = 0, y = 0)
                else -> it
            }
        }

        val normalized = normalizeHomeLayout(source, priority = HomeModuleId.Profile)
        val visibleCells = normalized.filterNot { it.hidden }.flatMap { item ->
            (item.y until item.y + item.height).flatMap { row ->
                (item.x until item.x + item.width).map { column -> row to column }
            }
        }

        assertEquals(visibleCells.size, visibleCells.distinct().size)
        assertEquals(0, normalized.first { it.id == HomeModuleId.Profile }.y)
    }

    @Test
    fun requiredModulesNeverDisappearWhenAResizeCannotFitAtTheRequestedPosition() {
        val crowded = HomeLayoutDefaults.layouts.map {
            if (it.id == HomeModuleId.Assistant) it.copy(x = 3, y = 9, width = 4, height = 3) else it
        }
        val normalized = normalizeHomeLayout(crowded, priority = HomeModuleId.Assistant)

        assertFalse(normalized.first { it.id == HomeModuleId.Assistant }.hidden)
        assertTrue(normalized.all { it.x >= 0 && it.y >= 0 })
    }

    @Test
    fun corruptModuleSizeFallsBackToTheNearestSupportedSize() {
        val corrupt = HomeLayoutDefaults.layouts.map {
            if (it.id == HomeModuleId.Totals) it.copy(width = 99, height = -7) else it
        }
        val normalized = normalizeHomeLayout(corrupt)
        val totals = normalized.first { it.id == HomeModuleId.Totals }
        assertTrue(totals.width to totals.height in HomeLayoutDefaults.allowedSizes(HomeModuleId.Totals))
        assertTrue(totals.x + totals.width <= HomeLayoutDefaults.columns)
        assertTrue(totals.y + totals.height <= HomeLayoutDefaults.rows)
    }
}
