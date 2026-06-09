package com.compass.diary.data.repository

import com.compass.diary.data.local.dao.*
import com.compass.diary.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao,
    private val starredDao: StarredDao,
    private val reminderDao: ReminderDao,
    private val versionHistoryDao: VersionHistoryDao,
    private val drawingDao: DrawingDao,
    private val voiceNoteDao: VoiceNoteDao
) {
    // ─── DIARY ENTRIES ────────────────────────────────────────────

    fun getAllEntries(): Flow<List<DiaryEntryEntity>> = diaryDao.getAllEntries()

    fun getEntryByDate(dateKey: String): Flow<DiaryEntryEntity?> =
        diaryDao.getEntryByDate(dateKey)

    fun getAllDateKeys(): Flow<List<String>> = diaryDao.getAllDateKeys()

    /** Returns today's entry, creating it if it doesn't exist. */
    suspend fun getOrCreateTodayEntry(): DiaryEntryEntity {
        val today = LocalDate.now()
        val key = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = diaryDao.getEntryByDateOnce(key)
        if (existing != null) return existing
        val title = buildTitle(today)
        val newEntry = DiaryEntryEntity(dateKey = key, title = title)
        diaryDao.upsertEntry(newEntry)
        return newEntry
    }

    suspend fun saveContent(dateKey: String, contentJson: String, plainText: String) {
        val wordCount = plainText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        diaryDao.updateContent(dateKey, contentJson, plainText, wordCount)
        // Save to version history
        versionHistoryDao.insertVersion(
            VersionHistoryEntity(
                diaryDateKey = dateKey,
                contentJson = contentJson,
                plainText = plainText,
                wordCount = wordCount,
                changeDescription = "Auto-saved"
            )
        )
        pruneVersionHistory(dateKey)
    }

    suspend fun searchEntries(query: String): List<DiaryEntryEntity> =
        diaryDao.searchEntries(query)

    suspend fun getEntriesInRange(from: String, to: String): List<DiaryEntryEntity> =
        diaryDao.getEntriesInRange(from, to)

    suspend fun softDeleteEntry(dateKey: String) = diaryDao.softDelete(dateKey)

    suspend fun getUnsyncedEntries() = diaryDao.getUnsyncedEntries()

    suspend fun markEntrySynced(dateKey: String, cloudId: String) =
        diaryDao.markSynced(dateKey, cloudId)

    // ─── VERSION HISTORY ──────────────────────────────────────────

    fun getVersionHistory(dateKey: String): Flow<List<VersionHistoryEntity>> =
        versionHistoryDao.getHistoryForDate(dateKey)

    suspend fun getVersionHistoryLimited(dateKey: String, limit: Int = 30) =
        versionHistoryDao.getHistoryForDateLimited(dateKey, limit)

    suspend fun restoreVersion(dateKey: String, version: VersionHistoryEntity) {
        diaryDao.updateContent(
            dateKey,
            version.contentJson,
            version.plainText,
            version.wordCount
        )
        versionHistoryDao.insertVersion(
            version.copy(
                id = 0,
                changeDescription = "Restored version from ${version.savedAt}"
            )
        )
    }

    private suspend fun pruneVersionHistory(dateKey: String) {
        val count = versionHistoryDao.countVersions(dateKey)
        if (count > 100) {
            // Keep only the last 100 versions; prune the oldest
            val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // 30 days
            versionHistoryDao.pruneOldVersions(dateKey, cutoff)
        }
    }

    // ─── STARRED ITEMS ────────────────────────────────────────────

    fun getAllStarred(): Flow<List<StarredItemEntity>> = starredDao.getAllStarred()

    fun getStarredForDate(dateKey: String): Flow<List<StarredItemEntity>> =
        starredDao.getStarredForDate(dateKey)

    suspend fun addStarred(item: StarredItemEntity): Long = starredDao.insertStarred(item)

    suspend fun removeStarred(id: Long) = starredDao.deleteStarredById(id)

    suspend fun searchStarred(query: String) = starredDao.searchStarred(query)

    // ─── REMINDERS ────────────────────────────────────────────────

    fun getAllReminders(): Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    fun getUpcomingReminders(): Flow<List<ReminderEntity>> = reminderDao.getUpcomingReminders()

    fun getCompletedReminders(): Flow<List<ReminderEntity>> = reminderDao.getCompletedReminders()

    suspend fun upsertReminder(reminder: ReminderEntity): Long =
        reminderDao.upsertReminder(reminder)

    suspend fun deleteReminder(reminder: ReminderEntity) = reminderDao.deleteReminder(reminder)

    suspend fun markReminderCompleted(id: Long) = reminderDao.markCompleted(id)

    suspend fun getReminderById(id: Long) = reminderDao.getReminderById(id)

    // ─── DRAWINGS ─────────────────────────────────────────────────

    fun getDrawings(dateKey: String): Flow<List<DrawingEntity>> =
        drawingDao.getDrawingsForDate(dateKey)

    suspend fun saveDrawing(drawing: DrawingEntity): Long = drawingDao.upsertDrawing(drawing)

    suspend fun deleteDrawing(drawing: DrawingEntity) = drawingDao.deleteDrawing(drawing)

    // ─── VOICE NOTES ──────────────────────────────────────────────

    fun getVoiceNotes(dateKey: String): Flow<List<VoiceNoteEntity>> =
        voiceNoteDao.getVoiceNotesForDate(dateKey)

    suspend fun saveVoiceNote(note: VoiceNoteEntity): Long = voiceNoteDao.upsertVoiceNote(note)

    suspend fun deleteVoiceNote(note: VoiceNoteEntity) = voiceNoteDao.deleteVoiceNote(note)

    suspend fun searchTranscripts(query: String) = voiceNoteDao.searchTranscripts(query)

    // ─── HELPERS ──────────────────────────────────────────────────

    private fun buildTitle(date: LocalDate): String {
        val dow = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val month = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return "$dow, $month ${date.dayOfMonth}, ${date.year}"
    }
}
