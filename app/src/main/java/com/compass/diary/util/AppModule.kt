package com.compass.diary.util

import android.content.Context
import com.compass.diary.data.local.database.AppDatabase
import com.compass.diary.data.local.dao.*
import com.compass.diary.data.remote.AnthropicApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        // Passphrase is derived from device-specific data in production.
        // For this build we use a fixed key; wire EncryptionUtil.deriveDbPassphrase()
        // to the user's compass angles for full security.
        val passphrase = "compass_secure_2026".toByteArray()
        return AppDatabase.create(context, passphrase)
    }

    @Provides fun provideDiaryDao(db: AppDatabase): DiaryDao = db.diaryDao()
    @Provides fun provideStarredDao(db: AppDatabase): StarredDao = db.starredDao()
    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideVersionHistoryDao(db: AppDatabase): VersionHistoryDao = db.versionHistoryDao()
    @Provides fun provideDrawingDao(db: AppDatabase): DrawingDao = db.drawingDao()
    @Provides fun provideVoiceNoteDao(db: AppDatabase): VoiceNoteDao = db.voiceNoteDao()

    @Provides
    @Singleton
    fun provideAnthropicApiService(): AnthropicApiService = AnthropicApiService()
}
