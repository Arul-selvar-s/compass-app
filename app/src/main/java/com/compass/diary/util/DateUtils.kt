package com.compass.diary.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────
// DATE UTILITIES
// ─────────────────────────────────────────────────────────────────
object DateUtils {

    private val ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    /** Today's date key: "2026-06-07" */
    fun todayKey(): String = LocalDate.now().format(ISO_FORMATTER)

    /** "2026-06-07" → "Sunday, June 7, 2026" */
    fun keyToTitle(dateKey: String): String {
        return try {
            val date = LocalDate.parse(dateKey, ISO_FORMATTER)
            val dow   = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val month = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            "$dow, $month ${date.dayOfMonth}, ${date.year}"
        } catch (e: Exception) { dateKey }
    }

    /** "2026-06-07" → "07 Jun" */
    fun keyToShort(dateKey: String): String {
        return try {
            LocalDate.parse(dateKey).format(DateTimeFormatter.ofPattern("dd MMM"))
        } catch (e: Exception) { dateKey }
    }

    /** Returns all date keys in a month: ["2026-06-01", …, "2026-06-30"] */
    fun keysInMonth(yearMonth: YearMonth): List<String> {
        return (1..yearMonth.lengthOfMonth()).map { day ->
            yearMonth.atDay(day).format(ISO_FORMATTER)
        }
    }

    /** Returns the date key [n] days ago */
    fun daysAgo(n: Long): String =
        LocalDate.now().minusDays(n).format(ISO_FORMATTER)

    /** Human-readable relative time (e.g. "2 hours ago", "3 days ago") */
    fun relativeTime(epochMs: Long): String {
        val diff = System.currentTimeMillis() - epochMs
        return when {
            diff < TimeUnit.MINUTES.toMillis(1)  -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1)    -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1)     -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < TimeUnit.DAYS.toMillis(7)     -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            else -> {
                val date = java.util.Date(epochMs)
                java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// RICH CONTENT SERIALISER
// Converts between the internal plain text format and display.
// In a future version this would use a proper AST (e.g. Quill delta).
// ─────────────────────────────────────────────────────────────────
object ContentSerializer {

    /**
     * Extracts plain text from contentJson for search indexing and word-count.
     * Current implementation is trivial (content IS plain text);
     * extend when switching to a rich AST format.
     */
    fun extractPlainText(contentJson: String): String = contentJson.trim()

    /** Count words in a string */
    fun wordCount(text: String): Int =
        text.split(Regex("\\s+")).count { it.isNotBlank() }

    /** Extract tags from diary text (lines starting with # are treated as tags) */
    fun extractTags(text: String): List<String> =
        Regex("""(?<=\s|^)#(\w+)""").findAll(text)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .take(20)
            .toList()

    /** Format tags for storage (comma-separated) */
    fun tagsToString(tags: List<String>): String = tags.joinToString(",")

    /** Parse stored tags */
    fun stringToTags(stored: String): List<String> =
        stored.split(",").filter { it.isNotBlank() }
}
