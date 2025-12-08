package com.example.passwordstorageapp.feature.auth

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

@Composable
fun SetupMasterPasswordScreen(
    masterPasswordRepository: MasterPasswordRepository,
    onSetupComplete: () -> Unit = {}
){
    var password by remember{ mutableStateOf("") }
    var confirmPassword by remember{ mutableStateOf("") }
    var errorMessage by remember{ mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val masterPasswordRepo = remember { MasterPasswordRepository(context) }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ){
        Text("Master password setup")
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enter master password")
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { newPassword : String -> password = newPassword },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Confirm master password")
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = confirmPassword,
            onValueChange = { newConfirmedPassword -> confirmPassword = newConfirmedPassword },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        errorMessage?.let{
            Spacer(modifier = Modifier.height(8.dp))
            Text(it)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            when{
                password != confirmPassword -> {
                    errorMessage = "Passwords do not match"
                }
                !isValidPassword(password) -> {
                    errorMessage = "Password is too weak"
                }
                else -> {
                    masterPasswordRepo.setMasterPassword(password)
                    errorMessage = null
                    onSetupComplete()
                }
            }
        }){
            Text("Confirm")
        }
        Spacer(modifier = Modifier.height(30.dp))
        Text("Warning\n" +
                "Your master password cannot be recovered.\n" +
                "If you lose it, all stored data will be permanently inaccessible.")
    }
}

fun isValidPassword(password: String): Boolean {
    val passwordRegex = Regex("^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#\$%^&+=!]).{6,}$")
    return passwordRegex.matches(password)
}