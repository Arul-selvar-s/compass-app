package com.compass.diary.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.compass.diary.data.local.dao.*
import com.compass.diary.data.local.entity.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        DiaryEntryEntity::class,
        StarredItemEntity::class,
        ReminderEntity::class,
        VersionHistoryEntity::class,
        SharedPageEntity::class,
        DrawingEntity::class,
        VoiceNoteEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun diaryDao(): DiaryDao
    abstract fun starredDao(): StarredDao
    abstract fun reminderDao(): ReminderDao
    abstract fun versionHistoryDao(): VersionHistoryDao
    abstract fun drawingDao(): DrawingDao
    abstract fun voiceNoteDao(): VoiceNoteDao

    companion object {
        const val DATABASE_NAME = "compass_diary.db"

        fun create(context: Context, passphrase: ByteArray): AppDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }
    }
}
