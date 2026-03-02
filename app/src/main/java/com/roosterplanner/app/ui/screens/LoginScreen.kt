package com.roosterplanner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.AuthConfig

@Composable
fun LoginScreen(
    initialConfig: AuthConfig?,
    loading: Boolean,
    error: String?,
    onConnect: (AuthConfig) -> Unit
) {
    val (url, setUrl) = remember { mutableStateOf(initialConfig?.baseUrl ?: "https://") }
    val (user, setUser) = remember { mutableStateOf(initialConfig?.username ?: "") }
    val (pass, setPass) = remember { mutableStateOf(initialConfig?.appPassword ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Verbind met RoosterPlanner", modifier = Modifier.padding(bottom = 8.dp))

        OutlinedTextField(
            value = url,
            onValueChange = setUrl,
            label = { Text("WordPress site URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = user,
            onValueChange = setUser,
            label = { Text("Gebruikersnaam") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = pass,
            onValueChange = setPass,
            label = { Text("Application Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(text = error, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val cfg = AuthConfig(url.trim(), user.trim(), pass.trim())
                onConnect(cfg)
            },
            enabled = !loading
        ) {
            Text(if (loading) "Verbinden..." else "Verbind")
        }

        TextButton(onClick = { setPass("") }) {
            Text("Wachtwoord wissen")
        }
    }
}
