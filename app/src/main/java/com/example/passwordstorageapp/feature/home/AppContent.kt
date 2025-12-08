package com.example.passwordstorageapp.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.passwordstorageapp.feature.auth.MasterPasswordRepository
import com.example.passwordstorageapp.feature.auth.SessionViewModel
import com.example.passwordstorageapp.feature.auth.SetupMasterPasswordScreen
import com.example.passwordstorageapp.feature.auth.UnlockScreen

@Composable
fun AppContent(masterPasswordRepository: MasterPasswordRepository, sessionViewModel : SessionViewModel, vaultViewModel: VaultViewModel){
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, sessionViewModel) {
        val observer = LifecycleEventObserver{_, event ->
            if(event == Lifecycle.Event.ON_START){
                if(masterPasswordRepository.isMasterPasswordSet() && !sessionViewModel.isUnlocked){
                    navController.navigate("unlock"){
                        popUpTo(navController.graph.startDestinationId){
                            inclusive = true
                        }
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(navController=navController, startDestination =
        if(!masterPasswordRepository.isMasterPasswordSet()){
            "setup"
        }
        else if(!sessionViewModel.isUnlocked){
            "unlock"
        }
        else{
            "home"
        }
    ){
        composable("setup"){
            SetupMasterPasswordScreen(masterPasswordRepository, onSetupComplete = { navController.navigate("unlock") })
        }
        composable("unlock"){
            UnlockScreen(masterPasswordRepository, onUnlockSuccess = { derivedKey ->
                sessionViewModel.vaultKey = derivedKey
                sessionViewModel.markUnlocked()
                navController.navigate("home"){
                    popUpTo(navController.graph.startDestinationId){
                        inclusive = true
                    }
                }
            })
        }
        composable("home"){
            HomeScreen(
                onIdleTimeout = {
                    sessionViewModel.markLocked()
                    sessionViewModel.vaultKey = null
                    navController.navigate("unlock"){
                        popUpTo(navController.graph.startDestinationId){
                            inclusive = true
                        }
                    }
                },
                sessionViewModel,
                vaultViewModel
            )
        }
    }
}