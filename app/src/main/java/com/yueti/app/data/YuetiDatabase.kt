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
)

@Entity(tableName = "graph_history")
data class GraphHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val formula: String,
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
    entities = [ExamSessionEntity::class, ExamAnswerEntity::class, WrongRecordEntity::class, ScanDocumentEntity::class, ScanPageEntity::class, GraphHistoryEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class YuetiDatabase : RoomDatabase() {
    abstract fun dao(): YuetiDao

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

        fun get(context: Context): YuetiDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                YuetiDatabase::class.java,
                "yueti.db",
            ).addMigrations(migration1To2, migration2To3, migration3To4).build().also { instance = it }
        }
    }
}
