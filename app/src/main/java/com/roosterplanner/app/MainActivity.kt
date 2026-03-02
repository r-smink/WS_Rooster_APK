package com.roosterplanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.roosterplanner.app.data.ApiClient
import com.roosterplanner.app.data.AuthConfig
import com.roosterplanner.app.data.ConfigStore
import com.roosterplanner.app.data.MeResponse
import com.roosterplanner.app.data.RoosterPlannerRepository
import com.roosterplanner.app.data.ScheduleItem
import com.roosterplanner.app.data.AvailabilityEntryRequest
import com.roosterplanner.app.data.Shift
import com.roosterplanner.app.data.AvailabilityResponse
import com.roosterplanner.app.data.NotificationItem
import com.roosterplanner.app.data.AvailabilityUpsertRequest
import com.roosterplanner.app.ui.NavDest
import com.roosterplanner.app.ui.screens.LoginScreen
import com.roosterplanner.app.ui.screens.ScheduleScreen
import com.roosterplanner.app.ui.screens.AvailabilityScreen
import com.roosterplanner.app.ui.screens.SickScreen
import com.roosterplanner.app.ui.screens.NotificationsScreen
import com.roosterplanner.app.ui.theme.RoosterPlannerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val configStore by lazy { ConfigStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RoosterPlannerTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                var config by remember { mutableStateOf<AuthConfig?>(null) }
                var repo by remember { mutableStateOf<RoosterPlannerRepository?>(null) }
                var me by remember { mutableStateOf<MeResponse?>(null) }
                var schedules by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }
                var loading by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }
                var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
                var availabilityEntries by remember { mutableStateOf<List<AvailabilityEntryRequest>>(emptyList()) }
                var shifts by remember { mutableStateOf<List<Shift>>(emptyList()) }
                var availabilityStatus by remember { mutableStateOf<String?>(null) }
                var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
                var teamPhone by remember { mutableStateOf<String?>("+31638993687") }
                val scope = rememberCoroutineScope()

                // Restore saved config from DataStore
                LaunchedEffect(Unit) {
                    configStore.config.collect { stored ->
                        config = stored
                        if (stored != null) {
                            connectAndLoad(stored, snackbarHostState, { repo = it }, { me = it }, { schedules = it }, { loading = it }, { error = it })
                        }
                    }
                }

                // Load stored teamleader phone (if any)
                LaunchedEffect(Unit) {
                    configStore.teamPhone.collect { phone ->
                        if (!phone.isNullOrBlank()) teamPhone = phone
                    }
                }

                var currentTab by remember { mutableStateOf(NavDest.DASHBOARD) }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (config != null && me != null) {
                            val tabs = listOf(
                                NavDest.DASHBOARD,
                                NavDest.AVAILABILITY,
                                NavDest.SICK,
                                NavDest.NOTIFICATIONS
                            )
                            NavigationBar {
                                tabs.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = { Icon(iconFor(tab), contentDescription = tab.label) },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    if (config == null) {
                        LoginScreen(
                            initialConfig = null,
                            loading = loading,
                            error = error,
                            onConnect = { cfg ->
                                config = cfg
                                scope.launch { configStore.save(cfg) }
                                connectAndLoad(cfg, snackbarHostState, { repo = it }, { me = it }, { schedules = it }, { loading = it }, { error = it })
                            }
                        )
                    } else if (me != null) {
                        val locationId = me?.locations?.firstOrNull()?.id?.toLong() ?: 0L

                        // Load shifts, availability, notifications when tab/month changes
                        LaunchedEffect(currentTab, selectedMonth, locationId) {
                            if (currentTab == NavDest.AVAILABILITY && locationId != 0L) {
                                availabilityStatus = null
                                // Clear entries while loading new ones to avoid showing old month data
                                availabilityEntries = emptyList()
                                
                                if (shifts.isEmpty()) {
                                    repo?.fetchShifts(locationId)?.onSuccess { shifts = it }
                                }
                                
                                loading = true
                                repo?.fetchAvailability(selectedMonth.toString(), locationId)?.onSuccess { response ->
                                    availabilityEntries = response.items.map { item ->
                                        AvailabilityEntryRequest(
                                            date = item.work_date,
                                            is_available = item.is_available == 1,
                                            shift_preference = item.shift_preference,
                                            custom_start = item.custom_start,
                                            custom_end = item.custom_end,
                                            notes = item.notes
                                        )
                                    }
                                }?.onFailure {
                                    availabilityStatus = "Kon beschikbaarheid niet laden"
                                }
                                loading = false
                            }
                            if (currentTab == NavDest.NOTIFICATIONS) {
                                repo?.fetchNotifications()?.onSuccess { notifications = it }
                            }
                        }

                        Column(modifier = Modifier.padding(padding)) {
                            when (currentTab) {
                                NavDest.DASHBOARD -> ScheduleScreen(
                                    me = me!!,
                                    schedules = schedules,
                                    onRefresh = {
                                        refreshSchedules(repo, scope, loadingSetter = { loading = it }) { schedules = it }
                                    },
                                    onLogout = {
                                        scope.launch { configStore.clear() }
                                        config = null; me = null; schedules = emptyList(); repo = null; error = null
                                    }
                                )
                                NavDest.AVAILABILITY -> AvailabilityScreen(
                                    month = selectedMonth.toString(),
                                    days = availabilityEntries,
                                    shifts = shifts,
                                    onMonthChange = { ym -> selectedMonth = ym },
                                    onSubmit = { entries ->
                                        scope.launch {
                                            loading = true
                                            val body = AvailabilityUpsertRequest(location_id = me!!.locations.firstOrNull()?.id ?: 0, entries = entries)
                                            repo?.upsertAvailability(body)
                                            loading = false
                                            availabilityStatus = "Opgeslagen"
                                        }
                                    },
                                    loading = loading,
                                    status = availabilityStatus
                                )
                                NavDest.SICK -> SickScreen(
                                    teamPhone = teamPhone,
                                    onSubmit = { reason ->
                                        scope.launch {
                                            loading = true
                                            repo?.reportSick(reason)
                                            loading = false
                                        }
                                    },
                                    loading = loading,
                                    status = error
                                )
                                NavDest.NOTIFICATIONS -> NotificationsScreen(
                                    notifications = notifications,
                                    onRefresh = {
                                        scope.launch {
                                            repo?.fetchNotifications()?.onSuccess { notifications = it }
                                        }
                                    },
                                    onMarkRead = { id ->
                                        scope.launch {
                                            repo?.markNotificationRead(id)
                                            repo?.fetchNotifications()?.onSuccess { notifications = it }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun connectAndLoad(
        cfg: AuthConfig,
        snackbarHostState: SnackbarHostState,
        onRepo: (RoosterPlannerRepository) -> Unit,
        onMe: (MeResponse) -> Unit,
        onSchedules: (List<ScheduleItem>) -> Unit,
        onLoading: (Boolean) -> Unit,
        onError: (String?) -> Unit
    ) {
        val repo = RoosterPlannerRepository(ApiClient.build(cfg, enableLogging = true))
        onRepo(repo)
        val scope = lifecycleScope
        scope.launch {
            onLoading(true)
            onError(null)
            val meResult = repo.fetchMe()
            meResult.onSuccess { me ->
                onMe(me)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val start = LocalDate.now().format(formatter)
                val end = LocalDate.now().plusDays(21).format(formatter)
                val scheduleResult = repo.fetchSchedules(start, end)
                scheduleResult.onSuccess { onSchedules(it) }
                    .onFailure { onError(it.message) }
                
                val locId = me.locations.firstOrNull()?.id?.toLong() ?: 0L
                if (locId != 0L) {
                    repo.fetchShifts(locId).onSuccess { /* cached via LaunchedEffect */ }
                }
            }.onFailure { e ->
                onError(e.message ?: "Inloggen mislukt")
                snackbarHostState.showSnackbar("Verbinding mislukt: ${e.message}")
            }
            onLoading(false)
        }
    }
}

private fun iconFor(tab: NavDest) = when (tab) {
    NavDest.DASHBOARD -> Icons.Default.Schedule
    NavDest.AVAILABILITY -> Icons.Default.EventAvailable
    NavDest.SICK -> Icons.Default.MedicalInformation
    NavDest.NOTIFICATIONS -> Icons.Default.Notifications
}

private fun refreshSchedules(
    repo: RoosterPlannerRepository?,
    scope: kotlinx.coroutines.CoroutineScope,
    loadingSetter: (Boolean) -> Unit,
    onSchedules: (List<ScheduleItem>) -> Unit
) {
    val now = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val start = now.format(formatter)
    val end = now.plusDays(21).format(formatter)
    scope.launch {
        loadingSetter(true)
        val result = repo?.fetchSchedules(start, end)
        loadingSetter(false)
        result?.onSuccess { onSchedules(it) }
    }
}
