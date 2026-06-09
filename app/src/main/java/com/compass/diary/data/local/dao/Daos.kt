package com.compass.diary.data.local.dao

import androidx.room.*
import com.compass.diary.data.local.entity.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────
// DIARY DAO
// ─────────────────────────────────────────────────────────────────
@Dao
interface DiaryDao {

    @Query("SELECT * FROM diary_entries WHERE isDeleted = 0 ORDER BY dateKey DESC")
    fun getAllEntries(): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE dateKey = :dateKey AND isDeleted = 0 LIMIT 1")
    fun getEntryByDate(dateKey: String): Flow<DiaryEntryEntity?>

    @Query("SELECT * FROM diary_entries WHERE dateKey = :dateKey AND isDeleted = 0 LIMIT 1")
    suspend fun getEntryByDateOnce(dateKey: String): DiaryEntryEntity?

    @Query("""
        SELECT * FROM diary_entries 
        WHERE (plainText LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        AND isDeleted = 0
        ORDER BY dateKey DESC
    """)
    suspend fun searchEntries(query: String): List<DiaryEntryEntity>

    @Query("SELECT * FROM diary_entries WHERE dateKey BETWEEN :from AND :to AND isDeleted = 0 ORDER BY dateKey ASC")
    suspend fun getEntriesInRange(from: String, to: String): List<DiaryEntryEntity>

    @Query("SELECT dateKey FROM diary_entries WHERE isDeleted = 0")
    fun getAllDateKeys(): Flow<List<String>>

    @Query("SELECT * FROM diary_entries WHERE isDeleted = 0 AND syncedAt IS NULL")
    suspend fun getUnsyncedEntries(): List<DiaryEntryEntity>

    @Upsert
    suspend fun upsertEntry(entry: DiaryEntryEntity)

    @Query("UPDATE diary_entries SET isDeleted = 1, updatedAt = :now WHERE dateKey = :dateKey")
    suspend fun softDelete(dateKey: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE diary_entries SET syncedAt = :time, cloudId = :cloudId WHERE dateKey = :dateKey")
    suspend fun markSynced(dateKey: String, cloudId: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE diary_entries SET contentJson = :json, plainText = :plain, wordCount = :wc, updatedAt = :now WHERE dateKey = :dateKey")
    suspend fun updateContent(dateKey: String, json: String, plain: String, wc: Int, now: Long = System.currentTimeMillis())
}

// ─────────────────────────────────────────────────────────────────
// STARRED DAO
// ─────────────────────────────────────────────────────────────────
@Dao
interface StarredDao {

    @Query("SELECT * FROM starred_items ORDER BY starredAt DESC")
    fun getAllStarred(): Flow<List<StarredItemEntity>>

    @Query("SELECT * FROM starred_items WHERE diaryDateKey = :dateKey")
    fun getStarredForDate(dateKey: String): Flow<List<StarredItemEntity>>

    @Query("SELECT * FROM starred_items WHERE preview LIKE '%' || :query || '%'")
    suspend fun searchStarred(query: String): List<StarredItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStarred(item: StarredItemEntity): Long

    @Delete
    suspend fun deleteStarred(item: StarredItemEntity)

    @Query("DELETE FROM starred_items WHERE id = :id")
    suspend fun deleteStarredById(id: Long)
}

// ─────────────────────────────────────────────────────────────────
// REMINDER DAO
// ─────────────────────────────────────────────────────────────────
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY triggerAt ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND triggerAt > :now ORDER BY triggerAt ASC")
    fun getUpcomingReminders(now: Long = System.currentTimeMillis()): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY triggerAt DESC")
    fun getCompletedReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity): Long

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?
}

// ─────────────────────────────────────────────────────────────────
// VERSION HISTORY DAO
// ─────────────────────────────────────────────────────────────────
@Dao
interface VersionHistoryDao {

    @Query("SELECT * FROM version_history WHERE diaryDateKey = :dateKey ORDER BY savedAt DESC")
    fun getHistoryForDate(dateKey: String): Flow<List<VersionHistoryEntity>>

    @Query("SELECT * FROM version_history WHERE diaryDateKey = :dateKey ORDER BY savedAt DESC LIMIT :limit")
    suspend fun getHistoryForDateLimited(dateKey: String, limit: Int = 50): List<VersionHistoryEntity>

    @Insert
    suspend fun insertVersion(version: VersionHistoryEntity)

    @Query("DELETE FROM version_history WHERE diaryDateKey = :dateKey AND savedAt < :before")
    suspend fun pruneOldVersions(dateKey: String, before: Long)

    @Query("SELECT COUNT(*) FROM version_history WHERE diaryDateKey = :dateKey")
    suspend fun countVersions(dateKey: String): Int
}

// ─────────────────────────────────────────────────────────────────
// DRAWING DAO
// ─────────────────────────────────────────────────────────────────
@Dao
interface DrawingDao {

    @Query("SELECT * FROM drawings WHERE diaryDateKey = :dateKey")
    fun getDrawingsForDate(dateKey: String): Flow<List<DrawingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDrawing(drawing: DrawingEntity): Long

    @Delete
    suspend fun deleteDrawing(drawing: DrawingEntity)

    @Query("SELECT * FROM drawings WHERE id = :id")
    suspend fun getDrawingById(id: Long): DrawingEntity?
}

// ─────────────────────────────────────────────────────────────────
// VOICE NOTE DAO
// ─────────────────────────────────────────────────────────────────
@Dao
interface VoiceNoteDao {

    @Query("SELECT * FROM voice_notes WHERE diaryDateKey = :dateKey ORDER BY createdAt ASC")
    fun getVoiceNotesForDate(dateKey: String): Flow<List<VoiceNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVoiceNote(note: VoiceNoteEntity): Long

    @Delete
    suspend fun deleteVoiceNote(note: VoiceNoteEntity)

    @Query("SELECT * FROM voice_notes WHERE transcript LIKE '%' || :query || '%'")
    suspend fun searchTranscripts(query: String): List<VoiceNoteEntity>
}
