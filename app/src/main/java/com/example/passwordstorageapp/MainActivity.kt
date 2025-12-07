package com.example.passwordstorageapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.passwordstorageapp.data.AppDatabase
import com.example.passwordstorageapp.data.VaultRepository
import com.example.passwordstorageapp.feature.auth.MasterPasswordRepository
import com.example.passwordstorageapp.feature.auth.SessionViewModel
import com.example.passwordstorageapp.feature.home.AppContent
import com.example.passwordstorageapp.feature.home.VaultViewModel
import com.example.passwordstorageapp.feature.home.VaultViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var masterPasswordRepository: MasterPasswordRepository

    // SessionViewModel: no factory needed
    private val sessionViewModel: SessionViewModel by viewModels()

    // VaultViewModel: build DB + DAO + Repo INSIDE the factory
    private val vaultViewModel: VaultViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.vaultDao()
        val repo = VaultRepository(dao)
        VaultViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        masterPasswordRepository = MasterPasswordRepository(applicationContext)

        // Lock on app background
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    sessionViewModel.markLocked()
                    sessionViewModel.vaultKey = null
                }
            }
        )

        setContent {
            AppContent(
                masterPasswordRepository = masterPasswordRepository,
                sessionViewModel = sessionViewModel,
                vaultViewModel = vaultViewModel
            )
        }
    }
}
