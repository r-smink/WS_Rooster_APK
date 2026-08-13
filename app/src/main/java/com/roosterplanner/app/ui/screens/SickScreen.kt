package com.roosterplanner.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.TeamPhoneContact

@Composable
fun SickScreen(
    teamPhoneContacts: List<TeamPhoneContact>,
    onSubmit: (String?) -> Unit,
    loading: Boolean,
    status: String?
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ziek melden", style = MaterialTheme.typography.headlineMedium)
        Text("Bel eerst je teamleider als het dringend is.")

        if (teamPhoneContacts.isEmpty()) {
            Text("Geen telefoonnummers beschikbaar.")
        } else {
            teamPhoneContacts.forEach { contact ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(contact.name, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                            context.startActivity(intent)
                        }) {
                            Text("Bel: ${contact.phone}")
                        }
                    }
                }
            }
        }
    }
}
