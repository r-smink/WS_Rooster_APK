@file:OptIn(ExperimentalMaterial3Api::class)
package com.roosterplanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
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
import kotlinx.coroutines.launch
import com.roosterplanner.app.data.AvailabilityEntryRequest
import com.roosterplanner.app.data.Shift
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

private fun normalizeTimeToHalfHour(time: String?): String {
    if (time.isNullOrBlank()) return ""
    val parts = time.split(":")
    if (parts.size != 2) return ""
    val hours = parts[0].toIntOrNull() ?: return ""
    val minutes = parts[1].toIntOrNull() ?: return ""
    if (hours !in 0..23 || minutes !in 0..59) return ""
    return "%02d:%02d".format(hours, if (minutes >= 30) 30 else 0)
}

/** "2026-10-22" -> "Donderdag 22 oktober 2026" */
fun formatDutchDate(isoDate: String): String {
    val dutch = Locale("nl")
    return runCatching {
        LocalDate.parse(isoDate)
            .format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", dutch))
            .replaceFirstChar { it.titlecase(dutch) }
    }.getOrElse { isoDate }
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
    var viewMode by remember { mutableStateOf(ScheduleViewMode.CALENDAR) }

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
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val next = currentMonth.plusMonths(1)
                    currentMonth = next
                    onMonthChange(next)
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Volgende")
                }
                IconButton(onClick = {
                    viewMode = if (viewMode == ScheduleViewMode.LIST) ScheduleViewMode.CALENDAR else ScheduleViewMode.LIST
                }) {
                    Icon(
                        if (viewMode == ScheduleViewMode.LIST) Icons.Default.CalendarMonth else Icons.AutoMirrored.Filled.List,
                        contentDescription = "Switch weergave"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewMode == ScheduleViewMode.LIST) {
            val sortedEntries = entriesState.values.sortedBy { it.date }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedEntries, key = { it.date }) { entry ->
                    AvailabilityCard(
                        entry = entry,
                        shifts = shifts,
                        onClick = { selectedDateForEdit = entry }
                    )
                }
            }
        } else {
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

@Composable
fun AvailabilityCard(
    entry: AvailabilityEntryRequest,
    shifts: List<Shift>,
    onClick: () -> Unit
) {
    val containerColor = when {
        !entry.is_available -> MaterialTheme.colorScheme.errorContainer
        entry.shift_preference != null -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    }
    val contentColor = if (entry.is_available) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val shiftName = entry.shift_preference?.let { id -> shifts.firstOrNull { it.id == id }?.name }
    val timeRange = listOfNotNull(entry.custom_start, entry.custom_end).joinToString(" – ")

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = formatDutchDate(entry.date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (entry.is_available) "Beschikbaar" else "Niet beschikbaar",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (entry.is_available && shiftName != null) {
                Text("Voorkeur: $shiftName", style = MaterialTheme.typography.bodyMedium)
            }
            if (entry.is_available && timeRange.isNotBlank()) {
                Text(timeRange, color = MaterialTheme.colorScheme.primary)
            }
            if (!entry.notes.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    entry.notes!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
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
    var customStart by remember { mutableStateOf(normalizeTimeToHalfHour(entry.custom_start)) }
    var customEnd by remember { mutableStateOf(normalizeTimeToHalfHour(entry.custom_end)) }
    var notes by remember { mutableStateOf(entry.notes ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        TimeWheelPickerDialog(
            title = "Starttijd",
            initialTime = customStart.takeIf { it.isNotBlank() },
            onDismiss = { showStartPicker = false },
            onConfirm = {
                customStart = it ?: ""
                showStartPicker = false
            }
        )
    }
    if (showEndPicker) {
        TimeWheelPickerDialog(
            title = "Eindtijd",
            initialTime = customEnd.takeIf { it.isNotBlank() },
            onDismiss = { showEndPicker = false },
            onConfirm = {
                customEnd = it ?: ""
                showEndPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatDutchDate(entry.date)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                        TimePickerField(
                            label = "Start",
                            value = customStart,
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        TimePickerField(
                            label = "Eind",
                            value = customEnd,
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f)
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
                    custom_start = customStart.takeIf { it.isNotBlank() },
                    custom_end = customEnd.takeIf { it.isNotBlank() },
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

@Composable
private fun TimePickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text("--:--") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        )
        Box(modifier = Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

@Composable
private fun TimeWheelPickerDialog(
    title: String,
    initialTime: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    val hourValues = (0..23).toList()
    val minuteValues = listOf(0, 30)

    val initialHour = initialTime?.substringBefore(':')?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val initialMinuteIndex = initialTime?.substringAfter(':')?.toIntOrNull()?.let { if (it >= 30) 1 else 0 } ?: 0

    var hourIndex by remember { mutableIntStateOf(hourValues.indexOf(initialHour).coerceAtLeast(0)) }
    var minuteIndex by remember { mutableIntStateOf(initialMinuteIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelColumn(
                    items = hourValues.map { "%02d".format(it) },
                    selectedIndex = hourIndex,
                    onSelectedIndexChange = { hourIndex = it },
                    modifier = Modifier.width(72.dp)
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                WheelColumn(
                    items = minuteValues.map { "%02d".format(it) },
                    selectedIndex = minuteIndex,
                    onSelectedIndexChange = { minuteIndex = it },
                    modifier = Modifier.width(72.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm("%02d:%02d".format(hourValues[hourIndex], minuteValues[minuteIndex]))
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) { Text("Wissen") }
                TextButton(onClick = onDismiss) { Text("Annuleren") }
            }
        }
    )
}

@Composable
private fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 40.dp
    val visibleCount = 5
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentOnSelectedIndexChange by rememberUpdatedState(onSelectedIndexChange)

    // contentPadding already reserves visibleCount / 2 rows above the first item,
    // so scrolling item `index` to the top of the content area centers it.
    // (Subtracting visibleCount / 2 here made every snap land 2 rows higher,
    // which cascaded the wheel back to "01".)
    fun scrollTargetFor(index: Int): Pair<Int, Int> = index to 0

    LaunchedEffect(Unit) {
        val (index, offset) = scrollTargetFor(selectedIndex)
        listState.scrollToItem(index, offset)
    }

    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                selectedIndex
            } else {
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
                info.visibleItemsInfo.minByOrNull {
                    kotlin.math.abs(it.offset + it.size / 2 - viewportCenter)
                }?.index?.coerceIn(0, items.lastIndex) ?: selectedIndex
            }
        }
    }

    LaunchedEffect(centeredIndex) { currentOnSelectedIndexChange(centeredIndex) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val (index, offset) = scrollTargetFor(centeredIndex)
            if (listState.firstVisibleItemIndex != index || listState.firstVisibleItemScrollOffset != offset) {
                listState.animateScrollToItem(index, offset)
            }
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
        )
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items.size) { index ->
                val selected = index == centeredIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            onSelectedIndexChange(index)
                            scope.launch {
                                val (target, offset) = scrollTargetFor(index)
                                listState.animateScrollToItem(target, offset)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                }
            }
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
