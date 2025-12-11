package com.example.passwordstorageapp.feature.home

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passwordstorageapp.data.VaultEntry
import com.example.passwordstorageapp.ui.theme.GradientBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    vaultEntry: VaultEntry,
    onEditComplete: (VaultEntry) -> Unit,
    onBack: () -> Unit,
    onIdleTimeout: () -> Unit        // 🔹 NEW: lock from entry screen too
) {
    GradientBackground {
        var isEditing by remember { mutableStateOf(false) }

        var editedService by remember { mutableStateOf(vaultEntry.serviceName) }
        var editedUsername by remember { mutableStateOf(vaultEntry.username) }
        var editedPassword by remember { mutableStateOf(vaultEntry.password) }
        var editedNote by remember { mutableStateOf(vaultEntry.notes ?: "") }

        // Password visibility in EDIT mode
        var isEditPasswordVisible by remember { mutableStateOf(false) }

        // -------- idle timer state ----------
        var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

        fun touch() {
            lastInteractionTime = System.currentTimeMillis()
        }

        // global interaction (any touch / scroll)
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

        Box(modifier = interactionModifier) {   // 🧠 attach global touch listener here
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
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
                                    touch()
                                    onBack()
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        )
                        // bottom divider under top bar
                        Divider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
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
                                // Header: service name
                                Text(
                                    text = editedService.ifBlank { "Unnamed service" },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (!isEditing) {
                                    // DISPLAY MODE
                                    InfoFieldSecure(
                                        label = "Username / Email",
                                        value = editedUsername,
                                        isPassword = false
                                    )
                                    InfoFieldSecure(
                                        label = "Password",
                                        value = editedPassword,
                                        isPassword = true
                                    )
                                    if (editedNote.isNotBlank()) {
                                        InfoField(label = "Notes", value = editedNote)
                                    }
                                } else {
                                    // EDIT MODE
                                    OutlinedTextField(
                                        value = editedService,
                                        onValueChange = {
                                            editedService = it
                                            touch()
                                        },
                                        label = { Text("Service name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = editedUsername,
                                        onValueChange = {
                                            editedUsername = it
                                            touch()
                                        },
                                        label = { Text("Username / Email") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = editedPassword,
                                        onValueChange = {
                                            editedPassword = it
                                            touch()
                                        },
                                        label = { Text("Password") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        visualTransformation = if (isEditPasswordVisible) {
                                            VisualTransformation.None
                                        } else {
                                            PasswordVisualTransformation()
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                isEditPasswordVisible = !isEditPasswordVisible
                                                touch()
                                            }) {
                                                Icon(
                                                    imageVector = if (isEditPasswordVisible)
                                                        Icons.Default.VisibilityOff
                                                    else
                                                        Icons.Default.Visibility,
                                                    contentDescription = if (isEditPasswordVisible) {
                                                        "Hide password"
                                                    } else {
                                                        "Show password"
                                                    }
                                                )
                                            }
                                        }
                                    )

                                    OutlinedTextField(
                                        value = editedNote,
                                        onValueChange = {
                                            editedNote = it
                                            touch()
                                        },
                                        label = { Text("Notes") },
                                        maxLines = 4,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // ACTION BUTTONS
                                if (!isEditing) {
                                    Button(
                                        onClick = {
                                            touch()
                                            isEditing = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text(
                                            text = "Edit entry",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                touch()
                                                val updatedEntry = vaultEntry.copy(
                                                    serviceName = editedService,
                                                    username = editedUsername,
                                                    password = editedPassword,
                                                    notes = editedNote.ifBlank { null }
                                                )
                                                isEditing = false
                                                onEditComplete(updatedEntry)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Text(
                                                text = "Save",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                touch()
                                                // Reset back to last saved values
                                                editedService = vaultEntry.serviceName
                                                editedUsername = vaultEntry.username
                                                editedPassword = vaultEntry.password
                                                editedNote = vaultEntry.notes ?: ""
                                                isEditing = false
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Text(
                                                text = "Cancel",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Editing updates only this entry. Your data stays encrypted on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 🔒 Idle timeout coroutine
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
    isPassword: Boolean = false
) {
    var isVisible by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboard = remember {
        context.getSystemService(ClipboardManager::class.java)
    }
    val scope = rememberCoroutineScope()

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
            // Show hidden dots ONLY if it's password AND visibility is off
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

            // Show eye icon ONLY for passwords
            if (isPassword) {
                IconButton(onClick = { isVisible = !isVisible }) {
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

            // Copy button
            IconButton(
                onClick = {
                    if (value.isNotBlank() && clipboard != null) {
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(label, value)
                        )
                        copied = true
                        scope.launch {
                            delay(1200L)
                            copied = false
                            delay(8000L)
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("Cleared", "")
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = if (copied)
                        Color(0xFF4CAF50)
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
