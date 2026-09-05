package com.yueti.app.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.homeLayoutDataStore by preferencesDataStore(name = "home_layout_v1")

enum class HomeModuleId { Profile, Totals, DailyGoal, Assistant, Practice, Translator, BeijingClock, Pk, EnglishSpeaking, Vocabulary }

data class HomeModuleLayout(
    val id: HomeModuleId,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val hidden: Boolean = false,
)

object HomeLayoutDefaults {
    const val columns = 4
    const val rows = 10

    val layouts = listOf(
        HomeModuleLayout(HomeModuleId.Profile, 0, 0, 4, 1),
        HomeModuleLayout(HomeModuleId.Totals, 0, 1, 2, 1),
        HomeModuleLayout(HomeModuleId.DailyGoal, 2, 1, 2, 1),
        HomeModuleLayout(HomeModuleId.Assistant, 0, 2, 4, 3),
        HomeModuleLayout(HomeModuleId.Practice, 0, 5, 4, 2),
        HomeModuleLayout(HomeModuleId.Translator, 0, 7, 2, 1),
        HomeModuleLayout(HomeModuleId.BeijingClock, 2, 7, 2, 1),
        HomeModuleLayout(HomeModuleId.Pk, 0, 8, 2, 1),
        HomeModuleLayout(HomeModuleId.EnglishSpeaking, 2, 8, 2, 1),
        HomeModuleLayout(HomeModuleId.Vocabulary, 0, 9, 4, 1),
    )

    fun allowedSizes(id: HomeModuleId): List<Pair<Int, Int>> = when (id) {
        HomeModuleId.Profile -> listOf(2 to 1, 4 to 1)
        HomeModuleId.Totals -> listOf(1 to 1, 2 to 1)
        HomeModuleId.DailyGoal -> listOf(1 to 1, 2 to 1, 2 to 2)
        HomeModuleId.Assistant -> listOf(2 to 2, 4 to 2, 4 to 3)
        HomeModuleId.Practice -> listOf(2 to 1, 4 to 1, 4 to 2)
        HomeModuleId.Translator -> listOf(2 to 1, 4 to 1)
        HomeModuleId.BeijingClock -> listOf(1 to 1, 2 to 1)
        HomeModuleId.Pk -> listOf(2 to 1, 4 to 1)
        HomeModuleId.EnglishSpeaking -> listOf(2 to 1, 4 to 1)
        HomeModuleId.Vocabulary -> listOf(2 to 1, 4 to 1, 4 to 2)
    }

    fun isLocked(id: HomeModuleId) = id == HomeModuleId.Assistant || id == HomeModuleId.Practice
}

class HomeLayoutStore(private val context: Context) {
    private val key = stringPreferencesKey("layout_json")

    val layout: Flow<List<HomeModuleLayout>> = context.homeLayoutDataStore.data.map { preferences ->
        (decode(preferences[key]).let(::normalizeHomeLayout).takeIf(::isValid) ?: HomeLayoutDefaults.layouts).also {
            cachedLayout = it
        }
    }

    suspend fun save(layout: List<HomeModuleLayout>) {
        val normalized = normalizeHomeLayout(layout)
        cachedLayout = normalized
        context.homeLayoutDataStore.edit { preferences -> preferences[key] = encode(normalized) }
    }

    suspend fun reset() = save(HomeLayoutDefaults.layouts)

    private fun encode(items: List<HomeModuleLayout>) = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("id", item.id.name); put("x", item.x); put("y", item.y)
                put("w", item.width); put("h", item.height); put("hidden", item.hidden)
            })
        }
    }.toString()

    private fun decode(raw: String?): List<HomeModuleLayout> = runCatching {
        if (raw.isNullOrBlank()) return@runCatching emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val id = HomeModuleId.valueOf(item.getString("id"))
                add(HomeModuleLayout(id, item.getInt("x"), item.getInt("y"), item.getInt("w"), item.getInt("h"), item.optBoolean("hidden")))
            }
            HomeModuleId.entries.filter { id -> none { it.id == id } }.forEach { id ->
                add(HomeLayoutDefaults.layouts.first { it.id == id })
            }
        }
    }.getOrDefault(emptyList())

    private fun isValid(items: List<HomeModuleLayout>) = items.map { it.id }.toSet() == HomeModuleId.entries.toSet() &&
        items.all { it.width > 0 && it.height > 0 && it.x >= 0 && it.y >= 0 && it.x + it.width <= HomeLayoutDefaults.columns && it.y + it.height <= HomeLayoutDefaults.rows } &&
        items.filterNot { it.hidden }.flatMap { item ->
            (item.y until item.y + item.height).flatMap { row -> (item.x until item.x + item.width).map { column -> row to column } }
        }.let { cells -> cells.size == cells.distinct().size }

    companion object {
        @Volatile private var cachedLayout: List<HomeModuleLayout>? = null

        fun cachedOrNull(): List<HomeModuleLayout>? = cachedLayout
    }
}

fun normalizeHomeLayout(source: List<HomeModuleLayout>, priority: HomeModuleId? = null): List<HomeModuleLayout> {
    // A fresh install has no DataStore value, and older/corrupted snapshots can contain only a
    // subset of modules.  Complete the catalogue before placement so normalization is total and
    // can never crash the first frame after onboarding.
    val complete = HomeModuleId.entries.map { id ->
        val raw = source.firstOrNull { it.id == id } ?: HomeLayoutDefaults.layouts.first { it.id == id }
        val safeSize = HomeLayoutDefaults.allowedSizes(id).minBy { (width, height) ->
            kotlin.math.abs(width - raw.width) + kotlin.math.abs(height - raw.height)
        }
        raw.copy(width = safeSize.first, height = safeSize.second)
    }
    val ordered = complete.sortedBy { if (it.id == priority) 0 else 1 }
    val occupied = Array(HomeLayoutDefaults.rows) { BooleanArray(HomeLayoutDefaults.columns) }
    val result = mutableListOf<HomeModuleLayout>()
    fun fits(item: HomeModuleLayout, x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x + item.width > HomeLayoutDefaults.columns || y + item.height > HomeLayoutDefaults.rows) return false
        return (y until y + item.height).all { row -> (x until x + item.width).all { column -> !occupied[row][column] } }
    }
    ordered.forEach { original ->
        if (original.hidden) { result += original; return@forEach }
        val clamped = original.copy(x = original.x.coerceIn(0, HomeLayoutDefaults.columns - original.width), y = original.y.coerceIn(0, HomeLayoutDefaults.rows - original.height))
        val position = if (fits(clamped, clamped.x, clamped.y)) clamped.x to clamped.y else {
            (0 until HomeLayoutDefaults.rows).firstNotNullOfOrNull { y ->
                (0 until HomeLayoutDefaults.columns).firstNotNullOfOrNull { x -> if (fits(clamped, x, y)) x to y else null }
            }
        }
        val placed = position?.let { (x, y) -> clamped.copy(x = x, y = y) } ?: clamped.copy(hidden = !HomeLayoutDefaults.isLocked(clamped.id))
        if (!placed.hidden) for (row in placed.y until placed.y + placed.height) for (column in placed.x until placed.x + placed.width) occupied[row][column] = true
        result += placed
    }
    return HomeModuleId.entries.map { id -> result.first { it.id == id } }
}
