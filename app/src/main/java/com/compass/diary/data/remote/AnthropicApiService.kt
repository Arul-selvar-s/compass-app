package com.compass.diary.data.remote

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────
// Anthropic Claude API client for the AI Diary Assistant
// ─────────────────────────────────────────────────────────────────
class AnthropicApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-sonnet-4-20250514"
        private const val ANTHROPIC_VERSION = "2023-06-01"
    }

    /**
     * Ask the AI a question about the diary, given context entries.
     * @param apiKey        User's Anthropic API key (stored encrypted)
     * @param userQuestion  The user's natural-language question
     * @param diaryContext  Relevant diary entries serialised to plain text
     * @return AI response text
     */
    suspend fun askAboutDiary(
        apiKey: String,
        userQuestion: String,
        diaryContext: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are a personal diary assistant for the Compass app.
                You have access to the user's diary entries provided below.
                
                Answer questions about their diary accurately and empathetically.
                When you reference something from the diary, cite the date (e.g. "On June 7, 2026, you wrote…").
                If the diary doesn't contain information about the topic, say so honestly.
                Keep answers concise but complete.
                
                --- DIARY ENTRIES ---
                $diaryContext
                --- END OF ENTRIES ---
            """.trimIndent()

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userQuestion)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 1024)
                put("system", systemPrompt)
                put("messages", messagesArray)
            }.toString()

            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .addHeader("content-type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("API error ${response.code}: $responseBody")
                )
            }

            val json = JSONObject(responseBody)
            val content = json.getJSONArray("content")
            val text = content.getJSONObject(0).getString("text")
            Result.success(text)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Summarise a set of diary entries.
     */
    suspend fun summariseEntries(
        apiKey: String,
        entries: String,
        summaryType: String = "concise"
    ): Result<String> = askAboutDiary(
        apiKey,
        "Please give me a $summaryType summary of these diary entries, highlighting key themes, emotions, and events.",
        entries
    )
}
