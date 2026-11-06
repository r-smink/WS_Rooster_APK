package com.roosterplanner.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.Colleague
import com.roosterplanner.app.data.SwapRequestItem
import com.roosterplanner.app.data.SwapShift
import com.roosterplanner.app.ui.components.LocationLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapScreen(
    myShifts: List<SwapShift>,
    availableShifts: List<SwapShift>,
    incomingRequests: List<SwapRequestItem>,
    myRequests: List<SwapRequestItem>,
    colleagues: List<Colleague>,
    loading: Boolean,
    status: String?,
    onRefresh: () -> Unit,
    onToggleSwappable: (Long) -> Unit,
    onRequestSwap: (scheduleId: Long, colleagueId: Long?, reason: String?, partStart: String?, partEnd: String?) -> Unit,
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

            item {
                Text(
                    "Elke ruil moet worden goedgekeurd door een teamleider. Je krijgt een melding zodra dat is gebeurd.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Incoming requests and open offers
            if (incomingRequests.isNotEmpty()) {
                item {
                    SectionHeader("📬 Ruilverzoeken voor jou (${incomingRequests.size})")
                }
                items(incomingRequests, key = { "in-${it.id}" }) { req ->
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
                items(myShifts, key = { "my-${it.id}" }) { shift ->
                    MyShiftCard(
                        shift = shift,
                        onToggleSwappable = { onToggleSwappable(shift.id) },
                        onRequestSwap = { showRequestDialog = shift }
                    )
                }
            }

            // Available shifts
            item { SectionHeader("Beschikbare diensten (overnemen)") }
            if (availableShifts.isEmpty()) {
                item { EmptyText("Er zijn momenteel geen diensten beschikbaar om over te nemen.") }
            } else {
                items(availableShifts, key = { "av-${it.id}" }) { shift ->
                    AvailableShiftCard(
                        shift = shift,
                        onClaim = { onClaimShift(shift.id) }
                    )
                }
            }

            // My requests history
            if (myRequests.isNotEmpty()) {
                item { SectionHeader("Mijn ruilverzoeken") }
                items(myRequests, key = { "mine-${it.id}" }) { req ->
                    MyRequestCard(request = req)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    showRequestDialog?.let { shift ->
        RequestSwapDialog(
            shift = shift,
            colleagues = colleagues,
            onDismiss = { showRequestDialog = null },
            onSubmit = { colleagueId, reason, partStart, partEnd ->
                onRequestSwap(shift.id, colleagueId, reason, partStart, partEnd)
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
        fontWeight = FontWeight.Bold,
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

/** "06:00:00" -> "06:00" */
internal fun hhmm(time: String?): String? = time?.take(5)

private fun timeRange(start: String?, end: String?): String =
    listOfNotNull(hhmm(start), hhmm(end)).joinToString(" – ")

@Composable
private fun ShiftTitle(shiftName: String?, locationName: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LocationLogo(locationName, size = 22.dp)
        if (locationName != null) Spacer(Modifier.width(6.dp))
        Text(shiftName ?: "Dienst", style = MaterialTheme.typography.titleMedium)
    }
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
            ShiftTitle(shift.shift_name, shift.location_name)
            Text("${formatDutchDate(shift.work_date)}  ${shift.location_name ?: ""}", style = MaterialTheme.typography.bodySmall)
            val range = timeRange(shift.start_time, shift.end_time)
            if (range.isNotBlank()) Text(range, color = MaterialTheme.colorScheme.primary)

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
            ShiftTitle(shift.shift_name, shift.location_name)
            Text("${formatDutchDate(shift.work_date)}  ${shift.location_name ?: ""}", style = MaterialTheme.typography.bodySmall)
            val range = timeRange(shift.start_time, shift.end_time)
            if (range.isNotBlank()) Text(range, color = MaterialTheme.colorScheme.primary)
            shift.owner_name?.let { Text("Van: $it", style = MaterialTheme.typography.bodySmall) }
            Button(onClick = onClaim, modifier = Modifier.fillMaxWidth()) {
                Text("Overnemen aanvragen")
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: SwapRequestItem,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val isOpen = request.is_open == 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShiftTitle(request.shift_name, request.location_name)
            Text("${request.work_date?.let { formatDutchDate(it) } ?: ""}  ${request.location_name ?: ""}", style = MaterialTheme.typography.bodySmall)
            if (request.part_start != null) {
                Text(
                    "Deel van de dienst: ${timeRange(request.part_start, request.part_end)}",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                val range = timeRange(request.start_time, request.end_time)
                if (range.isNotBlank()) Text(range, color = MaterialTheme.colorScheme.primary)
            }
            request.requester_name?.let {
                Text(if (isOpen) "Aangeboden door: $it (open aanbod)" else "Van: $it", style = MaterialTheme.typography.bodyMedium)
            }
            request.reason?.takeIf { it.isNotBlank() }?.let { Text("Reden: $it", style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text(if (isOpen) "Ik neem het over" else "Accepteren") }
                if (!isOpen) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Weigeren") }
                }
            }
        }
    }
}

@Composable
private fun MyRequestCard(request: SwapRequestItem) {
    val statusText = when (request.status) {
        "pending" -> if (request.requested_name == null) "⏳ Open aanbod, wacht op collega" else "⏳ Wacht op reactie collega"
        "accepted" -> "⏳ Wacht op goedkeuring teamleider"
        "approved", "completed" -> "✓ Goedgekeurd"
        "rejected" -> "✗ Afgewezen"
        else -> request.status
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LocationLogo(request.location_name, size = 18.dp)
                if (request.location_name != null) Spacer(Modifier.width(6.dp))
                Text(request.shift_name ?: "Dienst", style = MaterialTheme.typography.titleSmall)
            }
            Text("${request.work_date?.let { formatDutchDate(it) } ?: ""} - $statusText", style = MaterialTheme.typography.bodySmall)
            if (request.part_start != null) {
                Text("Deel: ${timeRange(request.part_start, request.part_end)}", style = MaterialTheme.typography.bodySmall)
            }
            request.requested_name?.let { Text("Collega: $it", style = MaterialTheme.typography.bodySmall) }
            request.admin_notes?.takeIf { it.isNotBlank() }?.let {
                Text("Teamleider: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RequestSwapDialog(
    shift: SwapShift,
    colleagues: List<Colleague>,
    onDismiss: () -> Unit,
    onSubmit: (colleagueId: Long?, reason: String?, partStart: String?, partEnd: String?) -> Unit
) {
    val context = LocalContext.current
    var selectedColleague by remember { mutableStateOf<Colleague?>(null) }
    var reason by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }

    // Partial swap is only possible for shifts with fixed, same-day times
    val shiftStart = hhmm(shift.start_time)
    val shiftEnd = hhmm(shift.end_time)
    val canSplit = shiftStart != null && shiftEnd != null && shiftEnd > shiftStart
    var partial by remember { mutableStateOf(false) }
    var partStart by remember { mutableStateOf(shiftStart ?: "") }
    var partEnd by remember { mutableStateOf(shiftEnd ?: "") }
    val partValid = !partial || (partStart < partEnd && partStart >= (shiftStart ?: "") && partEnd <= (shiftEnd ?: ""))

    fun pickTime(current: String, onPicked: (String) -> Unit) {
        val (h, m) = current.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 9) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
        TimePickerDialog(context, { _, hour, minute -> onPicked("%02d:%02d".format(hour, minute)) }, h, m, true).show()
    }

    val filtered = remember(search, colleagues) {
        if (search.isBlank()) colleagues else colleagues.filter { it.name.contains(search.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dienst ruilen") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LocationLogo(shift.location_name, size = 22.dp)
                    if (shift.location_name != null) Spacer(Modifier.width(6.dp))
                    Text("${shift.shift_name ?: "Dienst"} op ${formatDutchDate(shift.work_date)}")
                }

                if (canSplit) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Alleen een deel van de dienst", modifier = Modifier.weight(1f))
                        Switch(checked = partial, onCheckedChange = { partial = it })
                    }
                    if (partial) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pickTime(partStart) { partStart = it } }, modifier = Modifier.weight(1f)) {
                                Text("Van $partStart")
                            }
                            OutlinedButton(onClick = { pickTime(partEnd) { partEnd = it } }, modifier = Modifier.weight(1f)) {
                                Text("Tot $partEnd")
                            }
                        }
                        if (!partValid) {
                            Text(
                                "Kies een deel binnen $shiftStart – $shiftEnd, met de eindtijd na de begintijd.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Text("Aan wie?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Zoek collega") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    // Bounded height so the list scrolls inside the dialog
                    Box(modifier = Modifier.heightIn(max = 240.dp)) {
                        LazyColumn {
                            item {
                                ColleagueOption(
                                    label = "Iedereen op de locatie (open aanbod)",
                                    selected = selectedColleague == null,
                                    onClick = { selectedColleague = null }
                                )
                                HorizontalDivider()
                            }
                            items(filtered, key = { it.id }) { c ->
                                ColleagueOption(
                                    label = c.name,
                                    selected = selectedColleague?.id == c.id,
                                    onClick = { selectedColleague = c }
                                )
                            }
                            if (filtered.isEmpty()) {
                                item { EmptyText("Geen collega gevonden") }
                            }
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
            Button(
                enabled = partValid,
                onClick = {
                    onSubmit(
                        selectedColleague?.id,
                        reason.takeIf { it.isNotBlank() },
                        if (partial) partStart else null,
                        if (partial) partEnd else null
                    )
                }
            ) {
                Text("Verzoek indienen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}

@Composable
private fun ColleagueOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
