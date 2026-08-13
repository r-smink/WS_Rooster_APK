package com.roosterplanner.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.Colleague
import com.roosterplanner.app.data.SwapShift

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapScreen(
    myShifts: List<SwapShift>,
    availableShifts: List<SwapShift>,
    incomingRequests: List<com.roosterplanner.app.data.SwapRequestItem>,
    myRequests: List<com.roosterplanner.app.data.SwapRequestItem>,
    colleagues: List<Colleague>,
    loading: Boolean,
    status: String?,
    onRefresh: () -> Unit,
    onToggleSwappable: (Long) -> Unit,
    onRequestSwap: (Long, Long?, String?) -> Unit,
    onRespondSwap: (Long, String) -> Unit,
    onClaimShift: (Long) -> Unit,
    onBack: () -> Unit
) {
    var showRequestDialog by remember { mutableStateOf<SwapShift?>(null) }

    LaunchedEffect(Unit) { onRefresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dienst Ruilen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            if (!status.isNullOrBlank()) {
                item {
                    Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Incoming requests
            if (incomingRequests.isNotEmpty()) {
                item {
                    SectionHeader("📬 Ruilverzoeken voor jou (${incomingRequests.size})")
                }
                items(incomingRequests) { req ->
                    IncomingRequestCard(
                        request = req,
                        onAccept = { onRespondSwap(req.id, "accept") },
                        onReject = { onRespondSwap(req.id, "reject") }
                    )
                }
            }

            // My shifts
            item { SectionHeader("Mijn diensten") }
            if (myShifts.isEmpty()) {
                item { EmptyText("Je hebt geen komende diensten.") }
            } else {
                items(myShifts) { shift ->
                    MyShiftCard(
                        shift = shift,
                        onToggleSwappable = { onToggleSwappable(shift.id) },
                        onRequestSwap = { showRequestDialog = shift }
                    )
                }
            }

            // Available shifts
            item { SectionHeader("Besikbare diensten (overnemen)") }
            if (availableShifts.isEmpty()) {
                item { EmptyText("Er zijn momenteel geen diensten beschikbaar om over te nemen.") }
            } else {
                items(availableShifts) { shift ->
                    AvailableShiftCard(
                        shift = shift,
                        onClaim = { onClaimShift(shift.id) }
                    )
                }
            }

            // My requests history
            if (myRequests.isNotEmpty()) {
                item { SectionHeader("Mijn ruilverzoeken") }
                items(myRequests) { req ->
                    MyRequestCard(request = req)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Request swap dialog
    if (showRequestDialog != null) {
        RequestSwapDialog(
            shift = showRequestDialog!!,
            colleagues = colleagues,
            onDismiss = { showRequestDialog = null },
            onSubmit = { colleagueId, reason ->
                onRequestSwap(showRequestDialog!!.id, colleagueId, reason)
                showRequestDialog = null
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )
}

@Composable
private fun MyShiftCard(
    shift: SwapShift,
    onToggleSwappable: () -> Unit,
    onRequestSwap: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(shift.shift_name ?: "Dienst", style = MaterialTheme.typography.titleMedium)
            Text("${shift.work_date}  ${shift.location_name ?: ""}", style = MaterialTheme.typography.bodySmall)
            val timeRange = listOfNotNull(shift.start_time, shift.end_time).joinToString(" – ")
            if (timeRange.isNotBlank()) Text(timeRange, color = MaterialTheme.colorScheme.primary)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequestSwap, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Text(" Ruilen")
                }
                TextButton(
                    onClick = onToggleSwappable,
                    modifier = Modifier.weight(1f),
                    colors = if (shift.is_swappable == 1)
                        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    else ButtonDefaults.textButtonColors()
                ) {
                    Text(if (shift.is_swappable == 1) "✓ Ruilbaar" else "Ruilbaar maken")
                }
            }
        }
    }
}

@Composable
private fun AvailableShiftCard(
    shift: SwapShift,
    onClaim: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(shift.shift_name ?: "Dienst", style = MaterialTheme.typography.titleMedium)
            Text("${shift.work_date}  ${shift.location_name ?: ""}", style = MaterialTheme.typography.bodySmall)
            val timeRange = listOfNotNull(shift.start_time, shift.end_time).joinToString(" – ")
            if (timeRange.isNotBlank()) Text(timeRange, color = MaterialTheme.colorScheme.primary)
            shift.owner_name?.let { Text("Van: $it", style = MaterialTheme.typography.bodySmall) }
            Button(onClick = onClaim, modifier = Modifier.fillMaxWidth()) {
                Text("Dienst overnemen")
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: com.roosterplanner.app.data.SwapRequestItem,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(request.shift_name ?: "Dienst", style = MaterialTheme.typography.titleMedium)
            Text("${request.work_date ?: ""}  ${request.location_name ?: ""}", style = MaterialTheme.typography.bodySmall)
            request.requester_name?.let { Text("Van: $it", style = MaterialTheme.typography.bodyMedium) }
            request.reason?.takeIf { it.isNotBlank() }?.let { Text("Reden: $it", style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("Accepteren") }
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Weigeren") }
            }
        }
    }
}

@Composable
private fun MyRequestCard(request: com.roosterplanner.app.data.SwapRequestItem) {
    val statusText = when (request.status) {
        "pending" -> "⏳ In behandeling"
        "approved", "completed" -> "✓ Voltooid"
        "rejected" -> "✗ Afgewezen"
        else -> request.status
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(request.shift_name ?: "Dienst", style = MaterialTheme.typography.titleSmall)
            Text("${request.work_date ?: ""} - $statusText", style = MaterialTheme.typography.bodySmall)
            request.requested_name?.let { Text("Gevraagd aan: $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun RequestSwapDialog(
    shift: SwapShift,
    colleagues: List<Colleague>,
    onDismiss: () -> Unit,
    onSubmit: (Long?, String?) -> Unit
) {
    var selectedColleague by remember { mutableStateOf<Colleague?>(null) }
    var reason by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dienst ruilen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${shift.shift_name ?: "Dienst"} op ${shift.work_date}")
                Text("Specifieke collega (optioneel):", style = MaterialTheme.typography.bodyMedium)
                // Simple dropdown-like selection
                TextButton(onClick = { expanded = !expanded }) {
                    Text(selectedColleague?.name ?: "Iedereen mag reageren")
                }
                if (expanded) {
                    Column {
                        TextButton(onClick = {
                            selectedColleague = null
                            expanded = false
                        }) { Text("Iedereen mag reageren") }
                        colleagues.forEach { c ->
                            TextButton(onClick = {
                                selectedColleague = c
                                expanded = false
                            }) { Text(c.name) }
                        }
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reden (optioneel)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedColleague?.id, reason.takeIf { it.isNotBlank() }) }) {
                Text("Verzoek indienen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
