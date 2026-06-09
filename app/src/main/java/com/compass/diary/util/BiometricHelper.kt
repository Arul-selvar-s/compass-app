package com.compass.diary.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Thin wrapper around AndroidX BiometricPrompt.
 *
 * Usage (from any Activity/Fragment):
 *
 *   BiometricHelper.authenticate(
 *       activity = this,
 *       title    = "Unlock Compass",
 *       onSuccess = { openDiary() },
 *       onError   = { msg -> showError(msg) }
 *   )
 */
object BiometricHelper {

    enum class Availability {
        AVAILABLE,
        NO_HARDWARE,
        NOT_ENROLLED,
        TEMPORARILY_LOCKED,
        UNAVAILABLE
    }

    fun checkAvailability(context: Context): Availability {
        val bm = BiometricManager.from(context)
        return when (bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS                   -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE         -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED       -> Availability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Availability.UNAVAILABLE
            else                                                  -> Availability.UNAVAILABLE
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title:    String = "Unlock Compass",
        subtitle: String = "Use your fingerprint or face to unlock",
        onSuccess: () -> Unit,
        onError:   (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // errorCode 10 = USER_CANCELED, 13 = NEGATIVE_BUTTON — treat these silently
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    onError("Biometric error: $errString")
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Single failed attempt — BiometricPrompt handles retry UI itself
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(info)
    }
}
