package com.example.passwordstorageapp.feature.auth

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.passwordstorageapp.ui.theme.GradientBackground
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.luminance

@Composable
fun UnlockScreen(
    masterPasswordRepository: MasterPasswordRepository,
    onUnlockSuccess: (ByteArray) -> Unit = {}
) {
    GradientBackground {
        // for local context
        val context = LocalContext.current

        // Defensive cast — fail loudly if not inside FragmentActivity
        val activity = context as? FragmentActivity
            ?: error("UnlockScreen must be hosted in a FragmentActivity")

        val biometricKeyStoreManager = remember { BiometricKeyStoreManager(context) }
        val hasBiometric =
            biometricKeyStoreManager.isBiometricAvailable() &&
                    biometricKeyStoreManager.loadDerivedKey() != null


        var password by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Dark / light detection for card styling
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        val cardColors = if (isDark) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            )
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            )
        }

        val cardElevation = if (isDark) 8.dp else 6.dp
        val cardBorder: BorderStroke? =
            if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            else null

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = cardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Unlock your vault",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Enter your master password to decrypt your data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Master password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val derivedKey = masterPasswordRepository.verifyPassword(password)
                                if (derivedKey != null) {
                                    errorMessage = null
                                    // Cache derived key for biometric unlock next time
                                    if(hasBiometric){
                                        biometricKeyStoreManager.saveDerivedKey(derivedKey)
                                    }
                                    onUnlockSuccess(derivedKey)
                                } else {
                                    errorMessage = "Incorrect master password"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "Unlock",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        if (hasBiometric) {
                            OutlinedButton(
                                onClick = {
                                    launchBiometricPrompt(
                                        activity = activity,
                                        biometricKeyStoreManager = biometricKeyStoreManager,
                                        onSuccess = onUnlockSuccess,
                                        onError = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = "Unlock with biometrics",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Singularity Vault keeps everything encrypted locally. " +
                            "Your master password is never stored or sent anywhere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun launchBiometricPrompt(
    activity: FragmentActivity,
    biometricKeyStoreManager: BiometricKeyStoreManager,
    onSuccess: (ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)

    val callback = object : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)

            val derivedKey = biometricKeyStoreManager.loadDerivedKey()
            if (derivedKey != null) {
                onSuccess(derivedKey)
            } else {
                // This is a real error
                onError("No stored biometric key found")
            }
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)

            // 👇 User cancelled or tapped "Use master password"
            if (
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                errorCode == BiometricPrompt.ERROR_USER_CANCELED
            ) {
                // Silent exit — this is expected behavior
                return
            }

            // Real biometric error
            onError(errString.toString())
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            // Fingerprint mismatch etc.
            // No toast needed — system already gives feedback
        }
    }

    val biometricPrompt = BiometricPrompt(activity, executor, callback)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Singularity Vault")
        .setSubtitle("Use your fingerprint or face")
        .setNegativeButtonText("Use master password")
        .build()

    biometricPrompt.authenticate(promptInfo)
}
