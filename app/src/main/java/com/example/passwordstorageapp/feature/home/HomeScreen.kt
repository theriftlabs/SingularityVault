package com.example.passwordstorageapp.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passwordstorageapp.data.VaultEntry
import com.example.passwordstorageapp.ui.theme.GradientBackground
import kotlinx.coroutines.delay
import com.example.passwordstorageapp.R
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onIdleTimeout: () -> Unit,
    onEntryClick: (VaultEntry) -> Unit,
    onSettingsClick: () -> Unit,
    vaultViewModel: VaultViewModel
) {
    // Idle timer
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    fun touch() {
        lastInteractionTime = System.currentTimeMillis()
    }

    // Global interaction detector: any touch / scroll will call touch()
    val interactionModifier = Modifier.pointerInput(Unit) {
        while (true) {
            awaitPointerEventScope {
                awaitPointerEvent()
                touch()
            }
        }
    }

    GradientBackground(modifier = interactionModifier) {
        // 🔹 entries is now nullable
        val entries: List<VaultEntry>? by vaultViewModel.entries.collectAsState()

        // Add-entry dialog fields
        var showAddDialog by remember { mutableStateOf(false) }
        var serviceName by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

                            val logoRes = if (isDark) {
                                R.drawable.singularity_vault_logo_dark_new2
                            } else {
                                R.drawable.singularity_vault_logo_light_new2
                            }
                            Box(
                                modifier = Modifier
                                    .height(90.dp)
                                    .width(90.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = logoRes),
                                    contentDescription = "App logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    touch()
                                    showAddDialog = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add entry"
                                )
                            }

                            IconButton(
                                onClick = {
                                    touch()
                                    onSettingsClick()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
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
                    // 🔹 First load: entries == null → show subtle loading instead of "No entries"
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

                    // Real empty state
                    entries!!.isEmpty() -> {
                        EmptyState(
                            onAddClick = {
                                touch()
                                showAddDialog = true
                            }
                        )
                    }

                    // Normal list
                    else -> {
                        EntryList(
                            entries = entries!!,
                            onEntryClick = {
                                touch()
                                onEntryClick(it)
                            },
                            onDeleteClick = {
                                touch()
                                vaultViewModel.deleteEntry(it)
                            }
                        )
                    }
                }

                // Add-entry dialog
                if (showAddDialog) {
                    AddEntryDialog(
                        serviceName = serviceName,
                        username = username,
                        password = password,
                        notes = notes,
                        onServiceNameChange = { serviceName = it; touch() },
                        onUsernameChange = { username = it; touch() },
                        onPasswordChange = { password = it; touch() },
                        onNotesChange = { notes = it; touch() },
                        onDismiss = {
                            showAddDialog = false
                            touch()
                        },
                        onConfirm = {
                            touch()
                            if (serviceName.isNotBlank() &&
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
                        }
                    )
                }
            }

            // Idle timeout coroutine
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
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add entry",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = onServiceNameChange,
                    label = { Text("Service name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username / email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
    // Detect dark theme based on current background
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val cardColors = CardDefaults.cardColors(
        containerColor = if (isDark) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        } else {
            // Slightly stronger, cleaner white so it stands off the light gradient better
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        }
    )

    val cardElevation = if (isDark) 6.dp else 4.dp

    // Border in BOTH themes for clearer separation
    val cardBorder: BorderStroke =
        BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (isDark) 0.12f else 0.20f
            )
        )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
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

                    Column {
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
}
