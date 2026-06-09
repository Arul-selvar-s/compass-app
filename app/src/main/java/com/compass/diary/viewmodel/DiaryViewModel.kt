package com.compass.diary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compass.diary.data.local.entity.*
import com.compass.diary.data.repository.DiaryRepository
import com.compass.diary.util.AutoSaveManager
import com.compass.diary.util.SaveState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repo: DiaryRepository,
    private val autoSave: AutoSaveManager
) : ViewModel() {

    // ── Today's date key (yyyy-MM-dd) ────────────────────────────
    val todayKey: StateFlow<String> = MutableStateFlow(
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    )

    // ── All entries ───────────────────────────────────────────────
    val allEntries: StateFlow<List<DiaryEntryEntity>> = repo.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWordCount: StateFlow<Int> = allEntries.map { list ->
        list.sumOf { it.wordCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streakDays: Int get() {
        val keys = allEntries.value.map { it.dateKey }.toSet()
        var streak = 0
        var day = LocalDate.now()
        while (keys.contains(day.format(DateTimeFormatter.ISO_LOCAL_DATE))) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    // ── Currently selected / open entry ─────────────────────────
    private val _selectedDateKey = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentEntry: StateFlow<DiaryEntryEntity?> = _selectedDateKey
        .filterNotNull()
        .flatMapLatest { key -> repo.getEntryByDate(key) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentWordCount: StateFlow<Int> = currentEntry.map { it?.wordCount ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val saveState: StateFlow<SaveState> = autoSave.saveState

    // ── All dates that have entries (for calendar highlighting) ──
    val allDateKeys: StateFlow<List<String>> = repo.getAllDateKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Starred items ─────────────────────────────────────────────
    val allStarred: StateFlow<List<StarredItemEntity>> = repo.getAllStarred()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Reminders ─────────────────────────────────────────────────
    val upcomingReminders: StateFlow<List<ReminderEntity>> = repo.getUpcomingReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val completedReminders: StateFlow<List<ReminderEntity>> = repo.getCompletedReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─────────────────────────────────────────────────────────────
    // DIARY OPERATIONS
    // ─────────────────────────────────────────────────────────────

    fun selectEntry(dateKey: String) {
        _selectedDateKey.value = dateKey
        viewModelScope.launch { repo.getOrCreateTodayEntry() }
    }

    fun onContentChanged(dateKey: String, plainText: String) {
        autoSave.onContentChanged {
            repo.saveContent(dateKey, plainText, plainText)
        }
    }

    fun forceSave(dateKey: String, plainText: String) {
        viewModelScope.launch {
            autoSave.forceSave { repo.saveContent(dateKey, plainText, plainText) }
        }
    }

    fun getVersionHistory(dateKey: String) = repo.getVersionHistory(dateKey)

    fun restoreVersion(dateKey: String, version: VersionHistoryEntity) {
        viewModelScope.launch { repo.restoreVersion(dateKey, version) }
    }

    // ─────────────────────────────────────────────────────────────
    // STARRED
    // ─────────────────────────────────────────────────────────────

    fun starContent(dateKey: String, text: String) {
        viewModelScope.launch {
            repo.addStarred(
                StarredItemEntity(
                    diaryDateKey = dateKey,
                    contentType  = "TEXT",
                    contentJson  = text,
                    preview      = text.take(80)
                )
            )
        }
    }

    fun removeStarred(id: Long) {
        viewModelScope.launch { repo.removeStarred(id) }
    }

    // ─────────────────────────────────────────────────────────────
    // DRAWINGS
    // ─────────────────────────────────────────────────────────────

    fun saveDrawing(dateKey: String, pathsJson: String) {
        viewModelScope.launch {
            repo.saveDrawing(
                DrawingEntity(diaryDateKey = dateKey, pathsJson = pathsJson, width = 1080, height = 1920)
            )
        }
    }

    // ─────────────────────────────────────────────────────────────
    // VOICE NOTES
    // ─────────────────────────────────────────────────────────────

    fun saveVoiceNote(dateKey: String, filePath: String, durationMs: Long) {
        viewModelScope.launch {
            repo.saveVoiceNote(
                VoiceNoteEntity(diaryDateKey = dateKey, filePath = filePath, durationMs = durationMs)
            )
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<DiaryEntryEntity>>(emptyList())
    val searchResults: StateFlow<List<DiaryEntryEntity>> = _searchResults

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _searchResults.value = repo.searchEntries(query)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // REMINDERS
    // ─────────────────────────────────────────────────────────────

    fun addReminder(reminder: ReminderEntity) {
        viewModelScope.launch { repo.upsertReminder(reminder) }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch { repo.deleteReminder(reminder) }
    }

    fun markReminderComplete(id: Long) {
        viewModelScope.launch { repo.markReminderCompleted(id) }
    }

    // ─────────────────────────────────────────────────────────────
    // ENTRIES IN RANGE (for AI context)
    // ─────────────────────────────────────────────────────────────

    suspend fun getEntriesInRange(from: String, to: String): List<DiaryEntryEntity> =
        repo.getEntriesInRange(from, to)

    suspend fun getAllEntriesForAI(): String {
        return allEntries.value
            .filter { it.plainText.isNotBlank() }
            .joinToString("\n\n---\n\n") { entry ->
                "Date: ${entry.title}\n${entry.plainText}"
            }
    }

    override fun onCleared() {
        super.onCleared()
        autoSave.dispose()
    }
}
