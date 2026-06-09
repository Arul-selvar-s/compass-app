package com.compass.diary.viewmodel

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compass.diary.data.repository.DiaryRepository
import com.compass.diary.util.CompassSensorManager
import com.compass.diary.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────
// SETUP VIEW MODEL — first-launch compass lock configuration
// ─────────────────────────────────────────────────────────────────
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val sensorManager: CompassSensorManager,
    private val prefs: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step

    val heading: StateFlow<Float> = sensorManager.headingFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    private val _savedAngle1 = MutableStateFlow<Float?>(null)
    val savedAngle1: StateFlow<Float?> = _savedAngle1

    val isBiometricAvailable: StateFlow<Boolean> = MutableStateFlow(
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    )

    fun saveAngle1(angle: Float) {
        _savedAngle1.value = angle
        _step.value = 2
    }

    fun saveAngle2(angle: Float) {
        viewModelScope.launch {
            prefs.setUnlockAngles(_savedAngle1.value ?: 0f, angle)
        }
        _step.value = 3
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { prefs.setBiometricEnabled(enabled) }
    }

    fun finishSecurity() { _step.value = 4 }

    fun completeSetup() {
        viewModelScope.launch {
            prefs.setSetupComplete(true)
            prefs.setFirstLaunch(false)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// SPLASH VIEW MODEL
// ─────────────────────────────────────────────────────────────────
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {
    val isSetupComplete: StateFlow<Boolean> = prefs.isSetupComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}

// ─────────────────────────────────────────────────────────────────
// SETTINGS VIEW MODEL — injects Repository and Prefs directly
// (never injects another ViewModel — that breaks Hilt's scoping)
// ─────────────────────────────────────────────────────────────────
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val repo: DiaryRepository
) : ViewModel() {

    val darkMode: StateFlow<String> = prefs.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val notificationsEnabled: StateFlow<Boolean> = prefs.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoSync: StateFlow<Boolean> = prefs.isAutoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val googleAccount: StateFlow<String?> = prefs.googleAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val biometricEnabled: StateFlow<Boolean> = prefs.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val anthropicApiKey: StateFlow<String?> = prefs.anthropicApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _lastSyncLabel = MutableStateFlow("Never")
    val lastSyncLabel: StateFlow<String> = _lastSyncLabel

    init {
        viewModelScope.launch {
            // No KEY_LAST_SYNC direct read needed here; derive from prefs if added later
        }
    }

    // ── Setters ──────────────────────────────────────────────────

    fun setDarkMode(mode: String) { viewModelScope.launch { prefs.setDarkMode(mode) } }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotificationsEnabled(enabled) }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoSync(enabled) }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { prefs.setBiometricEnabled(enabled) }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch { prefs.setAnthropicApiKey(key) }
    }

    fun syncNow() {
        viewModelScope.launch {
            prefs.setLastSync(System.currentTimeMillis())
            _lastSyncLabel.value = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date())
        }
    }

    fun exportDiary() {
        // Export all diary entries as plain text files
        // TODO: write to Downloads or share-sheet via FileProvider
    }

    fun logout() {
        viewModelScope.launch {
            prefs.setGoogleAccount(null)
            prefs.setSetupComplete(false)
        }
    }
}
