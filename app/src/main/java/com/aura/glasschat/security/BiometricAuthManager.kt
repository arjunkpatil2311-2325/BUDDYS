package com.aura.glasschat.security

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

object BiometricAuthManager {

    fun isBiometricHardwareAvailable(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val biometricManager = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                val canAuth = biometricManager?.canAuthenticate(
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                canAuth == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as? android.hardware.fingerprint.FingerprintManager
                fingerprintManager != null && fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun promptBiometricAuthentication(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ): CancellationSignal? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val cancellationSignal = CancellationSignal()
                val executor: Executor = ContextCompat.getMainExecutor(activity)

                val prompt = BiometricPrompt.Builder(activity)
                    .setTitle("Unlock Buddies")
                    .setSubtitle("Confirm your fingerprint or biometric credential")
                    .setDescription("Touch the fingerprint sensor to access your Buddies space")
                    .setNegativeButton("Use PIN", executor) { _, _ ->
                        cancellationSignal.cancel()
                    }
                    .build()

                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            // errorCode 10 = USER_CANCELED, 13 = NEGATIVE_BUTTON, 5 = CANCELED
                            if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                                errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED &&
                                errorCode != 13 // BIOMETRIC_ERROR_NEGATIVE_BUTTON
                            ) {
                                onError(errString?.toString() ?: "Biometric error")
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onError("Biometric not recognized. Please try again or use PIN.")
                        }
                    }
                )
                cancellationSignal
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val cancellationSignal = CancellationSignal()
                val fingerprintManager = activity.getSystemService(Context.FINGERPRINT_SERVICE) as? android.hardware.fingerprint.FingerprintManager
                if (fingerprintManager != null && fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()) {
                    fingerprintManager.authenticate(
                        null,
                        cancellationSignal,
                        0,
                        object : android.hardware.fingerprint.FingerprintManager.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: android.hardware.fingerprint.FingerprintManager.AuthenticationResult?) {
                                super.onAuthenticationSucceeded(result)
                                onSuccess()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                                super.onAuthenticationError(errorCode, errString)
                                if (errorCode != 5) {
                                    onError(errString?.toString() ?: "Fingerprint error")
                                }
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                onError("Fingerprint not recognized. Try again or enter PIN.")
                            }
                        },
                        null
                    )
                    cancellationSignal
                } else {
                    onError("No enrolled fingerprint found on this device.")
                    null
                }
            } else {
                onError("Biometric authentication is not supported on this Android version.")
                null
            }
        } catch (e: Exception) {
            onError("Biometric prompt failed: ${e.localizedMessage ?: "Unknown error"}")
            null
        }
    }
}
