package com.example.passwordstorageapp.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.passwordstorageapp.data.VaultEntry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Composable
fun EntryScreen(vaultEntry: VaultEntry,
                onEditComplete : (VaultEntry) -> Unit){
    var isEditing by remember { mutableStateOf(false) }
    var editedService by remember { mutableStateOf(vaultEntry.serviceName) }
    var editedUsername by remember { mutableStateOf(vaultEntry.username) }
    var editedPassword by remember { mutableStateOf(vaultEntry.password) }
    var editedNote by remember { mutableStateOf(vaultEntry.notes ?: "") }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ){
        if(!isEditing){
            Text(editedService)
            Spacer(modifier = Modifier.height(8.dp))
            Text(editedUsername)
            Spacer(modifier = Modifier.height(8.dp))
            Text(editedPassword)
            Spacer(modifier = Modifier.height(8.dp))
            Text(editedNote)
            Spacer(modifier = Modifier.height(8.dp))
        }
        else{
            OutlinedTextField(
                value = editedService,
                onValueChange = {
                    editedService = it
                },
                label = { Text("Service name") },
                singleLine = true
            )
            OutlinedTextField(
                value = editedUsername,
                onValueChange = {
                    editedUsername = it
                },
                label = { Text("Username/ Email") },
                singleLine = true
            )
            OutlinedTextField(
                value = editedPassword,
                onValueChange = {
                    editedPassword = it
                },
                label = { Text("Password") },
                singleLine = true
            )
            OutlinedTextField(
                value = editedNote,
                onValueChange = {
                    editedNote = it
                },
                label = { Text("Notes") },
                singleLine = false,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if(!isEditing){
            Button(onClick = {
                isEditing = true
            }){
                Text("Edit")
            }
        }
        else{
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ){
                Button(onClick = {
                    val editedEntry = vaultEntry.copy(
                        serviceName = editedService,
                        username = editedUsername,
                        password = editedPassword,
                        notes = editedNote
                    )
                    isEditing = false
                    onEditComplete(editedEntry)
                }){
                    Text("Confirm")
                }
                Button(onClick = {
                    isEditing = false
                }){
                    Text("Cancel")
                }
            }
        }
    }
}