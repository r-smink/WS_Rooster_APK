package com.roosterplanner.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SickScreen(
    teamPhone: String?,
    onSubmit: (String?) -> Unit,
    loading: Boolean,
    status: String?
) {
    val (reason, setReason) = remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ziek melden")
        Text("Bel eerst je teamleider als het dringend is.")
        if (!teamPhone.isNullOrBlank()) {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$teamPhone"))
                context.startActivity(intent)
            }) {
                Text("Bel teamleider: $teamPhone")
            }
        }
        OutlinedTextField(
            value = reason,
            onValueChange = setReason,
            label = { Text("Toelichting (optioneel)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Button(onClick = { onSubmit(reason.ifBlank { null }) }, enabled = !loading) {
            Text(if (loading) "Verzenden..." else "Verzend ziekmelding")
        }
        if (!status.isNullOrBlank()) {
            Text(status)
        }
    }
}
