package com.example.passwordstorageapp.feature.home

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passwordstorageapp.data.VaultEntry
import com.example.passwordstorageapp.ui.theme.GradientBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    vaultEntry: VaultEntry,
    onEditComplete: (VaultEntry) -> Unit,
    onBack: () -> Unit,
    onIdleTimeout: () -> Unit
) {
    GradientBackground {
        var isEditing by remember { mutableStateOf(false) }

        var editedService by remember { mutableStateOf(vaultEntry.serviceName) }
        var editedUsername by remember { mutableStateOf(vaultEntry.username) }
        var editedPassword by remember { mutableStateOf(vaultEntry.password) }
        var editedNote by remember { mutableStateOf(vaultEntry.notes ?: "") }

        var isEditPasswordVisible by remember { mutableStateOf(false) }
        var showWeakPasswordInfo by remember { mutableStateOf(false) }

        // ---------- Idle timer ----------
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

        Box(modifier = interactionModifier) {
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
                                    touch()
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
                                InfoFieldSecure("Username / Email", editedUsername, false)
                                InfoFieldSecure("Password", editedPassword, true)

                                if (editedNote.isNotBlank()) {
                                    InfoField("Notes", editedNote)
                                }
                            } else {
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
                                    visualTransformation =
                                        if (isEditPasswordVisible)
                                            VisualTransformation.None
                                        else
                                            PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            isEditPasswordVisible = !isEditPasswordVisible
                                            touch()
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
                                        touch()
                                        editedPassword = generateStrongPassword()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Generate strong password")
                                }

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

                            Spacer(Modifier.height(8.dp))

                            if (!isEditing) {
                                Button(
                                    onClick = {
                                        touch()
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
                                        touch()
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
                                            touch()
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
                                            touch()
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
                                        touch()
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
                                touch()
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
                                    touch()
                                    showWeakPasswordInfo = false
                                }) {
                                    Text("Got it")
                                }
                            }
                        )
                    }
                }
            }

            // ---------- Idle timeout ----------
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
