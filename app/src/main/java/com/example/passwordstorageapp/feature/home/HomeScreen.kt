package com.example.passwordstorageapp.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.passwordstorageapp.data.VaultEntry
import com.example.passwordstorageapp.feature.auth.SessionViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onIdleTimeout: () -> Unit,
    sessionViewModel: SessionViewModel,
    vaultViewModel: VaultViewModel
) {
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    fun touch() {
        lastInteractionTime = System.currentTimeMillis()
    }

    val entries by vaultViewModel.entries.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var serviceName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nano Vault") },
                actions = {
                    IconButton(onClick = {
                        touch()
                        showAddDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add entry"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (entries.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No entries yet")
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + in the top bar to add one")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        VaultEntryCard(
                            entry = entry,
                            onClick = { touch() }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = {
                    touch()
                    showAddDialog = false
                },
                title = { Text("Add entry") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = serviceName,
                            onValueChange = {
                                serviceName = it
                                touch()
                            },
                            label = { Text("Service name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                touch()
                            },
                            label = { Text("Username / email") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                touch()
                            },
                            label = { Text("Password") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = {
                                notes = it
                                touch()
                            },
                            label = { Text("Notes (optional)") },
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            touch()
                            if (serviceName.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                vaultViewModel.addEntry(
                                    service = serviceName.trim(),
                                    username = username.trim(),
                                    password = password.trim(),
                                    notes = notes.trim().ifBlank { null }
                                )
                                // clear fields
                                serviceName = ""
                                username = ""
                                password = ""
                                notes = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            touch()
                            showAddDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Idle timeout watcher
    LaunchedEffect(Unit) {
        val timeoutMs = 15_000L
        while (true) {
            delay(3_000L)
            val now = System.currentTimeMillis()
            val idleTime = now - lastInteractionTime
            if (idleTime >= timeoutMs) {
                onIdleTimeout()
                break
            }
        }
    }
}

@Composable
private fun VaultEntryCard(
    entry: VaultEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = entry.serviceName, style = MaterialTheme.typography.titleMedium)
            Text(text = entry.username, style = MaterialTheme.typography.bodyMedium)
            if (!entry.notes.isNullOrBlank()) {
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
