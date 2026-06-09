package com.compass.diary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compass.diary.util.CompassSensorManager
import com.compass.diary.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompassViewModel @Inject constructor(
    private val sensorManager: CompassSensorManager,
    private val prefs: PreferencesManager
) : ViewModel() {

    enum class UnlockState { IDLE, STEP_1, STEP_2, UNLOCKED, FAILED }

    // Live compass heading
    val heading: StateFlow<Float> = sensorManager.headingFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    private val _unlockState = MutableStateFlow(UnlockState.IDLE)
    val unlockState: StateFlow<UnlockState> = _unlockState

    private val _unlockStep = MutableStateFlow(0)
    val unlockStep: StateFlow<Int> = _unlockStep

    private var targetAngle1 = 0f
    private var targetAngle2 = 0f
    private var tolerance    = 20f

    init {
        viewModelScope.launch {
            prefs.unlockAngles.collect { angles ->
                if (angles != null) {
                    targetAngle1 = angles.first
                    targetAngle2 = angles.second
                }
            }
        }
        viewModelScope.launch {
            prefs.unlockTolerance.collect { tolerance = it }
        }
    }

    /** Called when user long-presses the compass center */
    fun startUnlock() {
        _unlockState.value = UnlockState.STEP_1
        _unlockStep.value  = 1
    }

    /** Called when user taps the compass during unlock */
    fun confirmStep() {
        val currentHeading = heading.value
        when (_unlockState.value) {
            UnlockState.STEP_1 -> {
                if (sensorManager.isNearAngle(currentHeading, targetAngle1, tolerance)) {
                    _unlockState.value = UnlockState.STEP_2
                    _unlockStep.value  = 2
                } else {
                    failUnlock()
                }
            }
            UnlockState.STEP_2 -> {
                if (sensorManager.isNearAngle(currentHeading, targetAngle2, tolerance)) {
                    _unlockState.value = UnlockState.UNLOCKED
                    _unlockStep.value  = 0
                } else {
                    failUnlock()
                }
            }
            else -> {}
        }
    }

    private fun failUnlock() {
        _unlockState.value = UnlockState.FAILED
        _unlockStep.value  = 0
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            _unlockState.value = UnlockState.IDLE
        }
    }

    fun resetUnlock() {
        _unlockState.value = UnlockState.IDLE
        _unlockStep.value  = 0
    }
}
