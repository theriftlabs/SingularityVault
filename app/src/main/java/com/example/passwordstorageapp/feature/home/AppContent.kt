package com.example.passwordstorageapp.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.passwordstorageapp.data.VaultEntry
import com.example.passwordstorageapp.feature.auth.MasterPasswordRepository
import com.example.passwordstorageapp.feature.auth.SessionViewModel
import com.example.passwordstorageapp.feature.auth.SetupMasterPasswordScreen
import com.example.passwordstorageapp.feature.auth.UnlockScreen

@Composable
fun AppContent(
    masterPasswordRepository: MasterPasswordRepository,
    sessionViewModel: SessionViewModel,
    vaultViewModel: VaultViewModel,
    darkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    var currentEntry by remember { mutableStateOf<VaultEntry?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, sessionViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (masterPasswordRepository.isMasterPasswordSet() && !sessionViewModel.isUnlocked) {
                    navController.navigate("unlock") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Decide start destination directly
    val startDestination =
        if (!masterPasswordRepository.isMasterPasswordSet()) {
            "setup"
        } else if (!sessionViewModel.isUnlocked) {
            "unlock"
        } else {
            "home"
        }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("setup") {
            SetupMasterPasswordScreen(
                masterPasswordRepository = masterPasswordRepository,
                onSetupComplete = {
                    navController.navigate("unlock") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }

        composable("unlock") {
            UnlockScreen(
                masterPasswordRepository = masterPasswordRepository,
                onUnlockSuccess = { derivedKey ->
                    sessionViewModel.vaultKey = derivedKey
                    sessionViewModel.markUnlocked()
                    vaultViewModel.setKey(derivedKey)

                    navController.navigate("home") {
                        popUpTo("unlock") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onIdleTimeout = {
                    sessionViewModel.markLocked()
                    sessionViewModel.vaultKey = null
                    vaultViewModel.clearKey()

                    navController.navigate("unlock") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onEntryClick = { newEntry ->
                    currentEntry = newEntry
                    navController.navigate("entry_screen")
                },
                onSettingsClick = {
                    navController.navigate("setting")
                },
                vaultViewModel = vaultViewModel
            )
        }

        composable("entry_screen") {
            val entry = currentEntry
            if (entry != null) {
                EntryScreen(
                    vaultEntry = entry,
                    onEditComplete = { editedEntry ->
                        vaultViewModel.updateEntry(editedEntry)
                        currentEntry = editedEntry
                    },
                    onBack = { navController.popBackStack() },
                    onIdleTimeout = {
                        sessionViewModel.markLocked()
                        sessionViewModel.vaultKey = null
                        vaultViewModel.clearKey()

                        navController.navigate("unlock") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            } else {
                navController.popBackStack()
            }
        }

        composable("setting") {
            SettingScreen(
                masterPasswordRepository = masterPasswordRepository,
                darkModeEnabled = darkModeEnabled,
                onDarkModeToggle = onDarkModeToggle,
                onBack = { navController.popBackStack() },
                onIdleTimeout = {
                    sessionViewModel.markLocked()
                    sessionViewModel.vaultKey = null
                    vaultViewModel.clearKey()

                    navController.navigate("unlock") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
