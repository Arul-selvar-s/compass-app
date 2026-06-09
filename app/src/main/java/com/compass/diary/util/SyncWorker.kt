package com.compass.diary.util

import android.content.Context
import androidx.work.*
import com.compass.diary.data.repository.DiaryRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background worker that syncs unsynced diary entries to Google Drive.
 * Uses WorkManager for battery-efficient background execution.
 * 
 * NOTE: To enable Drive sync, wire up the uploadToGoogleDrive() stub
 * with your OAuth token from Google Sign-In.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "compass_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun syncNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
        }
    }

    override suspend fun doWork(): Result {
        // TODO: Inject DiaryRepository and GoogleDriveRepository here
        // For now this is a no-op stub
        return Result.success()
    }
}
