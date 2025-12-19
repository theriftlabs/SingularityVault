package com.riftlabs.singularityvault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.riftlabs.singularityvault.data.AppDatabase
import com.riftlabs.singularityvault.feature.auth.MasterPasswordRepository
import com.riftlabs.singularityvault.feature.auth.SessionViewModel
import com.riftlabs.singularityvault.feature.home.AppContent
import com.riftlabs.singularityvault.feature.home.VaultViewModel
import com.riftlabs.singularityvault.feature.home.VaultViewModelFactory
import com.riftlabs.singularityvault.ui.theme.SingularityVaultTheme
import com.riftlabs.singularityvault.ui.theme.ThemeViewModel
import com.riftlabs.singularityvault.data.VaultRepository

class MainActivity : FragmentActivity() {

    private lateinit var masterPasswordRepository: MasterPasswordRepository

    private val sessionViewModel: SessionViewModel by viewModels()

    private val vaultViewModel: VaultViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.vaultDao()
        val repo = VaultRepository(dao)
        VaultViewModelFactory(repo)
    }

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_SingularityVault)
        super.onCreate(savedInstanceState)

        masterPasswordRepository = MasterPasswordRepository(applicationContext)

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    sessionViewModel.markLocked()
                    sessionViewModel.vaultKey = null
                    vaultViewModel.clearKey()
                }
            }
        )

        setContent {
            val darkModeEnabled by themeViewModel.isDarkTheme.collectAsState(initial = true)

            SingularityVaultTheme(darkTheme = darkModeEnabled) {
                AppContent(
                    masterPasswordRepository = masterPasswordRepository,
                    sessionViewModel = sessionViewModel,
                    vaultViewModel = vaultViewModel,
                    darkModeEnabled = darkModeEnabled,
                    onDarkModeToggle = { enabled ->
                        themeViewModel.setDarkTheme(enabled)
                    }
                )
            }
        }
    }
}
