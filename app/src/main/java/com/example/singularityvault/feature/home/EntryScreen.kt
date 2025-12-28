package com.riftlabs.singularityvault.feature.home

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riftlabs.singularityvault.data.VaultEntry
import com.riftlabs.singularityvault.feature.auth.SessionViewModel
import com.riftlabs.singularityvault.ui.theme.GradientBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    vaultEntry: VaultEntry,
    onEditComplete: (VaultEntry) -> Unit,
    onBack: () -> Unit,
    securitySettingsViewModel: SecuritySettingsViewModel,
    sessionViewModel: SessionViewModel
) {
    val settings by securitySettingsViewModel.settings.collectAsState()
    GradientBackground {
        var isEditing by remember { mutableStateOf(false) }

        var editedService by remember { mutableStateOf(vaultEntry.serviceName) }
        var editedUsername by remember { mutableStateOf(vaultEntry.username) }
        var editedPassword by remember { mutableStateOf(vaultEntry.password) }
        var editedNote by remember { mutableStateOf(vaultEntry.notes ?: "") }

        var isEditPasswordVisible by remember { mutableStateOf(false) }
        var showWeakPasswordInfo by remember { mutableStateOf(false) }

//        val interactionModifier = Modifier
//            .fillMaxSize()
//            .pointerInput(Unit) {
//                while (true) {
//                    awaitPointerEventScope {
//                        awaitPointerEvent()
//                        sessionViewModel.touch()
//                    }
//                }
//            }

        val strengthResult = remember(editedPassword) {
            checkPasswordStrength(editedPassword)
        }

        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        val cardColors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(
                alpha = if (isDark) 0.88f else 0.96f
            )
        )

        val cardBorder =
            if (isDark)
                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            else null

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Column {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = if (isEditing) "Edit entry" else "Entry details",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    sessionViewModel.touch()
                                    onBack()
                                }) {
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
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = cardColors,
                        border = cardBorder,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDark) 8.dp else 6.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {

                            // 🔹 Service name (fixed dark mode color)
                            Text(
                                text = editedService.ifBlank { "Unnamed service" },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (!isEditing) {
                                InfoFieldSecure("Username / Email", editedUsername, false, settings, securitySettingsViewModel, sessionViewModel)
                                InfoFieldSecure("Password", editedPassword, true, settings, securitySettingsViewModel, sessionViewModel)

                                if (editedNote.isNotBlank()) {
                                    InfoFieldScrollable("Notes", editedNote, sessionViewModel)
                                }
                            } else {
                                OutlinedTextField(
                                    value = editedService,
                                    onValueChange = {
                                        editedService = it
                                        sessionViewModel.touch()
                                    },
                                    label = { Text("Service name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = editedUsername,
                                    onValueChange = {
                                        editedUsername = it
                                        sessionViewModel.touch()
                                    },
                                    label = { Text("Username / Email") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = editedPassword,
                                    onValueChange = {
                                        editedPassword = it
                                        sessionViewModel.touch()
                                    },
                                    label = { Text("Password") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation =
                                        if (isEditPasswordVisible)
                                            VisualTransformation.None
                                        else
                                            PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            isEditPasswordVisible = !isEditPasswordVisible
                                            sessionViewModel.touch()
                                        }) {
                                            Icon(
                                                if (isEditPasswordVisible)
                                                    Icons.Default.VisibilityOff
                                                else
                                                    Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )

                                OutlinedButton(
                                    onClick = {
                                        sessionViewModel.touch()
                                        editedPassword = generateStrongPassword()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Generate strong password")
                                }

                                // Scrollable Notes Field
                                OutlinedTextField(
                                    value = editedNote,
                                    onValueChange = {
                                        editedNote = it
                                        sessionViewModel.touch()
                                    },
                                    label = { Text("Notes") },
                                    maxLines = 6,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    if (event.changes.any { it.positionChanged() }) {
                                                        sessionViewModel.touch()
                                                    }
                                                }
                                            }
                                        }
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            if (!isEditing) {
                                Button(
                                    onClick = {
                                        sessionViewModel.touch()
                                        isEditing = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Edit entry")
                                }

                                OutlinedButton(
                                    onClick = {
                                        sessionViewModel.touch()
                                        isEditing = true
                                        editedPassword = generateStrongPassword()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),   // 🔑 MATCHED HEIGHT
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Generate strong password")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            sessionViewModel.touch()
                                            onEditComplete(
                                                vaultEntry.copy(
                                                    serviceName = editedService,
                                                    username = editedUsername,
                                                    password = editedPassword,
                                                    notes = editedNote.ifBlank { null }
                                                )
                                            )
                                            isEditing = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Save")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            sessionViewModel.touch()
                                            editedService = vaultEntry.serviceName
                                            editedUsername = vaultEntry.username
                                            editedPassword = vaultEntry.password
                                            editedNote = vaultEntry.notes ?: ""
                                            isEditing = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }

                            if (!strengthResult.isStrong) {
                                TextButton(
                                    onClick = {
                                        sessionViewModel.touch()
                                        showWeakPasswordInfo = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⚠ Password is weak — tap to see why",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    if (showWeakPasswordInfo) {
                        AlertDialog(
                            onDismissRequest = {
                                sessionViewModel.touch()
                                showWeakPasswordInfo = false
                            },
                            title = {
                                Text(
                                    text = "Why this password is weak",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    strengthResult.issues.forEach {
                                        Text(
                                            text = "• $it",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    sessionViewModel.touch()
                                    showWeakPasswordInfo = false
                                }) {
                                    Text("Got it")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun InfoField(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InfoFieldSecure(
    label: String,
    value: String,
    isPassword: Boolean = false,
    settings: SecuritySettings,
    securitySettingsViewModel: SecuritySettingsViewModel,
    sessionViewModel: SessionViewModel
) {
    var isVisible by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboard = remember {
        context.getSystemService(ClipboardManager::class.java)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isPassword && !isVisible -> "••••••••"
                    else -> value.ifBlank { "—" }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isPassword) {
                IconButton(onClick = {
                    isVisible = !isVisible
                    sessionViewModel.touch()
                }) {
                    Icon(
                        imageVector = if (isVisible)
                            Icons.Default.VisibilityOff
                        else
                            Icons.Default.Visibility,
                        contentDescription = if (isVisible) "Hide" else "Show",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = {
                    if (value.isBlank() || clipboard == null) return@IconButton

                    securitySettingsViewModel.onClipboardCopied(
                        clipboard = clipboard,
                        label = label,
                        value = value
                    )
                    sessionViewModel.touch()

                    copied = true

                    Toast.makeText(
                        context,
                        if (settings.clipboardClearEnabled)
                            "Copied. Clears in ${settings.clipboardClearMs / 1000}s"
                        else
                            "Copied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Icon(
                    imageVector = if (copied)
                        Icons.Default.Check
                    else
                        Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = if (copied)
                        Color(0xFF4CAF50)
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // UI-only feedback reset
    LaunchedEffect(copied) {
        if (copied) {
            delay(1200L)
            copied = false
        }
    }
}

@Composable
private fun InfoFieldScrollable(
    label: String,
    value: String,
    sessionViewModel: SessionViewModel
) {
    val scrollState = rememberScrollState()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                )
                .padding(12.dp)
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.positionChanged() }) {
                                sessionViewModel.touch()
                            }
                        }
                    }
                }
        ) {
            Text(
                text = value.ifBlank { "—" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

