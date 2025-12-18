package com.example.passwordstorageapp.feature.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.passwordstorageapp.feature.auth.BiometricKeyStoreManager
import com.example.passwordstorageapp.feature.auth.MasterPasswordRepository
import com.example.passwordstorageapp.ui.theme.GradientBackground
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    masterPasswordRepository: MasterPasswordRepository,
    darkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onIdleTimeout: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val biometricManager = remember { BiometricKeyStoreManager(context) }
    val systemBiometricManager = BiometricManager.from(context)

    var biometricsEnabled by remember {
        mutableStateOf(biometricManager.loadDerivedKey() != null)
    }

    // ---------------- Idle timer ----------------
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    fun touch() { lastInteractionTime = System.currentTimeMillis() }

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

    // ---------------- Biometric flow ----------------
    var showVerifyDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }

    var activeBiometricPrompt by remember { mutableStateOf<BiometricPrompt?>(null) }

    // ---------------- Styling ----------------
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
        Box(modifier = interactionModifier) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Column {
                        CenterAlignedTopAppBar(
                            title = {
                                Text("Settings", color = MaterialTheme.colorScheme.onSurface)
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    touch()
                                    onBack()
                                }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // -------- Biometric card --------
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
                                        Icons.Filled.Fingerprint,
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
                                    touch()

                                    if (enabled) {
                                        val canAuth = systemBiometricManager.canAuthenticate(
                                            BiometricManager.Authenticators.BIOMETRIC_STRONG
                                        )

                                        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Enable fingerprint or face unlock in device settings",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
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

                    // -------- Appearance card --------
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
                                        if (darkModeEnabled)
                                            Icons.Filled.DarkMode
                                        else
                                            Icons.Filled.LightMode,
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
                                onCheckedChange = {
                                    touch()
                                    onDarkModeToggle(it)
                                }
                            )
                        }
                    }

                    // -------- Website card --------
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = cardColors,
                        border = cardBorder,
                        onClick = {
                            touch()
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://riftlabs.in/")
                                )
                            )
                        }
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Language,
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
                        touch()
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
                                    touch()
                                    currentPassword = it
                                    verifyError = null
                                },
                                label = { Text("Master password") },
                                singleLine = true,
                                visualTransformation =
                                    if (showPassword)
                                        VisualTransformation.None
                                    else
                                        PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        touch()
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
                            touch()
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
                                        touch()
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
                                        touch()
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
                            touch()
                            showVerifyDialog = false
                            currentPassword = ""
                            verifyError = null
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // -------- Idle timeout --------
            LaunchedEffect(showVerifyDialog) {
                val timeoutMs = 15_000L
                while (true) {
                    delay(3_000L)
                    if (System.currentTimeMillis() - lastInteractionTime >= timeoutMs) {

                        activeBiometricPrompt?.cancelAuthentication()
                        activeBiometricPrompt = null

                        showVerifyDialog = false
                        currentPassword = ""
                        verifyError = null

                        onIdleTimeout()
                        return@LaunchedEffect
                    }
                }
            }
        }
    }
}
