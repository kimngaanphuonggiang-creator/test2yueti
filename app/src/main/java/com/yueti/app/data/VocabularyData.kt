package com.yueti.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlin.math.max
import kotlin.math.min

const val ECDICT_REVISION = "bc015ed2e24a7abef49fc6dbbb7fe32c1dadaf8b"

@Entity(tableName = "vocabulary_words")
data class VocabularyWordEntity(
    @PrimaryKey val wordKey: String,
    val word: String,
    val phonetic: String,
    val definition: String,
    val translation: String,
    val partOfSpeech: String,
    val exchange: String,
    val collins: Int,
    val bnc: Int,
    val frequency: Int,
    val sourceRevision: String = ECDICT_REVISION,
)

@Entity(tableName = "vocabulary_progress")
data class VocabularyProgressEntity(
    @PrimaryKey val wordKey: String,
    val box: Int = 0,
    val nextReviewAt: Long = 0L,
    val lastReviewedAt: Long = 0L,
    val reviewCount: Int = 0,
    val lapseCount: Int = 0,
    val isStarred: Boolean = false,
    val isMastered: Boolean = false,
)

@Entity(tableName = "vocabulary_examples", primaryKeys = ["wordKey", "promptHash"])
data class VocabularyExampleEntity(
    val wordKey: String,
    val promptHash: String,
    val example: String,
    val translation: String,
    val usageNote: String,
    val model: String,
    val generatedAt: Long,
)

@Entity(tableName = "vocabulary_images")
data class VocabularyImageEntity(
    @PrimaryKey val wordKey: String,
    val fileTitle: String,
    val thumbnailUrl: String,
    val sourcePageUrl: String,
    val artist: String,
    val licenseName: String,
    val licenseUrl: String,
    val searchQuery: String,
    val selectedIndex: Int,
    val fetchedAt: Long,
    val provider: String = "COMMONS",
    val originalUrl: String = "",
    val sourceDomain: String = "commons.wikimedia.org",
    val localCachePath: String = "",
)

@Entity(tableName = "vocabulary_sessions")
data class VocabularySessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val completedAt: Long,
    val reviewedCount: Int,
    val rememberedCount: Int,
    val fuzzyCount: Int,
    val forgottenCount: Int,
    val masteredCount: Int,
)

data class VocabularyCounts(
    val totalCount: Int,
    val newCount: Int,
    val dueCount: Int,
    val learningCount: Int,
    val masteredCount: Int,
    val starredCount: Int,
)

enum class VocabularyGroup(val label: String) {
    Today("今日卡组"),
    Due("待复习"),
    New("新词"),
    Learning("学习中"),
    Mastered("已熟记"),
    Starred("收藏"),
    All("全部词汇"),
}

enum class MemoryRating { Forgot, Fuzzy, Remembered, Mastered }

private const val MinuteMillis = 60_000L
private const val DayMillis = 24L * 60L * MinuteMillis
private val BoxIntervals = longArrayOf(0L, 1L, 3L, 7L, 14L, 30L)

internal fun nextVocabularyProgress(
    current: VocabularyProgressEntity?,
    wordKey: String,
    rating: MemoryRating,
    now: Long,
): VocabularyProgressEntity {
    val previous = current ?: VocabularyProgressEntity(wordKey = wordKey)
    return when (rating) {
        MemoryRating.Forgot -> previous.copy(
            box = 1,
            nextReviewAt = now + 10L * MinuteMillis,
            lastReviewedAt = now,
            reviewCount = previous.reviewCount + 1,
            lapseCount = previous.lapseCount + 1,
            isMastered = false,
        )
        MemoryRating.Fuzzy -> previous.copy(
            box = max(1, previous.box - 1),
            nextReviewAt = now + DayMillis,
            lastReviewedAt = now,
            reviewCount = previous.reviewCount + 1,
            isMastered = false,
        )
        MemoryRating.Remembered -> {
            val nextBox = min(5, max(1, previous.box + 1))
            previous.copy(
                box = nextBox,
                nextReviewAt = now + BoxIntervals[nextBox] * DayMillis,
                lastReviewedAt = now,
                reviewCount = previous.reviewCount + 1,
                isMastered = nextBox == 5 && previous.isMastered,
            )
        }
        MemoryRating.Mastered -> previous.copy(
            box = 5,
            nextReviewAt = now + BoxIntervals[5] * DayMillis,
            lastReviewedAt = now,
            reviewCount = previous.reviewCount + 1,
            isMastered = true,
        )
    }
}

internal fun mergeVocabularyQueue(
    due: List<VocabularyWordEntity>,
    fresh: List<VocabularyWordEntity>,
    groupSize: Int,
): List<VocabularyWordEntity> {
    val limit = groupSize.coerceIn(5, 50)
    return (due + fresh).distinctBy(VocabularyWordEntity::wordKey).take(limit)
}

@Dao
interface VocabularyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWords(words: List<VocabularyWordEntity>)

    @Query("SELECT COUNT(*) FROM vocabulary_words")
    suspend fun countWords(): Int

    @Query("SELECT * FROM vocabulary_words WHERE wordKey = :wordKey LIMIT 1")
    suspend fun getWord(wordKey: String): VocabularyWordEntity?

    @Query(
        """
        SELECT w.* FROM vocabulary_words w
        JOIN vocabulary_progress p ON p.wordKey = w.wordKey
        WHERE p.nextReviewAt <= :now AND (p.isMastered = 0 OR :includeMastered = 1)
        ORDER BY p.nextReviewAt ASC, p.lapseCount DESC, w.frequency ASC, w.wordKey ASC
        LIMIT :limit
        """,
    )
    suspend fun getDueWords(now: Long, includeMastered: Boolean, limit: Int): List<VocabularyWordEntity>

    @Query(
        """
        SELECT w.* FROM vocabulary_words w
        LEFT JOIN vocabulary_progress p ON p.wordKey = w.wordKey
        WHERE p.wordKey IS NULL
        ORDER BY CASE WHEN w.collins > 0 THEN 0 ELSE 1 END, w.collins DESC,
                 CASE WHEN w.frequency > 0 THEN w.frequency ELSE 1000000 END,
                 CASE WHEN w.bnc > 0 THEN w.bnc ELSE 1000000 END, w.wordKey ASC
        LIMIT :limit
        """,
    )
    suspend fun getNewWords(limit: Int): List<VocabularyWordEntity>

    @Query(
        """
        SELECT w.* FROM vocabulary_words w
        JOIN vocabulary_progress p ON p.wordKey = w.wordKey
        WHERE p.isMastered = 0 AND p.box BETWEEN 1 AND 5
        ORDER BY p.nextReviewAt ASC, w.frequency ASC
        LIMIT :limit
        """,
    )
    suspend fun getLearningWords(limit: Int): List<VocabularyWordEntity>

    @Query(
        """
        SELECT w.* FROM vocabulary_words w
        JOIN vocabulary_progress p ON p.wordKey = w.wordKey
        WHERE p.isMastered = 1
        ORDER BY p.lastReviewedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getMasteredWords(limit: Int): List<VocabularyWordEntity>

    @Query(
        """
        SELECT w.* FROM vocabulary_words w
        JOIN vocabulary_progress p ON p.wordKey = w.wordKey
        WHERE p.isStarred = 1
        ORDER BY p.lastReviewedAt DESC, w.wordKey ASC
        LIMIT :limit
        """,
    )
    suspend fun getStarredWords(limit: Int): List<VocabularyWordEntity>

    @Query(
        """
        SELECT * FROM vocabulary_words
        ORDER BY CASE WHEN collins > 0 THEN 0 ELSE 1 END, collins DESC,
                 CASE WHEN frequency > 0 THEN frequency ELSE 1000000 END, wordKey ASC
        LIMIT :limit
        """,
    )
    suspend fun getAllWords(limit: Int): List<VocabularyWordEntity>

    @Query(
        """
        SELECT * FROM vocabulary_words
        WHERE word LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%'
        ORDER BY CASE WHEN wordKey = :query THEN 0 ELSE 1 END,
                 CASE WHEN frequency > 0 THEN frequency ELSE 1000000 END
        LIMIT :limit
        """,
    )
    suspend fun searchWords(query: String, limit: Int): List<VocabularyWordEntity>

    @Query("SELECT * FROM vocabulary_progress WHERE wordKey = :wordKey LIMIT 1")
    suspend fun getProgress(wordKey: String): VocabularyProgressEntity?

    @Query("SELECT * FROM vocabulary_progress WHERE wordKey IN (:wordKeys)")
    suspend fun getProgress(wordKeys: List<String>): List<VocabularyProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: VocabularyProgressEntity)

    @Query(
        """
        SELECT
          (SELECT COUNT(*) FROM vocabulary_words) AS totalCount,
          (SELECT COUNT(*) FROM vocabulary_words w LEFT JOIN vocabulary_progress p ON p.wordKey = w.wordKey WHERE p.wordKey IS NULL) AS newCount,
          (SELECT COUNT(*) FROM vocabulary_progress WHERE nextReviewAt <= :now AND isMastered = 0) AS dueCount,
          (SELECT COUNT(*) FROM vocabulary_progress WHERE box BETWEEN 1 AND 5 AND isMastered = 0) AS learningCount,
          (SELECT COUNT(*) FROM vocabulary_progress WHERE isMastered = 1) AS masteredCount,
          (SELECT COUNT(*) FROM vocabulary_progress WHERE isStarred = 1) AS starredCount
        """,
    )
    suspend fun getCounts(now: Long): VocabularyCounts

    @Query("SELECT * FROM vocabulary_examples WHERE wordKey = :wordKey AND promptHash = :promptHash LIMIT 1")
    suspend fun getExample(wordKey: String, promptHash: String): VocabularyExampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExample(example: VocabularyExampleEntity)

    @Query("SELECT * FROM vocabulary_images WHERE wordKey = :wordKey LIMIT 1")
    suspend fun getImage(wordKey: String): VocabularyImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImage(image: VocabularyImageEntity)

    @Query("DELETE FROM vocabulary_images WHERE wordKey = :wordKey")
    suspend fun deleteImage(wordKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: VocabularySessionEntity)
}
