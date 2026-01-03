package com.riftlabs.singularityvault.feature.home

import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riftlabs.singularityvault.data.VaultEntry
import com.riftlabs.singularityvault.feature.auth.MasterPasswordRepository
import com.riftlabs.singularityvault.feature.auth.SessionViewModel
import com.riftlabs.singularityvault.feature.auth.SetupMasterPasswordScreen
import com.riftlabs.singularityvault.feature.auth.UnlockScreen
import androidx.compose.ui.platform.LocalContext
import com.riftlabs.singularityvault.feature.home.OnboardingScreen
import com.riftlabs.singularityvault.feature.home.isOnboardingDone
import com.riftlabs.singularityvault.feature.home.setOnboardingDone
import kotlinx.coroutines.delay

@Composable
fun AppContent(
    masterPasswordRepository: MasterPasswordRepository,
    sessionViewModel: SessionViewModel,
    vaultViewModel: VaultViewModel,
    securitySettingsViewModel: SecuritySettingsViewModel,
    darkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    
    var currentEntry by remember { mutableStateOf<VaultEntry?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val onboardingDone = isOnboardingDone(context, masterPasswordRepository)
    val settings by securitySettingsViewModel.settings.collectAsState()
    
    // Track if we were locked due to background (to force navigation on resume)
    var lockedDueToBackground by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, sessionViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                // Check if locked OR if we have the background lock flag set
                if (
                    masterPasswordRepository.isMasterPasswordSet() &&
                    (!sessionViewModel.isUnlocked || lockedDueToBackground)
                ) {
                    // Show toast if locked due to background
                    if (lockedDueToBackground) {
                        Toast.makeText(
                            context,
                            "Vault locked on background",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    lockedDueToBackground = false  // Reset flag
                    navController.navigate("unlock") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                // Set flag when going to background if lock-on-background is enabled
                if (settings.lockOnBackground && sessionViewModel.isUnlocked) {
                    lockedDueToBackground = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Calculate startDestination ONLY ONCE on initial composition
    // Don't use reactive keys - navigation is handled explicitly via navigate() calls
    // Theme changes should NOT recalculate this value
    val startDestination = remember {
        when {
            !onboardingDone -> "onboarding"
            !masterPasswordRepository.isMasterPasswordSet() -> "setup"
            !sessionViewModel.isUnlocked -> "unlock"
            else -> "home"
        }
    }
    
    // Track current navigation route for background lock behavior
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            sessionViewModel.updateRoute(backStackEntry.destination.route ?: "")
        }
    }

    // System now handles insets automatically with setDecorFitsSystemWindows(true)
    // No need for manual padding wrapper
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

                    // Manually start the idle watcher with current settings
                    sessionViewModel.startIdleWatcher(
                        idleTimeoutEnabled = settings.idleTimeoutEnabled,
                        idleTimeoutMs = settings.idleTimeoutMs
                    ) {
                        sessionViewModel.markLocked()
                        vaultViewModel.clearKey()
                        
                        Toast.makeText(
                            context,
                            "Vault locked due to inactivity",
                            Toast.LENGTH_SHORT
                        ).show()

                        navController.navigate("unlock") {
                            popUpTo(0) { inclusive = true }
                        }
                    }

                    navController.navigate("home") {
                        popUpTo("unlock") { inclusive = true }
                    }
                }
            )
        }

            composable("home") {
                HomeScreen(
                    onEntryClick = { newEntry ->
                        // Only navigate if still unlocked (prevent race with idle timer)
                        if (sessionViewModel.isUnlocked) {
                            currentEntry = newEntry
                            navController.navigate("entry_screen")
                        }
                    },
                    onSettingsClick = {
                        // Only navigate if still unlocked (prevent race with idle timer)
                        if (sessionViewModel.isUnlocked) {
                            navController.navigate("setting")
                        }
                    },
                    vaultViewModel = vaultViewModel,
                    securitySettingsViewModel = securitySettingsViewModel,
                    sessionViewModel = sessionViewModel
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
                        onBack = { 
                            // Only navigate if still unlocked (prevent race with idle timer)
                            if (sessionViewModel.isUnlocked) {
                                navController.popBackStack()
                            }
                        },
                        securitySettingsViewModel = securitySettingsViewModel,
                        sessionViewModel = sessionViewModel
                    )
                } else {
                    navController.popBackStack()
                }
            }

            composable("setting") {
                SettingScreen(
                    masterPasswordRepository = masterPasswordRepository,
                    securitySettingsViewModel = securitySettingsViewModel,
                    darkModeEnabled = darkModeEnabled,
                    onDarkModeToggle = onDarkModeToggle,
                    onBack = { 
                        // Only navigate if still unlocked (prevent race with idle timer)
                        if (sessionViewModel.isUnlocked) {
                            navController.popBackStack()
                        }
                    },
                    sessionViewModel = sessionViewModel,
                    vaultViewModel = vaultViewModel,
                    onRestartIdleWatcher = { enabled, timeoutMs ->
                        // Restart the watcher with NEW settings values passed directly
                        sessionViewModel.touch()
                        sessionViewModel.startIdleWatcher(
                            idleTimeoutEnabled = enabled,
                            idleTimeoutMs = timeoutMs
                        ) {
                            sessionViewModel.markLocked()
                            vaultViewModel.clearKey()
                            Toast.makeText(
                                context,
                                "Vault locked due to inactivity",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.navigate("unlock") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable("onboarding") {
                OnboardingScreen(
                    onComplete = {
                        setOnboardingDone(context)
                        navController.navigate("setup") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
        }
}
