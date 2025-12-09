package com.example.passwordstorageapp.feature.auth

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun UnlockScreen(
    masterPasswordRepository: MasterPasswordRepository,
    onUnlockSuccess: (ByteArray) -> Unit = {}
){
    var password by remember{ mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
        ?: throw IllegalStateException("Activity must be a FragmentActivity")
    val biometricKeyStoreManager = BiometricKeyStoreManager(context)
    val hasBiometric = remember {
        biometricKeyStoreManager.loadDerivedKey() != null
    }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ){
        Text("Enter master password")
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { newPassword -> password = newPassword
                            errorMessage = null},
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val check = masterPasswordRepository.verifyPassword(password)
            if(check != null){
                errorMessage = null
                biometricKeyStoreManager.saveDerivedKey(check)
                onUnlockSuccess(check)
            }
            else{
                errorMessage = "Wrong master password"
            }
        }){
            Text("Verify")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if(hasBiometric){
            Button(onClick = {
                launchBiometricPrompt(
                    activity,
                    biometricKeyStoreManager,
                    onSuccess = { derivedKey ->
                        onUnlockSuccess(derivedKey)
                    },
                    onError = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }){
                Text("Unlock with biometric")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        errorMessage?.let{
            Spacer(modifier = Modifier.height(8.dp))
            Text(it)
        }
    }
}

fun launchBiometricPrompt(
    activity: FragmentActivity,
    biometricKeyStoreManager: BiometricKeyStoreManager,
    onSuccess : (ByteArray) -> Unit,
    onError : (String) -> Unit
    ){
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val derivedKey = biometricKeyStoreManager.loadDerivedKey()
                if(derivedKey != null){
                    onSuccess(derivedKey)
                }
                else{
                    onError("No stored key")
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Zero Trace")
        .setSubtitle("Use biometrics")
        .setNegativeButtonText("Use master password")
        .build()

    biometricPrompt.authenticate(promptInfo)
}

