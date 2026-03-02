@file:OptIn(ExperimentalMaterial3Api::class)
package com.roosterplanner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.AvailabilityEntryRequest
import com.roosterplanner.app.data.Shift
import java.time.YearMonth
import java.time.format.DateTimeFormatter


@Composable
fun AvailabilityScreen(
    month: String,
    days: List<AvailabilityEntryRequest>,
    shifts: List<Shift>,
    onMonthChange: (YearMonth) -> Unit,
    onSubmit: (List<AvailabilityEntryRequest>) -> Unit,
    loading: Boolean,
    status: String?
) {
    val ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    var expanded by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(YearMonth.parse(month, ymFormatter)) }

    // Sync local state when prop changes
    LaunchedEffect(month) {
        selectedMonth = YearMonth.parse(month, ymFormatter)
    }

    // Local copy of entries so UI edits are stored before submit
    val entriesState = remember { mutableStateMapOf<String, AvailabilityEntryRequest>() }
    
    LaunchedEffect(days, selectedMonth) {
        entriesState.clear()
        // 1. Fill with defaults for the whole month
        val allMonthDays = buildMonthDays(selectedMonth)
        allMonthDays.forEach { date ->
            entriesState[date] = AvailabilityEntryRequest(date = date, is_available = true)
        }
        // 2. Overlay with actual data from the API
        days.forEach { entry ->
            if (entry.date.startsWith(selectedMonth.toString())) {
                entriesState[entry.date] = entry
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = selectedMonth.toString(),
                    onValueChange = {},
                    label = { Text("Maand") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    val current = YearMonth.now()
                    // Show previous month, current, and next 3 months
                    val months = listOf(
                        current.minusMonths(1),
                        current,
                        current.plusMonths(1),
                        current.plusMonths(2),
                        current.plusMonths(3)
                    )
                    months.forEach { m ->
                        DropdownMenuItem(text = { Text(m.toString()) }, onClick = {
                            selectedMonth = m; expanded = false; onMonthChange(m)
                        })
                    }
                }
            }
            TextButton(onClick = {
                entriesState.keys.forEach { date ->
                    val existing = entriesState[date]!!
                    entriesState[date] = existing.copy(is_available = true, shift_preference = null)
                }
            }) { Text("Alles beschikbaar") }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(entriesState.values.sortedBy { it.date }) { entry ->
                val shiftNames = listOf("Geen voorkeur") + shifts.map { it.name }
                val shiftIds = listOf<Long?>(null) + shifts.map { it.id }
                var shiftExpanded by remember { mutableStateOf(false) }
                val selectedIndex = shiftIds.indexOf(entry.shift_preference)
                val selectedLabel = shiftNames.getOrElse(selectedIndex.takeIf { it >= 0 } ?: 0) { "Geen voorkeur" }

                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(entry.date, style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            entriesState[entry.date] = entry.copy(is_available = !entry.is_available, shift_preference = null)
                        }) {
                            Text(if (entry.is_available) "✅ Beschikbaar" else "❌ Niet beschikbaar")
                        }
                        
                        ExposedDropdownMenuBox(expanded = shiftExpanded, onExpandedChange = { shiftExpanded = it }, modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            OutlinedTextField(
                                value = selectedLabel,
                                onValueChange = {},
                                label = { Text("Shift") },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftExpanded) },
                                modifier = Modifier.menuAnchor(),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            DropdownMenu(expanded = shiftExpanded, onDismissRequest = { shiftExpanded = false }) {
                                shiftNames.forEachIndexed { idx, label ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        entriesState[entry.date] = entry.copy(shift_preference = shiftIds[idx])
                                        shiftExpanded = false
                                    })
                                }
                            }
                        }
                    }
                    if (entry.is_available) {
                        OutlinedTextField(
                            value = entry.notes ?: "",
                            onValueChange = { newNotes ->
                                entriesState[entry.date] = entry.copy(notes = newNotes)
                            },
                            label = { Text("Opmerking / Tijden") },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        Button(
            onClick = { onSubmit(entriesState.values.toList()) }, 
            enabled = !loading, 
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(if (loading) "Verzenden..." else "Verzend beschikbaarheid")
        }
        if (!status.isNullOrBlank()) {
            Text(status, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

private fun buildMonthDays(month: YearMonth): List<String> {
    val start = month.atDay(1)
    val end = month.atEndOfMonth()
    return generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(end) }
        .map { it.format(DateTimeFormatter.ISO_DATE) }
        .toList()
}
