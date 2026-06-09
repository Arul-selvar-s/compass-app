package com.compass.diary.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.compass.diary.data.local.entity.DiaryEntryEntity.Companion.TABLE_NAME

// ─────────────────────────────────────────────────────────────────
// DIARY ENTRY — one record per calendar day
// ─────────────────────────────────────────────────────────────────
@Entity(tableName = TABLE_NAME)
data class DiaryEntryEntity(
    @PrimaryKey val dateKey: String,          // "2026-06-07"
    val title: String,                         // "Sunday, June 7, 2026"
    val contentJson: String = "",              // Rich text serialised as JSON
    val plainText: String = "",                // Stripped text for search / AI
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isShared: Boolean = false,
    val sharePermission: String = "NONE",      // NONE|READ|ADD|READWRITE|FULL
    val shareCode: String? = null,
    val cloudId: String? = null,               // Google Drive file ID
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val mood: String? = null,                  // emoji or label
    val weather: String? = null,
    val location: String? = null,
    val tags: String = ""                      // comma-separated tags
) {
    companion object { const val TABLE_NAME = "diary_entries" }
}

// ─────────────────────────────────────────────────────────────────
// STARRED ITEM — any piece of content the user stars
// ─────────────────────────────────────────────────────────────────
@Entity(tableName = "starred_items")
data class StarredItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diaryDateKey: String,                  // FK to DiaryEntryEntity
    val contentType: String,                   // TEXT|IMAGE|DRAWING|VOICE|CHECKLIST
    val contentJson: String,                   // Serialised content
    val preview: String,                       // Short human-readable preview
    val starredAt: Long = System.currentTimeMillis(),
    val cloudId: String? = null,
    val syncedAt: Long? = null
)

// ─────────────────────────────────────────────────────────────────
// REMINDER
// ─────────────────────────────────────────────────────────────────
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val repeatType: String = "ONCE",          // ONCE|DAILY|WEEKLY|MONTHLY
    val triggerAt: Long,                       // epoch millis
    val isCompleted: Boolean = false,
    val isNotificationEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val diaryDateKey: String? = null           // Optional link to diary page
)

// ─────────────────────────────────────────────────────────────────
// VERSION HISTORY — one record per save of a diary page
// ─────────────────────────────────────────────────────────────────
@Entity(tableName = "version_history")
data class VersionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diaryDateKey: String,
    val contentJson: String,
    val plainText: String,
    val wordCount: Int,
    val savedAt: Long = System.currentTimeMillis(),
    val changeDescription: String = ""         // e.g. "Auto-saved", "Manual save"
)

// ─────────────────────────────────────────────────────────────────
// SHARED PAGE — collaborative share record
// ─────────────────────────────────────────────────────────────────
@Entity(tableName = "shared_pages")
data class SharedPageEntity(
    @PrimaryKey val shareCode: String,
    val diaryDateKey: String,
    val permission: String,                    // READ|ADD|READWRITE|FULL
    val sharedWithEmail: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isActive: Boolean = true
)

// ─────────────────────────────────────────────────────────────────
// DRAWING ASSET — stored separately from rich text JSON
// ─────────────────────────────────────────────────────────────────
@Entity(tableName = "drawings")
data class DrawingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diaryDateKey: String,
    val pathsJson: String,                     // Serialised list of DrawPath
    val width: Int,
    val height: Int,
    @androidx.room.Ignore val thumbnail: ByteArray? = null
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────────
// VOICE NOTE
// ─────────────────────────────────────────────────────────────────
@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diaryDateKey: String,
    val filePath: String,
    val durationMs: Long,
    val transcript: String = "",               // Auto-transcribed text for search
    val createdAt: Long = System.currentTimeMillis(),
    val cloudFileId: String? = null
)
