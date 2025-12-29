package com.riftlabs.singularityvault.feature.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.riftlabs.singularityvault.feature.auth.BiometricKeyStoreManager
import com.riftlabs.singularityvault.feature.auth.MasterPasswordRepository
import com.riftlabs.singularityvault.feature.auth.SessionViewModel
import com.riftlabs.singularityvault.ui.theme.GradientBackground
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    masterPasswordRepository: MasterPasswordRepository,
    securitySettingsViewModel: SecuritySettingsViewModel,
    sessionViewModel: SessionViewModel,
    darkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    vaultViewModel: VaultViewModel,
    onRestartIdleWatcher: (enabled: Boolean, timeoutMs: Long) -> Unit
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val settings by securitySettingsViewModel.settings.collectAsState()
    val biometricManager = remember { BiometricKeyStoreManager(context) }
    val systemBiometricManager = BiometricManager.from(context)

    var biometricsEnabled by remember {
        mutableStateOf(biometricManager.loadDerivedKey() != null)
    }

    // Check if biometrics are still available when screen is displayed
    LaunchedEffect(Unit) {
        if (biometricsEnabled) {
            val canAuth = systemBiometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                // Biometrics were enabled but are no longer available
                biometricManager.clearBiometricKey()
                biometricsEnabled = false
            }
        }
    }

    // ---------------- NEW SETTINGS STATE ----------------
    val idleOptions = listOf(15_000L, 30_000L, 45_000L, 60_000L)
    val clipboardOptions = listOf(10_000L, 20_000L, 30_000L)

    // ---------------- Biometric flow ----------------
    var showVerifyDialog by rememberSaveable { mutableStateOf(false) }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var verifyError by rememberSaveable { mutableStateOf<String?>(null) }
    var activeBiometricPrompt by remember { mutableStateOf<BiometricPrompt?>(null) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(
            alpha = if (isDark) 0.88f else 0.96f
        )
    )
    val cardBorder =
        if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        else null

    GradientBackground {
        Box {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.systemBars,
                topBar = {
                    Column {
                        CenterAlignedTopAppBar(
                            title = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                            navigationIcon = {
                                IconButton(onClick = { sessionViewModel.touch(); onBack() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                        Divider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )
                    }
                }
            ) { padding ->
                // Scroll state for vertical scrolling
                val scrollState = rememberScrollState()
                
                // Detect scroll gestures to reset idle timer
                LaunchedEffect(scrollState.value) {
                    if (scrollState.value > 0 || scrollState.isScrollInProgress) {
                        sessionViewModel.touch()
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()  // Fill available space to enable scrolling
                        .verticalScroll(scrollState)  // Enable vertical scrolling
                        .padding(padding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // -------- Biometric card (UNCHANGED) --------
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = cardColors,
                        border = cardBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Biometric unlock",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    "Use fingerprint or face recognition to unlock your vault faster.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }

                            Switch(
                                checked = biometricsEnabled,
                                onCheckedChange = { enabled ->
                                    sessionViewModel.touch()
                                    if (enabled) {
                                        val canAuth = systemBiometricManager.canAuthenticate(
                                            BiometricManager.Authenticators.BIOMETRIC_STRONG
                                        )
                                        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                                            Toast.makeText(
                                                context,
                                                "Enable biometrics in device settings",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Switch
                                        }
                                        showVerifyDialog = true
                                    } else {
                                        biometricManager.clearBiometricKey()
                                        biometricsEnabled = false
                                    }
                                }
                            )
                        }
                    }

                    // -------- Idle Timer card --------
                    SecuritySliderCard(
                        title = "Idle auto-lock",
                        description = "Automatically lock the app after inactivity.",
                        icon = Icons.Default.Timer,
                        options = listOf("15 sec", "30 sec", "45 sec", "60 sec"),
                        enabled = settings.idleTimeoutEnabled,
                        onToggle = { newEnabled ->
                            sessionViewModel.touch()
                            securitySettingsViewModel.setIdleTimeout(
                                enabled = newEnabled,
                                timeoutMs = settings.idleTimeoutMs
                            )
                            // Restart watcher with NEW enabled state (pass current timeout)
                            onRestartIdleWatcher(newEnabled, settings.idleTimeoutMs)
                        },
                        selectedIndex = idleOptions.indexOf(settings.idleTimeoutMs),
                        onIndexChange = { index ->
                            val newTimeoutMs = idleOptions[index]
                            sessionViewModel.touch()
                            securitySettingsViewModel.setIdleTimeout(
                                enabled = settings.idleTimeoutEnabled,
                                timeoutMs = newTimeoutMs
                            )
                            // Restart watcher with NEW timeout duration (pass current enabled state)
                            onRestartIdleWatcher(settings.idleTimeoutEnabled, newTimeoutMs)
                        },
                        sessionViewModel = sessionViewModel
                    )


                    // -------- Clipboard card --------
                    SecuritySliderCard(
                        title = "Clipboard auto-clear",
                        description = "Clear copied passwords automatically.",
                        icon = Icons.Default.ContentPasteOff,
                        options = listOf("10 sec", "20 sec", "30 sec"),
                        enabled = settings.clipboardClearEnabled,
                        onToggle = {
                            sessionViewModel.touch()
                            securitySettingsViewModel.setClipboardClear(
                                enabled = it,
                                timeoutMs = settings.clipboardClearMs
                            )
                        },
                        selectedIndex = clipboardOptions.indexOf(settings.clipboardClearMs),
                        onIndexChange = { index ->
                            sessionViewModel.touch()
                            securitySettingsViewModel.setClipboardClear(
                                enabled = settings.clipboardClearEnabled,
                                timeoutMs = clipboardOptions[index]
                            )
                        },
                        sessionViewModel = sessionViewModel
                    )

                    // -------- Background lock card --------
                    SecurityToggleCard(
                        title = "Lock on background",
                        description = "Lock vault when app goes to background.",
                        icon = Icons.Default.Lock,
                        enabled = settings.lockOnBackground,
                        onToggle = {
                            sessionViewModel.touch()
                            securitySettingsViewModel.setLockOnBackground(it)
                        }
                    )

                    // -------- Appearance card (UNCHANGED) --------
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = cardColors,
                        border = cardBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (darkModeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Appearance",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    "Switch between light and dark themes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }

                            Switch(
                                checked = darkModeEnabled,
                                onCheckedChange = { sessionViewModel.touch(); onDarkModeToggle(it) }
                            )
                        }
                    }

                    // -------- Website card (UNCHANGED) --------
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = cardColors,
                        border = cardBorder,
                        onClick = {
                            sessionViewModel.touch()
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://riftlabs.in/"))
                            )
                        }
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Visit our website",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "Learn more about Rift Labs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // -------- Verify password dialog --------
            if (showVerifyDialog) {
                AlertDialog(
                    onDismissRequest = {
                        sessionViewModel.touch()
                        showVerifyDialog = false
                        currentPassword = ""
                        verifyError = null
                    },
                    title = {
                        Text("Verify master password", color = MaterialTheme.colorScheme.onSurface)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = {
                                    sessionViewModel.touch()
                                    currentPassword = it
                                    verifyError = null
                                },
                                modifier = Modifier.semantics {
                                    password()
                                },
                                label = { Text("Master password") },
                                singleLine = true,
                                visualTransformation =
                                    if (showPassword)
                                        VisualTransformation.None
                                    else
                                        PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        sessionViewModel.touch()
                                        showPassword = !showPassword
                                    }) {
                                        Icon(
                                            if (showPassword)
                                                Icons.Filled.VisibilityOff
                                            else
                                                Icons.Filled.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )

                            verifyError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            sessionViewModel.touch()
                            
                            if (currentPassword.isBlank()) {
                                verifyError = "Please enter your master password"
                                return@TextButton
                            }
                            
                            val derivedKey =
                                masterPasswordRepository.verifyPassword(currentPassword)

                            if (derivedKey == null) {
                                verifyError = "Incorrect password"
                                return@TextButton
                            }

                            val executor = ContextCompat.getMainExecutor(context)
                            val prompt = BiometricPrompt(
                                activity,
                                executor,
                                object : BiometricPrompt.AuthenticationCallback() {

                                    override fun onAuthenticationSucceeded(
                                        result: BiometricPrompt.AuthenticationResult
                                    ) {
                                        sessionViewModel.touch()
                                        biometricManager.saveDerivedKey(derivedKey)
                                        biometricsEnabled = true
                                        showVerifyDialog = false
                                        currentPassword = ""
                                        activeBiometricPrompt = null
                                    }

                                    override fun onAuthenticationError(
                                        errorCode: Int,
                                        errString: CharSequence
                                    ) {
                                        sessionViewModel.touch()
                                        activeBiometricPrompt = null

                                        if (
                                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                            errorCode == BiometricPrompt.ERROR_USER_CANCELED
                                        ) {
                                            showVerifyDialog = false
                                            currentPassword = ""
                                            verifyError = null
                                        }
                                    }
                                }
                            )

                            activeBiometricPrompt = prompt

                            prompt.authenticate(
                                BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Enable biometric unlock")
                                    .setSubtitle("Confirm with fingerprint or face")
                                    .setNegativeButtonText("Cancel")
                                    .build()
                            )
                        }) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            sessionViewModel.touch()
                            showVerifyDialog = false
                            currentPassword = ""
                            verifyError = null
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SecurityToggleCard(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(
            alpha = if (isDark) 0.88f else 0.96f
        )
    )
    val cardBorder =
        if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun SecuritySliderCard(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    options: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    sessionViewModel: SessionViewModel
) {
    val alpha = if (enabled) 1f else 0.4f
    val interactionSource = remember { MutableInteractionSource() }
    val latestSessionVm by rememberUpdatedState(sessionViewModel)

    val safeIndex = selectedIndex.coerceIn(0, options.lastIndex)

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                latestSessionVm.touch()
            }
        }
    }

    SecurityToggleCard(
        title = title,
        description = description,
        icon = icon,
        enabled = enabled,
        onToggle = onToggle
    )

    Column(
        modifier = Modifier
            .padding(start = 48.dp, end = 24.dp, bottom = 8.dp)
            .alpha(alpha)
    ) {
        Slider(
            value = safeIndex.toFloat(),
            onValueChange = { onIndexChange(it.toInt()) },
            valueRange = 0f..options.lastIndex.toFloat(),
            steps = options.size - 2,
            enabled = enabled,
            interactionSource = interactionSource
        )

        Text(
            options[safeIndex],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

