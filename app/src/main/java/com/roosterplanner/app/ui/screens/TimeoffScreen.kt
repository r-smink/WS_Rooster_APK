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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.TimeoffItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeoffScreen(
    items: List<TimeoffItem>,
    loading: Boolean,
    status: String?,
    onRefresh: () -> Unit,
    onSubmit: (String, String, String, String?) -> Unit,
    onBack: () -> Unit
) {
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("vacation") }
    var reason by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }

    val typeOptions = listOf(
        "vacation" to "Vakantie",
        "personal" to "Persoonlijk",
        "appointment" to "Afspraak/dokter",
        "other" to "Overig"
    )

    LaunchedEffect(Unit) { onRefresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verlof Aanvragen") },
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

            // Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Nieuwe verlofaanvraag", style = MaterialTheme.typography.titleMedium)

                        // Start date
                        OutlinedTextField(
                            value = startDate?.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Start datum *") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                TextButton(onClick = { showStartPicker = true }) { Text("Kies") }
                            }
                        )

                        // End date
                        OutlinedTextField(
                            value = endDate?.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Eind datum *") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                TextButton(onClick = { showEndPicker = true }) { Text("Kies") }
                            }
                        )

                        // Type dropdown
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = !typeExpanded }
                        ) {
                            TextField(
                                value = typeOptions.firstOrNull { it.first == selectedType }?.second ?: selectedType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Type verlof *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                typeOptions.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { selectedType = value; typeExpanded = false }
                                    )
                                }
                            }
                        }

                        // Reason
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Reden / Toelichting") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Button(
                            onClick = {
                                val s = startDate?.toString()
                                val e = endDate?.toString()
                                if (s != null && e != null) {
                                    onSubmit(s, e, selectedType, reason.takeIf { it.isNotBlank() })
                                    reason = ""
                                }
                            },
                            enabled = startDate != null && endDate != null && !loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (loading) "Verzenden..." else "Verlof Aanvragen")
                        }

                        if (!status.isNullOrBlank()) {
                            Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // History
            item {
                Text("Mijn Verlofaanvragen", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }

            if (items.isEmpty()) {
                item { Text("Je hebt nog geen verlofaanvragen ingediend.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
            } else {
                items(items) { item -> TimeoffCard(item) }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Date pickers
    if (showStartPicker) {
        DatePickerModal(
            initial = startDate,
            onConfirm = { date ->
                startDate = date
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        DatePickerModal(
            initial = endDate,
            onConfirm = { date ->
                endDate = date
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initial: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun TimeoffCard(item: TimeoffItem) {
    val statusText = when (item.status) {
        "pending" -> "⏳ In behandeling"
        "approved" -> "✅ Goedgekeurd"
        "rejected" -> "❌ Afgekeurd"
        else -> item.status
    }
    val typeText = when (item.type) {
        "vacation" -> "🏖️ Vakantie"
        "personal" -> "👤 Persoonlijk"
        "appointment" -> "🏥 Afspraak"
        "other" -> "📝 Overig"
        else -> item.type
    }
    val containerColor = when (item.status) {
        "pending" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        "approved" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        "rejected" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(typeText, style = MaterialTheme.typography.titleSmall)
                Text(statusText, style = MaterialTheme.typography.bodySmall)
            }
            val fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val start = runCatching { LocalDate.parse(item.start_date).format(fmt) }.getOrDefault(item.start_date)
            val end = runCatching { LocalDate.parse(item.end_date).format(fmt) }.getOrDefault(item.end_date)
            Text("$start t/m $end (${item.days_requested ?: "?"} dagen)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            item.reason?.takeIf { it.isNotBlank() }?.let {
                Text("Reden: $it", style = MaterialTheme.typography.bodySmall)
            }
            item.admin_notes?.takeIf { it.isNotBlank() }?.let {
                Text("Reactie planner: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            item.requested_at?.let {
                Text("Aangevraagd: ${it.take(10)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}
