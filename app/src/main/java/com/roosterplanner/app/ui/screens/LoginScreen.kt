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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.AuthConfig
import com.roosterplanner.app.ui.theme.RoosterPlannerTheme

@Composable
fun LoginScreen(
    initialUrl: String?,
    loading: Boolean,
    error: String?,
    onLogin: (String, String, String) -> Unit
) {
    val (url, setUrl) = remember { mutableStateOf(initialUrl ?: "https://") }
    val (user, setUser) = remember { mutableStateOf("") }
    val (pass, setPass) = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Verbind met WerkRooster", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Text(text = "Voer je WordPress site URL en inloggegevens in.", modifier = Modifier.padding(bottom = 8.dp))

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
            label = { Text("Wachtwoord") },
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
                onLogin(url.trim(), user.trim(), pass.trim())
            },
            enabled = !loading
        ) {
            Text(if (loading) "Inloggen..." else "Inloggen")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    RoosterPlannerTheme {
        LoginScreen(
            initialUrl = null,
            loading = false,
            error = null,
            onLogin = { _, _, _ -> }
        )
    }
}
