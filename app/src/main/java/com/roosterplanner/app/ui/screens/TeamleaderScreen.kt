package com.roosterplanner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.ApprovalSwap
import com.roosterplanner.app.data.ApprovalTimeoff
import com.roosterplanner.app.data.ApprovalsResponse
import com.roosterplanner.app.ui.components.LocationLogo

/** What the teamleider is about to reject (asks for an optional explanation first). */
private sealed class RejectTarget {
    data class Swap(val id: Long) : RejectTarget()
    data class Timeoff(val id: Long) : RejectTarget()
}

@Composable
fun TeamleaderScreen(
    approvals: ApprovalsResponse?,
    status: String?,
    onRefresh: () -> Unit,
    onDecideSwap: (id: Long, approve: Boolean, notes: String?) -> Unit,
    onDecideTimeoff: (id: Long, approve: Boolean, notes: String?) -> Unit
) {
    var rejectTarget by remember { mutableStateOf<RejectTarget?>(null) }

    LaunchedEffect(Unit) { onRefresh() }

    val swaps = approvals?.swaps.orEmpty()
    val waitingSwaps = swaps.filter { it.status == "accepted" }
    val openSwaps = swaps.filter { it.status != "accepted" }
    val timeoff = approvals?.timeoff.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Te beoordelen", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onRefresh) { Text("Vernieuwen") }
            }
        }
        if (!status.isNullOrBlank()) {
            item { Text(status, color = MaterialTheme.colorScheme.primary) }
        }

        item { Header("🔁 Ruilverzoeken (${waitingSwaps.size})") }
        if (waitingSwaps.isEmpty()) {
            item { Muted("Geen ruilverzoeken die op jouw goedkeuring wachten.") }
        }
        items(waitingSwaps, key = { "sw-${it.id}" }) { swap ->
            SwapApprovalCard(
                swap = swap,
                onApprove = { onDecideSwap(swap.id, true, null) },
                onReject = { rejectTarget = RejectTarget.Swap(swap.id) }
            )
        }

        item { Header("🏖️ Verlofaanvragen (${timeoff.size})") }
        if (timeoff.isEmpty()) {
            item { Muted("Geen openstaande verlofaanvragen.") }
        }
        items(timeoff, key = { "to-${it.id}" }) { t ->
            TimeoffApprovalCard(
                timeoff = t,
                onApprove = { onDecideTimeoff(t.id, true, null) },
                onReject = { rejectTarget = RejectTarget.Timeoff(t.id) }
            )
        }

        if (openSwaps.isNotEmpty()) {
            item { Header("⏳ Wacht nog op een collega (${openSwaps.size})") }
            items(openSwaps, key = { "op-${it.id}" }) { swap ->
                SwapApprovalCard(swap = swap, onApprove = null, onReject = { rejectTarget = RejectTarget.Swap(swap.id) })
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    rejectTarget?.let { target ->
        var notes by remember(target) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Afwijzen") },
            text = {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Toelichting voor de medewerker (optioneel)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val n = notes.takeIf { it.isNotBlank() }
                        when (target) {
                            is RejectTarget.Swap -> onDecideSwap(target.id, false, n)
                            is RejectTarget.Timeoff -> onDecideTimeoff(target.id, false, n)
                        }
                        rejectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Afwijzen") }
            },
            dismissButton = { TextButton(onClick = { rejectTarget = null }) { Text("Annuleren") } }
        )
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
}

@Composable
private fun DecisionButtons(onApprove: (() -> Unit)?, onReject: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onApprove != null) {
            Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Goedkeuren") }
        }
        Button(
            onClick = onReject,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text("Afwijzen") }
    }
}

@Composable
private fun SwapApprovalCard(swap: ApprovalSwap, onApprove: (() -> Unit)?, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LocationLogo(swap.location_name, size = 22.dp)
                if (swap.location_name != null) Spacer(Modifier.width(6.dp))
                Text(swap.shift_name ?: "Dienst", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "${swap.work_date?.let { formatDutchDate(it) } ?: ""} · ${swap.location_name ?: ""}",
                style = MaterialTheme.typography.bodySmall
            )
            if (swap.part_start != null) {
                Text(
                    "Deel van de dienst: ${hhmm(swap.part_start)} – ${hhmm(swap.part_end)} (dienst ${hhmm(swap.start_time)} – ${hhmm(swap.end_time)})",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (swap.start_time != null && swap.end_time != null) {
                Text("${hhmm(swap.start_time)} – ${hhmm(swap.end_time)}", color = MaterialTheme.colorScheme.primary)
            }
            Text("Van: ${swap.requester_name ?: "?"}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Naar: ${swap.taker_name ?: "nog niemand (open aanbod)"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            swap.reason?.takeIf { it.isNotBlank() }?.let {
                Text("Reden: $it", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            DecisionButtons(onApprove, onReject)
        }
    }
}

private fun timeoffTypeLabel(type: String) = when (type) {
    "vacation" -> "Vakantie"
    "personal" -> "Persoonlijk"
    "appointment" -> "Afspraak"
    "sick" -> "Ziek"
    "other" -> "Overig"
    else -> type
}

@Composable
private fun TimeoffApprovalCard(timeoff: ApprovalTimeoff, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(timeoff.employee_name ?: "Medewerker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val period = if (timeoff.start_date == timeoff.end_date) formatDutchDate(timeoff.start_date)
                else "${formatDutchDate(timeoff.start_date)} t/m ${formatDutchDate(timeoff.end_date)}"
            Text(period, style = MaterialTheme.typography.bodyMedium)
            val days = timeoff.days_requested ?: 1
            Text(
                "${timeoffTypeLabel(timeoff.type)} · $days ${if (days == 1) "dag" else "dagen"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            timeoff.reason?.takeIf { it.isNotBlank() }?.let {
                Text("Reden: $it", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            DecisionButtons(onApprove, onReject)
        }
    }
}
