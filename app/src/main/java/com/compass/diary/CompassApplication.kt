package com.compass.diary

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class CompassApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialise SQLCipher native libraries
        SQLiteDatabase.loadLibs(this)
    }
}
