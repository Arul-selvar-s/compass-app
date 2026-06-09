package com.compass.diary.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "compass_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ─── Keys ─────────────────────────────────────────────────────
    companion object {
        val KEY_FIRST_LAUNCH       = booleanPreferencesKey("first_launch")
        val KEY_UNLOCK_ANGLE_1     = floatPreferencesKey("unlock_angle_1")
        val KEY_UNLOCK_ANGLE_2     = floatPreferencesKey("unlock_angle_2")
        val KEY_UNLOCK_TOLERANCE   = floatPreferencesKey("unlock_tolerance")
        val KEY_BIOMETRIC_ENABLED  = booleanPreferencesKey("biometric_enabled")
        val KEY_DARK_MODE          = stringPreferencesKey("dark_mode")   // SYSTEM|DARK|LIGHT
        val KEY_GOOGLE_ACCOUNT     = stringPreferencesKey("google_account")
        val KEY_DRIVE_FOLDER_ID    = stringPreferencesKey("drive_folder_id")
        val KEY_AUTO_SYNC          = booleanPreferencesKey("auto_sync")
        val KEY_NOTIFICATIONS      = booleanPreferencesKey("notifications_enabled")
        val KEY_FONT_SIZE          = floatPreferencesKey("font_size")
        val KEY_ANTHROPIC_API_KEY  = stringPreferencesKey("anthropic_api_key")
        val KEY_SETUP_COMPLETE     = booleanPreferencesKey("setup_complete")
        val KEY_DB_PASSPHRASE      = stringPreferencesKey("db_passphrase_enc") // encrypted
        val KEY_LAST_SYNC          = longPreferencesKey("last_sync")
    }

    private val ds = context.dataStore

    // ─── Readers ──────────────────────────────────────────────────

    val isFirstLaunch: Flow<Boolean> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_FIRST_LAUNCH] ?: true }

    val isSetupComplete: Flow<Boolean> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_SETUP_COMPLETE] ?: false }

    val unlockAngles: Flow<Pair<Float, Float>?> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val a1 = prefs[KEY_UNLOCK_ANGLE_1]
            val a2 = prefs[KEY_UNLOCK_ANGLE_2]
            if (a1 != null && a2 != null) Pair(a1, a2) else null
        }

    val unlockTolerance: Flow<Float> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_UNLOCK_TOLERANCE] ?: 20f }

    val isBiometricEnabled: Flow<Boolean> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_BIOMETRIC_ENABLED] ?: false }

    val darkMode: Flow<String> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_DARK_MODE] ?: "SYSTEM" }

    val googleAccount: Flow<String?> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_GOOGLE_ACCOUNT] }

    val driveFolderId: Flow<String?> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_DRIVE_FOLDER_ID] }

    val isAutoSyncEnabled: Flow<Boolean> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_AUTO_SYNC] ?: true }

    val isNotificationsEnabled: Flow<Boolean> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_NOTIFICATIONS] ?: false }

    val fontSize: Flow<Float> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_FONT_SIZE] ?: 16f }

    val anthropicApiKey: Flow<String?> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_ANTHROPIC_API_KEY] }

    // ─── Writers ──────────────────────────────────────────────────

    suspend fun setFirstLaunch(value: Boolean) =
        ds.edit { it[KEY_FIRST_LAUNCH] = value }

    suspend fun setSetupComplete(value: Boolean) =
        ds.edit { it[KEY_SETUP_COMPLETE] = value }

    suspend fun setUnlockAngles(angle1: Float, angle2: Float, tolerance: Float = 20f) {
        ds.edit {
            it[KEY_UNLOCK_ANGLE_1]   = angle1
            it[KEY_UNLOCK_ANGLE_2]   = angle2
            it[KEY_UNLOCK_TOLERANCE] = tolerance
        }
    }

    suspend fun setBiometricEnabled(value: Boolean) =
        ds.edit { it[KEY_BIOMETRIC_ENABLED] = value }

    suspend fun setDarkMode(mode: String) =
        ds.edit { it[KEY_DARK_MODE] = mode }

    suspend fun setGoogleAccount(email: String?) {
        ds.edit { prefs ->
            if (email != null) prefs[KEY_GOOGLE_ACCOUNT] = email
            else prefs.remove(KEY_GOOGLE_ACCOUNT)
        }
    }

    suspend fun setDriveFolderId(id: String) =
        ds.edit { it[KEY_DRIVE_FOLDER_ID] = id }

    suspend fun setAutoSync(value: Boolean) =
        ds.edit { it[KEY_AUTO_SYNC] = value }

    suspend fun setNotificationsEnabled(value: Boolean) =
        ds.edit { it[KEY_NOTIFICATIONS] = value }

    suspend fun setFontSize(size: Float) =
        ds.edit { it[KEY_FONT_SIZE] = size }

    suspend fun setAnthropicApiKey(key: String) =
        ds.edit { it[KEY_ANTHROPIC_API_KEY] = key }

    suspend fun setLastSync(time: Long) =
        ds.edit { it[KEY_LAST_SYNC] = time }
}
