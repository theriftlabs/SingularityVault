package com.riftlabs.singularityvault.feature.home

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riftlabs.singularityvault.data.VaultEntry
import com.riftlabs.singularityvault.ui.theme.GradientBackground
import kotlinx.coroutines.delay
import com.riftlabs.singularityvault.R
import com.riftlabs.singularityvault.feature.auth.SessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEntryClick: (VaultEntry) -> Unit,
    onSettingsClick: () -> Unit,
    vaultViewModel: VaultViewModel,
    securitySettingsViewModel: SecuritySettingsViewModel,
    sessionViewModel: SessionViewModel
) {

    val settings by securitySettingsViewModel.settings.collectAsState()

    GradientBackground() {

        val entries: List<VaultEntry>? by vaultViewModel.entries.collectAsState()

        var showAddDialog by remember { mutableStateOf(false) }
        var serviceName by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        // 🔥 Delete confirmation state
        var entryPendingDelete by remember { mutableStateOf<VaultEntry?>(null) }
        
        // 🎬 Animation state for deletion
        var deletingItems by remember { mutableStateOf<Set<Int>>(emptySet()) }
        val coroutineScope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize()) {
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
                                    sessionViewModel.touch()
                                    showAddDialog = true
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add entry")
                                }

                                IconButton(onClick = {
                                    sessionViewModel.touch()
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
                                sessionViewModel.touch()
                                showAddDialog = true
                            }
                        }

                        else -> {
                            EntryList(
                                entries = entries!!,
                                deletingItems = deletingItems,
                                onEntryClick = {
                                    sessionViewModel.touch()
                                    onEntryClick(it)
                                },
                                onDeleteClick = {
                                    sessionViewModel.touch()
                                    entryPendingDelete = it
                                },
                                sessionViewModel
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
                                sessionViewModel.touch()
                                showAddDialog = false
                            },
                            onConfirm = {
                                sessionViewModel.touch()
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
                            onUserInteraction = { sessionViewModel.touch() }
                        )
                    }
                }

                // ---------- Delete confirmation dialog ----------
                entryPendingDelete?.let { entry ->
                    AlertDialog(
                        onDismissRequest = {
                            sessionViewModel.touch()
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
                                    sessionViewModel.touch()
                                    // Trigger animation first
                                    deletingItems = deletingItems + entry.id
                                    entryPendingDelete = null
                                    
                                    // Delete after animation completes
                                    coroutineScope.launch {
                                        delay(300)
                                        vaultViewModel.deleteEntry(entry)
                                        deletingItems = deletingItems - entry.id
                                    }
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
                                    sessionViewModel.touch()
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
    deletingItems: Set<Int>,
    onEntryClick: (VaultEntry) -> Unit,
    onDeleteClick: (VaultEntry) -> Unit,
    sessionViewModel : SessionViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        // Detect any pointer movement (scroll, drag, swipe)
                        if (event.changes.any { it.positionChanged() }) {
                            sessionViewModel.touch()
                        }
                    }
                }
            },
            verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = entries,
            key = { entry -> entry.id }   // keep key for stable list, adjust if id field differs
        ) { entry ->
            val isDeleting = deletingItems.contains(entry.id)
            val scale = remember { androidx.compose.animation.core.Animatable(if (isDeleting) 1f else 0.8f) }
            val alpha = remember { androidx.compose.animation.core.Animatable(if (isDeleting) 1f else 0f) }
            
            LaunchedEffect(Unit) {
                if (!isDeleting) {
                    launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    launch {
                        alpha.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            }
            
            LaunchedEffect(isDeleting) {
                if (isDeleting) {
                    launch {
                        scale.animateTo(
                            targetValue = 0.8f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    launch {
                        alpha.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            }
            
            VaultEntryCard(
                entry = entry,
                onClick = { onEntryClick(entry) },
                onDeleteClick = { onDeleteClick(entry) },
                modifier = Modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
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
            onServiceNameChange("")
            onUsernameChange("")
            onPasswordChange("")
            onNotesChange("")
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
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier.fillMaxWidth(),
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

