package com.example.passwordstorageapp.feature.auth

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.LocalActivity
import androidx.fragment.app.FragmentActivity
import com.example.passwordstorageapp.ui.theme.GradientBackground

@Composable
fun SetupMasterPasswordScreen(
    masterPasswordRepository: MasterPasswordRepository,
    isChange: Boolean = false, // when true, caller expects re-encrypt
    onSetupComplete: (newDerivedKey: ByteArray) -> Unit = {}
) {
    GradientBackground {
        val context = LocalContext.current
        val activity = LocalActivity.current as? FragmentActivity
            ?: error("SetupMasterPasswordScreen must be hosted in a FragmentActivity")
        val biometricKeyStoreManager = remember { BiometricKeyStoreManager(context) }

        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        var pendingDerivedKey by remember { mutableStateOf<ByteArray?>(null) }
        var showBiometricDialog by remember { mutableStateOf(false) }

        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        val cardColors = if (isDark) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
        }

        val cardElevation = if (isDark) 8.dp else 6.dp
        val cardBorder: BorderStroke? = if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)) else null

        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = cardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                    border = cardBorder
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = if (!isChange) "Set your master password" else "Choose a new master password",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (!isChange)
                                "This password encrypts all your stored data. Use something strong and memorable."
                            else
                                "You're changing your master password. This will re-encrypt all stored data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (errorMessage != null) errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Master password") },
                            placeholder = { Text("At least 6 chars with A–Z, 0–9, symbol") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                if (errorMessage != null) errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm master password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                        )

                        if (errorMessage != null) {
                            Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                when {
                                    password.isBlank() || confirmPassword.isBlank() -> errorMessage = "Password fields cannot be empty"
                                    password != confirmPassword -> errorMessage = "Passwords do not match"
                                    !isValidPassword(password) -> errorMessage = "Password is too weak. Use uppercase, digit, and symbol."
                                    else -> {
                                        val derivedKey = masterPasswordRepository.setMasterPassword(password)
                                        if (derivedKey != null) {
                                            pendingDerivedKey = derivedKey
                                            errorMessage = null
                                            showBiometricDialog = true
                                        } else {
                                            errorMessage = "Failed to set master password"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(text = "Confirm & continue", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Important:\nYour master password cannot be recovered.\nIf you forget it, all stored data will be permanently inaccessible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showBiometricDialog && pendingDerivedKey != null) {
                AlertDialog(
                    onDismissRequest = {
                        // if dismissed, treat as Not now in change flow: ensure no stale old biometric remains
                        if (isChange) {
                            try { biometricKeyStoreManager.clearStoredDerivedKey() } catch (_: Exception) {}
                            try { biometricKeyStoreManager.deleteKeystoreKey() } catch (_: Exception) {}
                        }

                        val newDerived = masterPasswordRepository.verifyPassword(password)
                        pendingDerivedKey?.fill(0)
                        pendingDerivedKey = null
                        showBiometricDialog = false
                        if (newDerived != null) onSetupComplete(newDerived) else onSetupComplete(ByteArray(0))
                    },
                    title = { Text(text = "Enable biometric unlock?", style = MaterialTheme.typography.titleMedium) },
                    text = {
                        Text(text = "You can unlock the vault using fingerprint or face. Your master password will still be required if biometrics fail.", style = MaterialTheme.typography.bodyMedium)
                    },
                    confirmButton = {
                        Button(onClick = {
                            val biometricManager = BiometricManager.from(context)
                            val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

                            when (canAuth) {
                                BiometricManager.BIOMETRIC_SUCCESS -> {
                                    try {
                                        val derived = pendingDerivedKey ?: run {
                                            showBiometricDialog = false
                                            onSetupComplete(ByteArray(0))
                                            return@Button
                                        }

                                        val encryptCipher = biometricKeyStoreManager.getEncryptCipher()
                                        val cryptoObj = BiometricPrompt.CryptoObject(encryptCipher)

                                        launchBiometricPromptWithCrypto(
                                            activity = activity,
                                            title = "Enable biometric unlock",
                                            subtitle = "Authenticate to save key securely",
                                            crypto = cryptoObj,
                                            onSuccess = { result ->
                                                val cipher = result.cryptoObject?.cipher
                                                if (cipher != null) {
                                                    try {
                                                        val ciphertext = cipher.doFinal(derived)
                                                        val iv = cipher.iv
                                                        biometricKeyStoreManager.persistEncryptedDerivedKey(iv, ciphertext)
                                                    } catch (e: Exception) {
                                                        // swallow — will fall back to non-biometric
                                                    }
                                                }
                                                onSetupComplete(derived)
                                                pendingDerivedKey = null
                                                showBiometricDialog = false
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                // Even on biometric error, continue with derived key (user chose to set password)
                                                onSetupComplete(derived)
                                                pendingDerivedKey = null
                                                showBiometricDialog = false
                                            }
                                        )
                                    } catch (e: Exception) {
                                        pendingDerivedKey?.fill(0)
                                        pendingDerivedKey = null
                                        showBiometricDialog = false
                                        onSetupComplete(ByteArray(0))
                                    }
                                }

                                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                    Toast.makeText(context, "No biometrics enrolled. Set up fingerprint/face in system settings.", Toast.LENGTH_SHORT).show()
                                    val newDerived = pendingDerivedKey
                                    pendingDerivedKey = null
                                    showBiometricDialog = false
                                    if (newDerived != null) onSetupComplete(newDerived) else onSetupComplete(ByteArray(0))
                                }

                                else -> {
                                    Toast.makeText(context, "Biometric unlock is not available on this device.", Toast.LENGTH_SHORT).show()
                                    val newDerived = pendingDerivedKey
                                    pendingDerivedKey = null
                                    showBiometricDialog = false
                                    if (newDerived != null) onSetupComplete(newDerived) else onSetupComplete(ByteArray(0))
                                }
                            }
                        }) {
                            Text("Enable")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            // when user explicitly chooses Not now during change flow
                            if (isChange) {
                                try { biometricKeyStoreManager.clearStoredDerivedKey() } catch (_: Exception) {}
                                try { biometricKeyStoreManager.deleteKeystoreKey() } catch (_: Exception) {}
                            }

                            pendingDerivedKey?.fill(0)
                            pendingDerivedKey = null
                            showBiometricDialog = false
                            val newDerived = masterPasswordRepository.verifyPassword(password)
                            if (newDerived != null) onSetupComplete(newDerived) else onSetupComplete(ByteArray(0))
                        }) {
                            Text("Not now")
                        }
                    }
                )
            }
        }
    }
}

fun isValidPassword(password: String): Boolean {
    val passwordRegex = Regex("^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#\$%^&+=!]).{6,}$")
    return passwordRegex.matches(password)
}
