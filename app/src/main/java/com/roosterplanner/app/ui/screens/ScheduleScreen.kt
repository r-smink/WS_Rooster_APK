@file:OptIn(ExperimentalMaterial3Api::class)
package com.roosterplanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.roosterplanner.app.data.MeResponse
import com.roosterplanner.app.data.ScheduleItem
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun ScheduleScreen(
    me: MeResponse,
    schedules: List<ScheduleItem>,
    onRefresh: () -> Unit,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onBack: () -> Unit
) {
    var viewMode by remember { mutableStateOf(ScheduleViewMode.LIST) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val tf = DateTimeFormatter.ofPattern("HH:mm")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mijn Rooster", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { 
                    viewMode = if (viewMode == ScheduleViewMode.LIST) ScheduleViewMode.CALENDAR else ScheduleViewMode.LIST 
                }) {
                    Icon(
                        if (viewMode == ScheduleViewMode.LIST) Icons.Default.CalendarMonth else Icons.AutoMirrored.Filled.List,
                        contentDescription = "Switch weergave"
                    )
                }
                Button(onClick = onBack) { Text("Dashboard") }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (viewMode == ScheduleViewMode.LIST) {
            ListView(schedules, onRefresh, tf)
        } else {
            CalendarView(
                currentMonth = currentMonth,
                schedules = schedules,
                onMonthChange = { newMonth ->
                    currentMonth = newMonth
                    val start = newMonth.atDay(1)
                    val end = newMonth.atEndOfMonth()
                    onDateRangeSelected(start, end)
                },
                tf = tf
            )
        }
    }
}

enum class ScheduleViewMode {
    LIST, CALENDAR
}

@Composable
fun ListView(schedules: List<ScheduleItem>, onRefresh: () -> Unit, tf: DateTimeFormatter) {
    Column {
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Vernieuw rooster")
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(schedules) { item ->
                ScheduleCard(item, tf)
            }
        }
    }
}

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    schedules: List<ScheduleItem>,
    onMonthChange: (YearMonth) -> Unit,
    tf: DateTimeFormatter
) {
    var selectedDateShifts by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }
    var selectedDateStr by remember { mutableStateOf("") }

    if (selectedDateShifts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { selectedDateShifts = emptyList() },
            title = { Text("Diensten op $selectedDateStr") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedDateShifts.forEach { shift ->
                        ScheduleCard(shift, tf)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDateShifts = emptyList() }) {
                    Text("Sluiten")
                }
            }
        )
    }

    Column {
        // Month Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Vorige")
            }
            Text(
                text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale("nl")) + " " + currentMonth.year,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Volgende")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekdays Header
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekDays = listOf("Ma", "Di", "Wo", "Do", "Vr", "Za", "Zo")
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val firstDayOfMonth = currentMonth.atDay(1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val daysInMonth = currentMonth.lengthOfMonth()

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dayOfWeek - 1) { Spacer(Modifier) }
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val dateStr = currentMonth.atDay(day).format(DateTimeFormatter.ISO_DATE)
                val daySchedules = schedules.filter { it.work_date == dateStr }
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (daySchedules.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable {
                            if (daySchedules.isNotEmpty()) {
                                selectedDateShifts = daySchedules
                                selectedDateStr = dateStr
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (daySchedules.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                        )
                        if (daySchedules.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                daySchedules.forEach { _ ->
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
                }
            }
        }
    }
}

@Composable
fun ScheduleCard(item: ScheduleItem, tf: DateTimeFormatter) {
    val formattedStart = item.start_time?.let { 
        runCatching { LocalTime.parse(it).format(tf) }.getOrElse { it }
    }
    val formattedEnd = item.end_time?.let { 
        runCatching { LocalTime.parse(it).format(tf) }.getOrElse { it }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.shift_name ?: "Dienst",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.location_name ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = item.work_date,
                style = MaterialTheme.typography.bodySmall
            )
            val timeRange = listOfNotNull(formattedStart, formattedEnd).joinToString(" – ")
            if (timeRange.isNotBlank()) {
                Text(timeRange, color = MaterialTheme.colorScheme.primary)
            }
            if (!item.notes.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    item.notes!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
