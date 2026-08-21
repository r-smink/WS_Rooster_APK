@file:OptIn(ExperimentalMaterial3Api::class)
package com.roosterplanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.data.AvailabilityEntryRequest
import com.roosterplanner.app.data.Shift
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

fun roundTimeTo30Min(time: String?): String? {
    if (time.isNullOrBlank()) return time
    val parts = time.split(":")
    if (parts.size != 2) return time
    val hours = parts[0].toIntOrNull() ?: return time
    val minutes = parts[1].toIntOrNull() ?: return time
    if (hours !in 0..23 || minutes !in 0..59) return time

    val totalMinutes = hours * 60 + minutes
    val rounded = (kotlin.math.round(totalMinutes / 30.0) * 30).toInt() % (24 * 60)
    val newHours = rounded / 60
    val newMinutes = rounded % 60
    return "%02d:%02d".format(newHours, newMinutes)
}

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
    var currentMonth by remember { mutableStateOf(YearMonth.parse(month, ymFormatter)) }
    var selectedDateForEdit by remember { mutableStateOf<AvailabilityEntryRequest?>(null) }
    
    // Local copy of entries
    val entriesState = remember { mutableStateMapOf<String, AvailabilityEntryRequest>() }
    
    LaunchedEffect(month) {
        currentMonth = YearMonth.parse(month, ymFormatter)
    }

    LaunchedEffect(days, currentMonth) {
        entriesState.clear()
        val allMonthDays = buildMonthDays(currentMonth)
        allMonthDays.forEach { date ->
            entriesState[date] = AvailabilityEntryRequest(date = date, is_available = true)
        }
        days.forEach { entry ->
            if (entry.date.startsWith(currentMonth.toString())) {
                entriesState[entry.date] = entry
            }
        }
    }

    if (selectedDateForEdit != null) {
        EditAvailabilityDialog(
            entry = selectedDateForEdit!!,
            shifts = shifts,
            onDismiss = { selectedDateForEdit = null },
            onSave = { updated ->
                entriesState[updated.date] = updated
                selectedDateForEdit = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Month Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val next = currentMonth.minusMonths(1)
                currentMonth = next
                onMonthChange(next)
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Vorige")
            }
            
            Text(
                text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale("nl")) + " " + currentMonth.year,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = {
                val next = currentMonth.plusMonths(1)
                currentMonth = next
                onMonthChange(next)
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Volgende")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekdays Header
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekDays = listOf("Ma", "Di", "Wo", "Do", "Vr", "Za", "Zo")
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid
        val firstDayOfMonth = currentMonth.atDay(1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val daysInMonth = currentMonth.lengthOfMonth()
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Empty spaces for previous month
            items(dayOfWeek - 1) { Spacer(Modifier) }
            
            // Days of the month
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val dateStr = currentMonth.atDay(day).format(DateTimeFormatter.ISO_DATE)
                val entry = entriesState[dateStr] ?: AvailabilityEntryRequest(date = dateStr, is_available = true)
                
                CalendarDayCell(
                    day = day,
                    entry = entry,
                    onClick = { selectedDateForEdit = entry }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = {
                    entriesState.keys.forEach { date ->
                        val existing = entriesState[date]!!
                        entriesState[date] = existing.copy(is_available = true, shift_preference = null)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Alles beschikbaar")
            }
            
            Button(
                onClick = { onSubmit(entriesState.values.toList()) },
                enabled = !loading,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (loading) "Opslaan..." else "Opslaan")
            }
        }

        if (!status.isNullOrBlank()) {
            Text(
                text = status,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    entry: AvailabilityEntryRequest,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        !entry.is_available -> MaterialTheme.colorScheme.errorContainer
        entry.shift_preference != null -> MaterialTheme.colorScheme.primaryContainer
        entry.is_available -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    }

    val contentColor = when {
        !entry.is_available -> MaterialTheme.colorScheme.onErrorContainer
        entry.is_available -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (entry.shift_preference != null) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAvailabilityDialog(
    entry: AvailabilityEntryRequest,
    shifts: List<Shift>,
    onDismiss: () -> Unit,
    onSave: (AvailabilityEntryRequest) -> Unit
) {
    var isAvailable by remember { mutableStateOf(entry.is_available) }
    var selectedShiftId by remember { mutableStateOf(entry.shift_preference) }
    var customStart by remember { mutableStateOf(roundTimeTo30Min(entry.custom_start) ?: "") }
    var customEnd by remember { mutableStateOf(roundTimeTo30Min(entry.custom_end) ?: "") }
    var notes by remember { mutableStateOf(entry.notes ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Beschikbaarheid ${entry.date}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Beschikbaar")
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { 
                            isAvailable = it
                            if (!it) selectedShiftId = null
                        }
                    )
                }

                if (isAvailable) {
                    val shiftNames = listOf("Geen voorkeur") + shifts.map { it.name }
                    val shiftIds = listOf<Long?>(null) + shifts.map { it.id }
                    val selectedIndex = shiftIds.indexOf(selectedShiftId).coerceAtLeast(0)
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = shiftNames[selectedIndex],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Shift voorkeur") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            shiftNames.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedShiftId = shiftIds[index]
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customStart,
                            onValueChange = { customStart = roundTimeTo30Min(it) ?: it },
                            label = { Text("Start (HH:mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customEnd,
                            onValueChange = { customEnd = roundTimeTo30Min(it) ?: it },
                            label = { Text("Eind (HH:mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Opmerking") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(entry.copy(
                    is_available = isAvailable,
                    shift_preference = selectedShiftId,
                    custom_start = roundTimeTo30Min(customStart.takeIf { it.isNotBlank() }),
                    custom_end = roundTimeTo30Min(customEnd.takeIf { it.isNotBlank() }),
                    notes = notes.takeIf { it.isNotBlank() }
                ))
            }) {
                Text("Toepassen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuleren")
            }
        }
    )
}

private fun buildMonthDays(month: YearMonth): List<String> {
    val start = month.atDay(1)
    val end = month.atEndOfMonth()
    return generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(end) }
        .map { it.format(DateTimeFormatter.ISO_DATE) }
        .toList()
}
