package com.roosterplanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.MeResponse
import com.roosterplanner.app.data.ScheduleItem
import com.roosterplanner.app.ui.NavDest
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    me: MeResponse,
    nextShift: ScheduleItem?,
    onNavigate: (NavDest) -> Unit,
    onLogout: () -> Unit
) {
    val tf = DateTimeFormatter.ofPattern("HH:mm")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Welkom,", style = MaterialTheme.typography.bodyLarge)
                Text(
                    me.user.display_name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(onClick = onLogout) {
                Text("Log uit")
            }
        }

        // Volgende dienst sectie
        if (nextShift != null) {
            val formattedStart = nextShift.start_time?.let {
                runCatching { LocalTime.parse(it).format(tf) }.getOrElse { it }
            }
            val formattedEnd = nextShift.end_time?.let {
                runCatching { LocalTime.parse(it).format(tf) }.getOrElse { it }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(NavDest.SCHEDULE) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Volgende dienst",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            nextShift.shift_name ?: "Dienst",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${nextShift.work_date} • $formattedStart - $formattedEnd",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Geen geplande diensten gevonden voor de komende tijd.")
                }
            }
        }

        Text(
            "Locaties: ${me.locations.joinToString { it.name }}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        val menuItems = listOf(
            DashboardMenuItem(NavDest.SCHEDULE, "Rooster", Icons.Default.Schedule),
            DashboardMenuItem(NavDest.AVAILABILITY, "Beschikbaarheid", Icons.Default.EventAvailable),
            DashboardMenuItem(NavDest.SWAPS, "Ruilen", Icons.Default.SwapHoriz),
            DashboardMenuItem(NavDest.TIMEOFF, "Verlof", Icons.Default.BeachAccess),
            DashboardMenuItem(NavDest.PROFILE, "Mijn Profiel", Icons.Default.Person),
            DashboardMenuItem(NavDest.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
            DashboardMenuItem(NavDest.SICK, "Ziek melden", Icons.Default.MedicalInformation),
            DashboardMenuItem(NavDest.NOTIFICATIONS, "Meldingen", Icons.Default.Notifications)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(menuItems) { item ->
                DashboardCard(item) {
                    onNavigate(item.dest)
                }
            }
        }
    }
}

data class DashboardMenuItem(
    val dest: NavDest,
    val title: String,
    val icon: ImageVector
)

@Composable
fun DashboardCard(item: DashboardMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
