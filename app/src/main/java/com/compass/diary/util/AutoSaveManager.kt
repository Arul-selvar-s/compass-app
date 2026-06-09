package com.compass.diary.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class SaveState { IDLE, SAVING, SAVED, ERROR }

/**
 * Debounced auto-save manager.
 * Call [onContentChanged] after every keystroke; it waits [debounceMs] before
 * actually persisting to avoid hammering the database.
 */
@Singleton
class AutoSaveManager @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _saveState = MutableStateFlow(SaveState.IDLE)
    val saveState: StateFlow<SaveState> = _saveState

    private var debounceJob: Job? = null

    companion object {
        const val DEBOUNCE_MS = 800L     // wait 800 ms after last change
        const val FORCE_SAVE_MS = 5000L  // force save every 5 s regardless
    }

    /**
     * Call this whenever content changes.
     * [saveBlock] is the suspend lambda that actually writes to the DB.
     */
    fun onContentChanged(saveBlock: suspend () -> Unit) {
        debounceJob?.cancel()
        _saveState.value = SaveState.SAVING

        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            try {
                saveBlock()
                _saveState.value = SaveState.SAVED
                delay(2000L)
                _saveState.value = SaveState.IDLE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _saveState.value = SaveState.ERROR
            }
        }
    }

    /**
     * Force an immediate save (e.g. when user presses back or app goes to background).
     */
    suspend fun forceSave(saveBlock: suspend () -> Unit) {
        debounceJob?.cancel()
        try {
            _saveState.value = SaveState.SAVING
            withContext(Dispatchers.IO) { saveBlock() }
            _saveState.value = SaveState.SAVED
        } catch (e: Exception) {
            _saveState.value = SaveState.ERROR
        }
    }

    fun dispose() {
        debounceJob?.cancel()
        scope.cancel()
    }
}
