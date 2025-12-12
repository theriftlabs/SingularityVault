package com.example.passwordstorageapp.feature.auth

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import android.os.Build

/**
 * Small helper to launch biometric prompt and wire callbacks.
 * Uses the provided cryptoObject (can be null) when calling authenticate.
 */
fun launchBiometricPromptWithCrypto(
    activity: FragmentActivity,
    title: String,
    subtitle: String? = null,
    crypto: BiometricPrompt.CryptoObject?,
    onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
    onError: (String) -> Unit
) {
    val executor: Executor = ContextCompat.getMainExecutor(activity)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess(result)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            onError(errString.toString())
        }

        override fun onAuthenticationFailed() {
            // Non-fatal failure; let caller decide whether to show message
        }
    }

    val prompt = BiometricPrompt(activity, executor, callback)

    val builder = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setNegativeButtonText("Cancel")

    if (!subtitle.isNullOrBlank()) builder.setSubtitle(subtitle)

    // For modern devices the old 'allowedAuthenticators' is optional; BiometricPrompt will
    // choose appropriate authenticators. Keep minimal to be broadly compatible.
    val promptInfo = builder.build()

    if (crypto != null) {
        prompt.authenticate(promptInfo, crypto)
    } else {
        prompt.authenticate(promptInfo)
    }
}
