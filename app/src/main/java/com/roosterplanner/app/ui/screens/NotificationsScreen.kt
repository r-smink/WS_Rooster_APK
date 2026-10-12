package com.roosterplanner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.NotificationItem

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onRefresh: () -> Unit,
    onMarkRead: (Long) -> Unit
) {
    val unreadIds = notifications.filter { it.is_read == 0 }.map { it.id }
    LaunchedEffect(unreadIds) {
        unreadIds.forEach(onMarkRead)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        if (notifications.isEmpty()) {
            Text("Geen meldingen", fontWeight = FontWeight.SemiBold)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notifications) { notification ->
                    Card(colors = CardDefaults.cardColors(), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(notification.title, fontWeight = FontWeight.Bold)
                            if (!notification.message.isNullOrBlank()) Text(notification.message ?: "")
                            Text(notification.created_at)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { onRefresh() }
}
