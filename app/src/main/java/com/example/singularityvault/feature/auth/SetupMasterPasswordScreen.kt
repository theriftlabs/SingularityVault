package com.riftlabs.singularityvault.feature.auth

import android.app.Activity
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.riftlabs.singularityvault.ui.theme.GradientBackground

@Composable
fun SetupMasterPasswordScreen(
    masterPasswordRepository: MasterPasswordRepository,
    onSetupComplete: () -> Unit = {}
) {
    // Configure status bar appearance once per screen using SideEffect
    val view = LocalView.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SideEffect {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        
        // Configure status bar for visibility in both themes
        if (isDarkTheme) {
            // Dark theme: dark background with light icons
            window.statusBarColor = android.graphics.Color.parseColor("#0F172A")
            insetsController.isAppearanceLightStatusBars = false
        } else {
            // Light theme: transparent background with dark icons
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            insetsController.isAppearanceLightStatusBars = true
        }
    }

    GradientBackground {
        val context = LocalContext.current
        val biometricKeyStoreManager = remember { BiometricKeyStoreManager(context) }

        var password by rememberSaveable { mutableStateOf("") }
        var confirmPassword by rememberSaveable { mutableStateOf("") }
        var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

        // ByteArray cannot be saved in rememberSaveable - will regenerate if needed
        var pendingDerivedKey by remember { mutableStateOf<ByteArray?>(null) }
        var showBiometricDialog by rememberSaveable { mutableStateOf(false) }

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

        val scrollState = rememberScrollState()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),  // Enable scrolling when keyboard opens
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card over gradient
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
                            text = "Set your master password",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "This password encrypts all your stored data. Use something strong and memorable.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (errorMessage != null) errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    // Mark as password for autofill & accessibility
                                    password()
                                },
                            label = { Text("Master password") },
                            placeholder = { Text("At least 6 chars with A–Z, 0–9, symbol") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                // KeyboardType.Password requests keyboards to:
                                // - Disable text predictions
                                // - Disable autocorrect
                                // - Hide typed characters (with PasswordVisualTransformation)
                                // Note: Keyboards may still show clipboard - that's keyboard-level behavior
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            )
                        )

                        // Confirm password field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                if (errorMessage != null) errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    password()
                                },
                            label = { Text("Confirm master password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )

                        // Error message
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Confirm button
                        Button(
                            onClick = {
                                when {
                                    password.isBlank() || confirmPassword.isBlank() -> {
                                        errorMessage = "Password fields cannot be empty"
                                    }

                                    password != confirmPassword -> {
                                        errorMessage = "Passwords do not match"
                                    }

                                    !isValidPassword(password) -> {
                                        errorMessage =
                                            "Password is too weak. Use uppercase, digit, and symbol."
                                    }

                                    else -> {
                                        val derivedKey =
                                            masterPasswordRepository.setMasterPassword(password)
                                        pendingDerivedKey = derivedKey
                                        errorMessage = null
                                        showBiometricDialog = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "Confirm & continue",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Warning under card on gradient
                Text(
                    text = "Important:\n" +
                            "Your master password cannot be recovered.\n" +
                            "If you forget it, all stored data will be permanently inaccessible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---------------------------
            // BIOMETRIC DIALOG
            // ---------------------------
            if (showBiometricDialog) {
                // Regenerate key if lost due to rotation
                LaunchedEffect(Unit) {
                    if (pendingDerivedKey == null && password.isNotBlank()) {
                        pendingDerivedKey = masterPasswordRepository.setMasterPassword(password)
                    }
                }
                
                if (pendingDerivedKey != null) {
                AlertDialog(
                    onDismissRequest = {
                        showBiometricDialog = false
                        pendingDerivedKey = null
                        onSetupComplete()
                    },
                    title = {
                        Text(
                            text = "Enable biometric unlock?",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Text(
                            text = "You can unlock the vault using fingerprint or face. " +
                                    "Your master password will still be required if biometrics fail.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val biometricManager = BiometricManager.from(context)
                                val canAuth = biometricManager.canAuthenticate(
                                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                                )

                                when (canAuth) {
                                    BiometricManager.BIOMETRIC_SUCCESS -> {
                                        pendingDerivedKey?.let {
                                            biometricKeyStoreManager.saveDerivedKey(it)
                                        }
                                        showBiometricDialog = false
                                        pendingDerivedKey = null
                                        onSetupComplete()
                                    }

                                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                        Toast.makeText(
                                            context,
                                            "No biometrics enrolled. Set up at least fingerprint in system settings.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showBiometricDialog = false
                                        pendingDerivedKey = null
                                        onSetupComplete()
                                    }

                                    else -> {
                                        Toast.makeText(
                                            context,
                                            "Biometric unlock is not available on this device.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showBiometricDialog = false
                                        pendingDerivedKey = null
                                        onSetupComplete()
                                    }
                                }
                            }
                        ) {
                            Text("Enable")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showBiometricDialog = false
                                pendingDerivedKey = null
                                onSetupComplete()
                            }
                        ) {
                            Text("Not now")
                        }
                    }
                )
                }
            }
        }
    }
}

fun isValidPassword(password: String): Boolean {
    val passwordRegex = Regex("^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#\$%^&+=!]).{6,}$")
    return passwordRegex.matches(password)
}
