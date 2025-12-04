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
fun UnlockScreen(
    onUnlockSuccess: () -> Unit = {}
){
    var password by remember{ mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val masterPasswordRepo = remember { MasterPasswordRepository(context) }

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
            val check = masterPasswordRepo.verifyPassword(password)
            if(check){
                errorMessage = null
                onUnlockSuccess()
            }
            else{
                errorMessage = "Wrong master password"
            }
        }){
            Text("Verify")
        }
        Spacer(modifier = Modifier.height(16.dp))
        errorMessage?.let{
            Spacer(modifier = Modifier.height(8.dp))
            Text(it)
        }
    }
}