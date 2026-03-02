package com.roosterplanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.MeResponse
import com.roosterplanner.app.data.ScheduleItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(
    me: MeResponse,
    schedules: List<ScheduleItem>,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val df = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Welkom, ${me.user.display_name}", style = MaterialTheme.typography.titleLarge)
                Text("Locaties: ${me.locations.joinToString { it.name }}", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onLogout) { Text("Log uit") }
        }

        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Vernieuw rooster")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(schedules) { item ->
                val date = runCatching { LocalDate.parse(item.work_date, df) }.getOrNull()
                val title = item.shift_name ?: "Dienst"
                Card {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = item.location_name ?: "", style = MaterialTheme.typography.bodyMedium)
                        Text(text = item.work_date, style = MaterialTheme.typography.bodySmall ?: MaterialTheme.typography.bodyMedium)
                        val timeRange = listOfNotNull(item.start_time, item.end_time).joinToString(" – ")
                        if (timeRange.isNotBlank()) {
                            Text(timeRange, color = MaterialTheme.colorScheme.primary)
                        }
                        if (!item.notes.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(item.notes!!, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}
