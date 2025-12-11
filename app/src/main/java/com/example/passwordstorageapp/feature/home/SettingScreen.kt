package com.example.passwordstorageapp.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.passwordstorageapp.ui.theme.GradientBackground
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.passwordstorageapp.feature.auth.BiometricKeyStoreManager
import com.example.passwordstorageapp.feature.auth.MasterPasswordRepository
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    masterPasswordRepository: MasterPasswordRepository,
    darkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onIdleTimeout: () -> Unit,
    onVerifyCurrentPassword: (currentPassword: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit = { _, s, f -> s() },
    onNavigateToSetupChangeMode: (verifiedCurrentPassword: String) -> Unit = {}
) {
    val context = LocalContext.current
    val biometricKeyStoreManager = remember { BiometricKeyStoreManager(context) }
    var biometricsEnabled by remember {
        mutableStateOf(biometricKeyStoreManager.loadDerivedKey() != null)
    }

    // ---------- Idle timer stuff ----------
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    var showVerifyDialog by remember { mutableStateOf(false) }
    var currentPwVerify by remember { mutableStateOf("") }
    var showCurrentVerify by remember { mutableStateOf(false) }
    var verifySubmitting by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }

    fun touch() {
        lastInteractionTime = System.currentTimeMillis()
    }

    // global interaction: any touch, scroll, drag etc updates lastInteractionTime
    val interactionModifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope {
                    awaitPointerEvent()
                    touch()
                }
            }
        }

    // ---------- Card styling ----------
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

    GradientBackground {
        Box(modifier = interactionModifier) {   // 🧠 attach global touch detector here
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Column {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    touch()
                                    onBack()
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        )
                        // bottom divider for separation
                        Divider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {

                    // Master password card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = cardColors,
                        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                        border = cardBorder
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VpnKey,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Master password",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Change your master password. This will re-encrypt your stored data.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Button(
                                onClick = {
                                    touch()

                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Change password")
                            }
                        }
                    }

                    // Biometrics card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = cardColors,
                        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                        border = cardBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Biometric unlock",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Use fingerprint / face to unlock your vault faster.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }

                            Switch(
                                checked = biometricsEnabled,
                                onCheckedChange = {
                                    biometricsEnabled = it
                                    touch()
                                }
                            )
                        }
                    }

                    // Theme card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = cardColors,
                        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                        border = cardBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (darkModeEnabled) {
                                        Icons.Filled.DarkMode
                                    } else {
                                        Icons.Filled.LightMode
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Appearance",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (darkModeEnabled) {
                                            "Dark mode enabled"
                                        } else {
                                            "Light mode enabled"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Switch(
                                checked = darkModeEnabled,
                                onCheckedChange = { enabled ->
                                    touch()
                                    onDarkModeToggle(enabled)
                                }
                            )
                        }
                    }
                }
            }

            if (showVerifyDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (!verifySubmitting) {
                            touch()
                            showVerifyDialog = false
                            verifyError = null
                            currentPwVerify = ""
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                touch()
                                verifyError = null
                                if (currentPwVerify.isBlank()) {
                                    verifyError = "Enter your current password"
                                    return@TextButton
                                }

                                verifySubmitting = true
                                onVerifyCurrentPassword(
                                    currentPwVerify,
                                    {
                                        val verifiedPw = masterPasswordRepository.verifyPassword(currentPwVerify)
                                        if(verifiedPw != null){
                                            verifySubmitting = false
                                            showVerifyDialog = false
                                            onNavigateToSetupChangeMode(currentPwVerify)
                                            currentPwVerify = ""
                                        }
                                    },
                                    { err ->
                                        verifySubmitting = false
                                        verifyError = err.takeIf { it.isNotBlank() } ?: "Verification failed"
                                    }
                                )
                            },
                            enabled = !verifySubmitting
                        ) {
                            Text("Verify")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                touch()
                                if (!verifySubmitting) {
                                    showVerifyDialog = false
                                    verifyError = null
                                    currentPwVerify = ""
                                }
                            },
                            enabled = !verifySubmitting
                        ) {
                            Text("Cancel")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                imageVector = Icons.Filled.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Enter current password", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = currentPwVerify,
                                onValueChange = {
                                    touch()
                                    currentPwVerify = it
                                    if (verifyError != null) verifyError = null
                                },
                                label = { Text("Current password") },
                                singleLine = true,
                                visualTransformation = if (showCurrentVerify) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showCurrentVerify = !showCurrentVerify }) {
                                        Icon(
                                            imageVector = if (showCurrentVerify) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (showCurrentVerify) "Hide" else "Show"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            verifyError?.let { err ->
                                Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            if (verifySubmitting) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator()
                                }
                            }

                            Text(
                                text = "If verification succeeds you will be taken to the master password screen to set a new password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    },
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            LaunchedEffect(Unit) {
                val timeoutMs = 15_000L
                while (true) {
                    delay(3_000L)
                    if (System.currentTimeMillis() - lastInteractionTime >= timeoutMs) {
                        onIdleTimeout()
                        return@LaunchedEffect
                    }
                }
            }
        }
    }
}
