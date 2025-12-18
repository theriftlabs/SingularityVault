package com.example.passwordstorageapp.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passwordstorageapp.R
import com.example.passwordstorageapp.data.VaultEntry
import com.example.passwordstorageapp.ui.theme.GradientBackground
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onIdleTimeout: () -> Unit,
    onEntryClick: (VaultEntry) -> Unit,
    onSettingsClick: () -> Unit,
    vaultViewModel: VaultViewModel
) {
    // ---------- Idle timer ----------
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    fun touch() { lastInteractionTime = System.currentTimeMillis() }

    val interactionModifier = Modifier.pointerInput(Unit) {
        while (true) {
            awaitPointerEventScope {
                awaitPointerEvent()
                touch()
            }
        }
    }

    GradientBackground(modifier = interactionModifier) {

        val entries: List<VaultEntry>? by vaultViewModel.entries.collectAsState()

        var showAddDialog by remember { mutableStateOf(false) }
        var serviceName by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        // 🔥 Delete confirmation state
        var entryPendingDelete by remember { mutableStateOf<VaultEntry?>(null) }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            val isDark =
                                MaterialTheme.colorScheme.background.luminance() < 0.5f
                            val logoRes =
                                if (isDark)
                                    R.drawable.singularity_vault_logo_dark_new2
                                else
                                    R.drawable.singularity_vault_logo_light_new2

                            Image(
                                painter = painterResource(logoRes),
                                contentDescription = "App logo",
                                modifier = Modifier.size(90.dp),
                                contentScale = ContentScale.Fit
                            )
                        },
                        actions = {
                            IconButton(onClick = {
                                touch()
                                showAddDialog = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add entry")
                            }

                            IconButton(onClick = {
                                touch()
                                onSettingsClick()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )

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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when {
                    entries == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    entries!!.isEmpty() -> {
                        EmptyState {
                            touch()
                            showAddDialog = true
                        }
                    }

                    else -> {
                        EntryList(
                            entries = entries!!,
                            onEntryClick = {
                                touch()
                                onEntryClick(it)
                            },
                            onDeleteClick = {
                                touch()
                                entryPendingDelete = it
                            }
                        )
                    }
                }

                if (showAddDialog) {
                    AddEntryDialog(
                        serviceName = serviceName,
                        username = username,
                        password = password,
                        notes = notes,
                        onServiceNameChange = { serviceName = it },
                        onUsernameChange = { username = it },
                        onPasswordChange = { password = it },
                        onNotesChange = { notes = it },
                        onDismiss = {
                            touch()
                            showAddDialog = false
                        },
                        onConfirm = {
                            touch()
                            if (
                                serviceName.isNotBlank() &&
                                username.isNotBlank() &&
                                password.isNotBlank()
                            ) {
                                vaultViewModel.addEntry(
                                    service = serviceName.trim(),
                                    username = username.trim(),
                                    password = password.trim(),
                                    notes = notes.trim().ifBlank { null }
                                )
                                serviceName = ""
                                username = ""
                                password = ""
                                notes = ""
                                showAddDialog = false
                            }
                        },
                        onUserInteraction = { touch() }
                    )
                }
            }

            // ---------- Delete confirmation dialog ----------
            entryPendingDelete?.let { entry ->
                AlertDialog(
                    onDismissRequest = {
                        touch()
                        entryPendingDelete = null
                    },
                    title = {
                        Text(
                            text = "Delete entry?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                            Text(
                                text = "This action cannot be undone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )

                            Text(
                                text = entry.serviceName.ifBlank { "Unnamed service" },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = entry.username,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                touch()
                                vaultViewModel.deleteEntry(entry)
                                entryPendingDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.Black   // 🔑 FORCE BLACK TEXT
                            )
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                touch()
                                entryPendingDelete = null
                            }
                        ) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
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
private fun EmptyState(
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No entries yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Start by adding your first password, note, or account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            modifier = Modifier.height(44.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text("Add entry")
        }
    }
}

@Composable
private fun EntryList(
    entries: List<VaultEntry>,
    onEntryClick: (VaultEntry) -> Unit,
    onDeleteClick: (VaultEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = entries,
            key = { entry -> entry.id }   // keep key for stable list, adjust if id field differs
        ) { entry ->
            VaultEntryCard(
                entry = entry,
                onClick = { onEntryClick(entry) },
                onDeleteClick = { onDeleteClick(entry) }
            )
        }
    }
}

@Composable
private fun AddEntryDialog(
    serviceName: String,
    username: String,
    password: String,
    notes: String,
    onServiceNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUserInteraction: () -> Unit
) {
    val strengthResult = remember(password) {
        checkPasswordStrength(password)
    }

    var suggestedPassword by remember { mutableStateOf("") }
    var showSuggestion by remember { mutableStateOf(false) }

    // 🔑 NEW: validation message
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            onUserInteraction()
            validationError = null
            onDismiss()
        },
        title = {
            Text(
                text = "Add entry",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .pointerInput(Unit) {
                        while (true) {
                            awaitPointerEventScope {
                                awaitPointerEvent()
                                onUserInteraction()
                            }
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                OutlinedTextField(
                    value = serviceName,
                    onValueChange = {
                        validationError = null
                        onUserInteraction()
                        onServiceNameChange(it)
                    },
                    label = { Text("Service name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        validationError = null
                        onUserInteraction()
                        onUsernameChange(it)
                    },
                    label = { Text("Username / email *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        validationError = null
                        onUserInteraction()
                        onPasswordChange(it)
                        showSuggestion = false
                    },
                    label = { Text("Password *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (password.isNotBlank() && !strengthResult.isStrong) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Improve password strength:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        strengthResult.issues.forEach {
                            Text(
                                "• $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onUserInteraction()
                        suggestedPassword = generateStrongPassword()
                        showSuggestion = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate strong password")
                }

                if (showSuggestion) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Suggested password",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(suggestedPassword)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onUserInteraction()
                                        onPasswordChange(suggestedPassword)
                                        showSuggestion = false
                                    }
                                ) {
                                    Text("Use")
                                }

                                TextButton(
                                    onClick = {
                                        onUserInteraction()
                                        suggestedPassword = generateStrongPassword()
                                    }
                                ) {
                                    Text("Regenerate")
                                }
                            }
                        }
                    }
                }

                Divider()

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        onUserInteraction()
                        onNotesChange(it)
                    },
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // 🔥 Validation message (inline, no toast nonsense)
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUserInteraction()

                if (
                    serviceName.isBlank() ||
                    username.isBlank() ||
                    password.isBlank()
                ) {
                    validationError = "Service name, username, and password are required."
                    return@Button
                }

                validationError = null
                onConfirm()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onUserInteraction()
                validationError = null
                onServiceNameChange("")
                onUsernameChange("")
                onPasswordChange("")
                onNotesChange("")
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun VaultEntryCard(
    entry: VaultEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val cardColors = CardDefaults.cardColors(
        containerColor = if (isDark) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        }
    )

    val cardElevation = if (isDark) 6.dp else 4.dp

    val cardBorder = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.onSurface.copy(
            alpha = if (isDark) 0.12f else 0.20f
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(
                modifier = Modifier.weight(1f), // 🔑 CONSTRAIN TEXT AREA
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f) // 🔑 THIS IS THE ACTUAL FIX
                ) {
                    Text(
                        text = entry.serviceName.ifBlank { "Unnamed service" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = entry.username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

