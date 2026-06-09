package com.compass.diary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compass.diary.data.remote.AnthropicApiService
import com.compass.diary.data.repository.DiaryRepository
import com.compass.diary.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AIViewModel @Inject constructor(
    private val api: AnthropicApiService,
    private val prefs: PreferencesManager,
    private val repo: DiaryRepository
) : ViewModel() {

    data class Message(
        val id: String = UUID.randomUUID().toString(),
        val role: String,           // "user" | "assistant"
        val content: String,
        val sourceDates: List<String> = emptyList()
    )

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking

    val isApiKeyConfigured: StateFlow<Boolean> = prefs.anthropicApiKey
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ── All entries cached for AI context ────────────────────────
    private val allEntries = repo.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun ask(question: String) {
        val userMsg = Message(role = "user", content = question)
        _messages.update { it + userMsg }
        _isThinking.value = true

        viewModelScope.launch {
            val apiKey = prefs.anthropicApiKey.first()
            if (apiKey.isNullOrBlank()) {
                _messages.update {
                    it + Message(
                        role    = "assistant",
                        content = "Please add your Anthropic API key in Settings → AI Assistant to use this feature."
                    )
                }
                _isThinking.value = false
                return@launch
            }

            val diaryContext = buildDiaryContext()

            val result = api.askAboutDiary(
                apiKey       = apiKey,
                userQuestion = question,
                diaryContext = diaryContext
            )

            result.fold(
                onSuccess = { response ->
                    val dates = extractMentionedDates(response)
                    _messages.update {
                        it + Message(role = "assistant", content = response, sourceDates = dates)
                    }
                },
                onFailure = { error ->
                    _messages.update {
                        it + Message(
                            role    = "assistant",
                            content = "Sorry, I couldn't reach the AI. ${error.message}"
                        )
                    }
                }
            )
            _isThinking.value = false
        }
    }

    private fun buildDiaryContext(): String {
        val text = allEntries.value
            .filter { it.plainText.isNotBlank() }
            .joinToString("\n\n---\n\n") { "Date: ${it.title}\n${it.plainText}" }
        return if (text.length > 100_000) text.take(100_000) + "\n[…truncated]" else text
    }

    private fun extractMentionedDates(response: String): List<String> =
        Regex("""\d{4}-\d{2}-\d{2}""").findAll(response)
            .map { it.value }.distinct().take(3).toList()

    fun clearConversation() { _messages.value = emptyList() }

    fun summarisePeriod(from: String, to: String) =
        ask("Please summarise everything I wrote between $from and $to.")

    fun summarisePage(dateKey: String) =
        ask("Please summarise what I wrote on $dateKey.")
}
