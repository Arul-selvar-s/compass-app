package com.compass.diary.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.compass.diary.MainActivity

// ─────────────────────────────────────────────────────────────────
// BOOT RECEIVER — reschedules reminder alarms after device reboot
// ─────────────────────────────────────────────────────────────────
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // TODO: query reminder DB in a background thread and re-schedule alarms
    }
}

// ─────────────────────────────────────────────────────────────────
// REMINDER RECEIVER — fires when an alarm triggers
// ─────────────────────────────────────────────────────────────────
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID  = "compass_reminders"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_NOTE  = "reminder_note"
        const val EXTRA_ID    = "reminder_id"

        fun createPendingIntent(
            context: Context,
            reminderId: Long,
            title: String,
            note: String
        ): PendingIntent {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_ID,    reminderId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_NOTE,  note)
            }
            return PendingIntent.getBroadcast(
                context, reminderId.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Compass Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Diary reminders from Compass" }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Compass Reminder"
        val note  = intent.getStringExtra(EXTRA_NOTE)  ?: ""
        val id    = intent.getLongExtra(EXTRA_ID, 0L)

        createChannel(context)

        val tap = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(note.ifBlank { "Time to write in your diary!" })
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(id.toInt(), notification)
    }
}
