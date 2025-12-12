package com.example.passwordstorageapp.feature.home

import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.example.passwordstorageapp.data.VaultEntry
import com.example.passwordstorageapp.feature.auth.MasterPasswordRepository
import com.example.passwordstorageapp.feature.auth.SessionViewModel
import com.example.passwordstorageapp.feature.auth.SetupMasterPasswordScreen
import com.example.passwordstorageapp.feature.auth.UnlockScreen
import com.example.passwordstorageapp.feature.auth.BiometricKeyStoreManager

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
    val context = LocalContext.current
    val biometricKeyStoreManager = remember { BiometricKeyStoreManager(context) }

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

    val startDestination =
        if (!masterPasswordRepository.isMasterPasswordSet()) "setup"
        else if (!sessionViewModel.isUnlocked) "unlock"
        else "home"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("setup") {
            SetupMasterPasswordScreen(masterPasswordRepository = masterPasswordRepository, isChange = false, onSetupComplete = {
                navController.navigate("unlock") { popUpTo("setup") { inclusive = true } }
            })
        }

        composable("unlock") {
            UnlockScreen(masterPasswordRepository = masterPasswordRepository, onUnlockSuccess = { derivedKey ->
                sessionViewModel.vaultKey = derivedKey
                sessionViewModel.markUnlocked()
                vaultViewModel.setKey(derivedKey)

                navController.navigate("home") { popUpTo("unlock") { inclusive = true } }
            })
        }

        composable("home") {
            HomeScreen(onIdleTimeout = {
                sessionViewModel.markLocked()
                sessionViewModel.vaultKey = null
                vaultViewModel.clearKey()
                navController.navigate("unlock") { popUpTo("home") { inclusive = true } }
            }, onEntryClick = { newEntry -> currentEntry = newEntry; navController.navigate("entry_screen") }, onSettingsClick = { navController.navigate("setting") }, vaultViewModel = vaultViewModel)
        }

        composable("entry_screen") {
            val entry = currentEntry
            if (entry != null) {
                EntryScreen(vaultEntry = entry, onEditComplete = { editedEntry -> vaultViewModel.updateEntry(editedEntry); currentEntry = editedEntry }, onBack = { navController.popBackStack() }, onIdleTimeout = {
                    sessionViewModel.markLocked()
                    sessionViewModel.vaultKey = null
                    vaultViewModel.clearKey()
                    navController.navigate("unlock") { popUpTo("home") { inclusive = true } }
                })
            } else {
                navController.popBackStack()
            }
        }

        composable("setting") {
            SettingScreen(masterPasswordRepository = masterPasswordRepository, darkModeEnabled = darkModeEnabled, onDarkModeToggle = onDarkModeToggle, onBack = { navController.popBackStack() }, onIdleTimeout = {
                sessionViewModel.markLocked()
                sessionViewModel.vaultKey = null
                vaultViewModel.clearKey()
                navController.navigate("unlock") { popUpTo("home") { inclusive = true } }
            }, onNavigateToSetupChangeMode = { verifiedDerivedKey ->
                // stash old derived key into session so setup_change can use it for re-encrypt
                sessionViewModel.vaultKey = verifiedDerivedKey
                navController.navigate("setup_change")
            })
        }

        composable("setup_change") {
            SetupMasterPasswordScreen(masterPasswordRepository = masterPasswordRepository, isChange = true, onSetupComplete = { newDerivedKey ->
                val oldDerived = sessionViewModel.vaultKey
                if (oldDerived != null && oldDerived.isNotEmpty()) {
                    vaultViewModel.reencryptAll(oldDerived, newDerivedKey) {
                        // cleanup biometric artifacts if no new blob was created
                        try {
                            val blob = biometricKeyStoreManager.loadEncryptedBlob()
                            if (blob == null) {
                                try { biometricKeyStoreManager.clearStoredDerivedKey() } catch (_: Exception) {}
                                try { biometricKeyStoreManager.deleteKeystoreKey() } catch (_: Exception) {}
                            }
                        } catch (_: Exception) {}

                        try { oldDerived.fill(0) } catch (_: Exception) {}
                        try { newDerivedKey.fill(0) } catch (_: Exception) {}
                        sessionViewModel.markLocked()
                        sessionViewModel.vaultKey = null
                        vaultViewModel.clearKey()
                        navController.navigate("unlock") { popUpTo("setup_change") { inclusive = true } }
                    }
                } else {
                    // no old key cached — be conservative and clear biometric if no blob found
                    try {
                        val blob = biometricKeyStoreManager.loadEncryptedBlob()
                        if (blob == null) {
                            try { biometricKeyStoreManager.clearStoredDerivedKey() } catch (_: Exception) {}
                            try { biometricKeyStoreManager.deleteKeystoreKey() } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}

                    try { newDerivedKey.fill(0) } catch (_: Exception) {}
                    sessionViewModel.markLocked()
                    sessionViewModel.vaultKey = null
                    vaultViewModel.clearKey()
                    navController.navigate("unlock") { popUpTo("setup_change") { inclusive = true } }
                }
            })
        }
    }
}
