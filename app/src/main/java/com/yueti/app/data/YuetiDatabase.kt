package com.yueti.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "exam_sessions")
data class ExamSessionEntity(
    @PrimaryKey val id: String,
    val bank: String,
    val questionCount: Int,
    val startedAt: Long,
    val completedAt: Long,
    val elapsedSeconds: Long,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
)

@Entity(tableName = "exam_answers", primaryKeys = ["sessionId", "questionId"])
data class ExamAnswerEntity(
    val sessionId: String,
    val questionId: String,
    val prompt: String,
    val selectedAnswer: Int?,
    val correctAnswer: Int,
    val isCorrect: Boolean,
)

@Entity(tableName = "wrong_records")
data class WrongRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val questionId: String,
    val prompt: String,
    val selectedAnswer: Int?,
    val correctAnswer: Int,
    val explanation: String,
    val wrongAt: Long,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null,
)

@Entity(tableName = "scan_documents")
data class ScanDocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "scan_pages")
data class ScanPageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val filePath: String,
    val pageIndex: Int,
    val filter: String = "bw",
    val rotation: Int = 0,
    val originalPath: String = "",
    val processedPath: String = "",
    val cornersJson: String = "",
    val filterParamsJson: String = "{}",
    val pixelWidth: Int = 0,
    val pixelHeight: Int = 0,
    val processingState: String = "ready",
)

@Entity(tableName = "graph_history")
data class GraphHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val formula: String,
    val createdAt: Long,
)

@Entity(tableName = "graph_documents")
data class GraphDocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val viewportJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "graph_expressions")
data class GraphExpressionEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val kind: String,
    val formula: String,
    val rangeStart: String = "",
    val rangeEnd: String = "",
    val color: Long,
    val visible: Boolean = true,
    val connectPoints: Boolean = true,
    val position: Int = 0,
)

@Entity(tableName = "chat_threads")
data class ChatThreadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mode: String,
    val summary: String = "",
    val summarizedThrough: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val role: String,
    val content: String,
    val reasoningContent: String = "",
    val emotionId: String = "02",
    val status: String = "complete",
    val createdAt: Long,
)

@Dao
interface YuetiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ExamSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<ExamAnswerEntity>)

    @Insert
    suspend fun insertWrongRecords(records: List<WrongRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreWrongRecord(record: WrongRecordEntity)

    @Query("SELECT * FROM exam_sessions ORDER BY completedAt DESC")
    fun observeSessions(): Flow<List<ExamSessionEntity>>

    @Query("SELECT * FROM wrong_records ORDER BY isPinned DESC, pinnedAt DESC, wrongAt DESC, id DESC")
    fun observeWrongRecords(): Flow<List<WrongRecordEntity>>

    @Query(
        """
        UPDATE wrong_records
        SET pinnedAt = CASE WHEN isPinned = 1 THEN NULL ELSE :pinnedAt END,
            isPinned = CASE WHEN isPinned = 1 THEN 0 ELSE 1 END
        WHERE id = :id
        """,
    )
    suspend fun togglePinned(id: Long, pinnedAt: Long)

    @Query("DELETE FROM wrong_records WHERE id = :id")
    suspend fun deleteWrong(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveScanDocument(document: ScanDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveScanPage(page: ScanPageEntity)

    @Query("SELECT * FROM scan_documents ORDER BY updatedAt DESC")
    fun observeScanDocuments(): Flow<List<ScanDocumentEntity>>

    @Insert
    suspend fun insertGraphHistory(item: GraphHistoryEntity)

    @Query("SELECT * FROM graph_history ORDER BY createdAt DESC LIMIT 20")
    fun observeGraphHistory(): Flow<List<GraphHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGraphDocument(item: GraphDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGraphExpressions(items: List<GraphExpressionEntity>)

    @Query("SELECT * FROM graph_expressions WHERE documentId = :documentId ORDER BY position")
    fun observeGraphExpressions(documentId: String): Flow<List<GraphExpressionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChatThread(item: ChatThreadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChatMessage(item: ChatMessageEntity)

    @Query("SELECT * FROM chat_threads ORDER BY updatedAt DESC")
    fun observeChatThreads(): Flow<List<ChatThreadEntity>>

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY createdAt, id")
    fun observeChatMessages(threadId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY createdAt, id")
    suspend fun getChatMessages(threadId: String): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE threadId = :threadId")
    suspend fun deleteChatMessages(threadId: String)

    @Query("DELETE FROM chat_threads WHERE id = :threadId")
    suspend fun deleteChatThreadRow(threadId: String)

    @Query("UPDATE chat_threads SET summary = :summary, summarizedThrough = :through, updatedAt = :updatedAt WHERE id = :threadId")
    suspend fun updateChatSummary(threadId: String, summary: String, through: Long, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusicQueue(items: List<MusicQueueEntity>)

    @Query("DELETE FROM music_queue")
    suspend fun clearMusicQueue()

    @Query("SELECT * FROM music_queue ORDER BY position")
    suspend fun getMusicQueue(): List<MusicQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMusicPlayback(item: MusicPlaybackEntity)

    @Query("SELECT * FROM music_playback WHERE singletonKey = 0 LIMIT 1")
    suspend fun getMusicPlayback(): MusicPlaybackEntity?

    @Transaction
    suspend fun replaceMusicQueue(items: List<MusicQueueEntity>, playback: MusicPlaybackEntity) {
        clearMusicQueue()
        if (items.isNotEmpty()) insertMusicQueue(items)
        saveMusicPlayback(playback)
    }

    @Transaction
    suspend fun deleteChatThread(threadId: String) {
        deleteChatMessages(threadId)
        deleteChatThreadRow(threadId)
    }

    @Transaction
    suspend fun saveCompletedExam(
        session: ExamSessionEntity,
        answers: List<ExamAnswerEntity>,
        wrongRecords: List<WrongRecordEntity>,
    ) {
        insertSession(session)
        insertAnswers(answers)
        if (wrongRecords.isNotEmpty()) insertWrongRecords(wrongRecords)
    }
}

@Database(
    entities = [
        ExamSessionEntity::class,
        ExamAnswerEntity::class,
        WrongRecordEntity::class,
        ScanDocumentEntity::class,
        ScanPageEntity::class,
        GraphHistoryEntity::class,
        GraphDocumentEntity::class,
        GraphExpressionEntity::class,
        ChatThreadEntity::class,
        ChatMessageEntity::class,
        VocabularyWordEntity::class,
        VocabularyProgressEntity::class,
        VocabularyExampleEntity::class,
        VocabularyImageEntity::class,
        VocabularySessionEntity::class,
        MusicQueueEntity::class,
        MusicPlaybackEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class YuetiDatabase : RoomDatabase() {
    abstract fun dao(): YuetiDao
    abstract fun vocabularyDao(): VocabularyDao

    companion object {
        @Volatile private var instance: YuetiDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wrong_records ADD COLUMN pinnedAt INTEGER DEFAULT NULL")
                db.execSQL("UPDATE wrong_records SET pinnedAt = wrongAt WHERE isPinned = 1")
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM wrong_records WHERE selectedAnswer IS NULL")
            }
        }

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS scan_documents (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS scan_pages (id TEXT NOT NULL PRIMARY KEY, documentId TEXT NOT NULL, filePath TEXT NOT NULL, pageIndex INTEGER NOT NULL, filter TEXT NOT NULL, rotation INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS graph_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, formula TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        private val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scan_pages ADD COLUMN originalPath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scan_pages ADD COLUMN processedPath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scan_pages ADD COLUMN cornersJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scan_pages ADD COLUMN filterParamsJson TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE scan_pages ADD COLUMN pixelWidth INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scan_pages ADD COLUMN pixelHeight INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scan_pages ADD COLUMN processingState TEXT NOT NULL DEFAULT 'ready'")
                db.execSQL("CREATE TABLE IF NOT EXISTS graph_documents (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, viewportJson TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS graph_expressions (id TEXT NOT NULL PRIMARY KEY, documentId TEXT NOT NULL, kind TEXT NOT NULL, formula TEXT NOT NULL, rangeStart TEXT NOT NULL, rangeEnd TEXT NOT NULL, color INTEGER NOT NULL, visible INTEGER NOT NULL, connectPoints INTEGER NOT NULL, position INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_threads (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, mode TEXT NOT NULL, summary TEXT NOT NULL, summarizedThrough INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_messages (id TEXT NOT NULL PRIMARY KEY, threadId TEXT NOT NULL, role TEXT NOT NULL, content TEXT NOT NULL, reasoningContent TEXT NOT NULL, emotionId TEXT NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        private val migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS vocabulary_words (wordKey TEXT NOT NULL PRIMARY KEY, word TEXT NOT NULL, phonetic TEXT NOT NULL, definition TEXT NOT NULL, translation TEXT NOT NULL, partOfSpeech TEXT NOT NULL, exchange TEXT NOT NULL, collins INTEGER NOT NULL, bnc INTEGER NOT NULL, frequency INTEGER NOT NULL, sourceRevision TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS vocabulary_progress (wordKey TEXT NOT NULL PRIMARY KEY, box INTEGER NOT NULL, nextReviewAt INTEGER NOT NULL, lastReviewedAt INTEGER NOT NULL, reviewCount INTEGER NOT NULL, lapseCount INTEGER NOT NULL, isStarred INTEGER NOT NULL, isMastered INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS vocabulary_examples (wordKey TEXT NOT NULL, promptHash TEXT NOT NULL, example TEXT NOT NULL, translation TEXT NOT NULL, usageNote TEXT NOT NULL, model TEXT NOT NULL, generatedAt INTEGER NOT NULL, PRIMARY KEY(wordKey, promptHash))")
                db.execSQL("CREATE TABLE IF NOT EXISTS vocabulary_images (wordKey TEXT NOT NULL PRIMARY KEY, fileTitle TEXT NOT NULL, thumbnailUrl TEXT NOT NULL, sourcePageUrl TEXT NOT NULL, artist TEXT NOT NULL, licenseName TEXT NOT NULL, licenseUrl TEXT NOT NULL, searchQuery TEXT NOT NULL, selectedIndex INTEGER NOT NULL, fetchedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS vocabulary_sessions (id TEXT NOT NULL PRIMARY KEY, startedAt INTEGER NOT NULL, completedAt INTEGER NOT NULL, reviewedCount INTEGER NOT NULL, rememberedCount INTEGER NOT NULL, fuzzyCount INTEGER NOT NULL, forgottenCount INTEGER NOT NULL, masteredCount INTEGER NOT NULL)")
            }
        }

        private val migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vocabulary_images ADD COLUMN provider TEXT NOT NULL DEFAULT 'COMMONS'")
                db.execSQL("ALTER TABLE vocabulary_images ADD COLUMN originalUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vocabulary_images ADD COLUMN sourceDomain TEXT NOT NULL DEFAULT 'commons.wikimedia.org'")
                db.execSQL("ALTER TABLE vocabulary_images ADD COLUMN localCachePath TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS music_queue (position INTEGER NOT NULL PRIMARY KEY, trackId TEXT NOT NULL, title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL, artworkUrl TEXT NOT NULL, playlistId TEXT NOT NULL, playlistName TEXT NOT NULL, durationMs INTEGER NOT NULL, playable INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS music_playback (singletonKey INTEGER NOT NULL PRIMARY KEY, currentIndex INTEGER NOT NULL, positionMs INTEGER NOT NULL, repeatMode INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            }
        }

        fun get(context: Context): YuetiDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                YuetiDatabase::class.java,
                "yueti.db",
            ).addMigrations(migration1To2, migration2To3, migration3To4, migration4To5, migration5To6, migration6To7).build().also { instance = it }
        }
    }
}
