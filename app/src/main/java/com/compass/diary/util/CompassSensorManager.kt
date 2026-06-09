package com.compass.diary.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Wraps Android SensorManager to emit the current compass bearing (0–360°).
 * Uses ROTATION_VECTOR sensor for accuracy; falls back to MAGNETIC_FIELD + ACCELEROMETER.
 */
@Singleton
class CompassSensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val magneticSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelerometerSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val hasCompass: Boolean
        get() = rotationVectorSensor != null ||
                (magneticSensor != null && accelerometerSensor != null)

    /**
     * Returns a Flow of compass headings in degrees (0 = North, 90 = East, …).
     */
    fun headingFlow(): Flow<Float> = callbackFlow {
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var smoothedHeading = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val rawDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        val heading = (rawDeg + 360f) % 360f
                        smoothedHeading = lowPassFilter(smoothedHeading, heading)
                        trySend(smoothedHeading)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        lowPassFilter3(event.values, gravity)
                        emitFromGeoMag(gravity, geomagnetic)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        emitFromGeoMag(gravity, geomagnetic)
                    }
                }
            }

            private fun emitFromGeoMag(g: FloatArray, m: FloatArray) {
                if (g.all { it == 0f }) return
                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, g, m)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    val rawDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val heading = (rawDeg + 360f) % 360f
                    smoothedHeading = lowPassFilter(smoothedHeading, heading)
                    trySend(smoothedHeading)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            magneticSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            accelerometerSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /**
     * Check whether [heading] is within [tolerance] degrees of [target].
     * Handles the 0°/360° wraparound.
     */
    fun isNearAngle(heading: Float, target: Float, tolerance: Float = 20f): Boolean {
        val diff = abs(((heading - target + 540f) % 360f) - 180f)
        return diff <= tolerance
    }

    /** Simple low-pass filter to smooth compass readings. */
    private fun lowPassFilter(current: Float, newValue: Float, alpha: Float = 0.15f): Float {
        // Handle 0/360 wraparound
        var diff = newValue - current
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return (current + alpha * diff + 360f) % 360f
    }

    private fun lowPassFilter3(input: FloatArray, output: FloatArray, alpha: Float = 0.15f) {
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }
}
