package com.roosterplanner.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.roosterplanner.app.data.*
import com.roosterplanner.app.ui.NavDest
import com.roosterplanner.app.ui.screens.*
import com.roosterplanner.app.ui.theme.RoosterPlannerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val configStore by lazy { ConfigStore(this) }
    private var keepSplashScreen = true
    
    // State to handle navigation from notifications
    private var pendingTargetUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        super.onCreate(savedInstanceState)
        
        // Check for target_url in starting intent
        handleIntent(intent)

        setContent {
            RoosterPlannerTheme {
                val context = LocalContext.current
                val focusManager = LocalFocusManager.current
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
                var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
                var currentTab by remember { mutableStateOf(NavDest.DASHBOARD) }
                var teamPhoneContacts by remember { mutableStateOf<List<TeamPhoneContact>>(emptyList()) }
                var chatError by remember { mutableStateOf<String?>(null) }
                var swapMyShifts by remember { mutableStateOf<List<SwapShift>>(emptyList()) }
                var swapAvailable by remember { mutableStateOf<List<SwapShift>>(emptyList()) }
                var swapIncoming by remember { mutableStateOf<List<SwapRequestItem>>(emptyList()) }
                var swapMine by remember { mutableStateOf<List<SwapRequestItem>>(emptyList()) }
                var swapColleagues by remember { mutableStateOf<List<Colleague>>(emptyList()) }
                var swapStatus by remember { mutableStateOf<String?>(null) }
                var timeoffItems by remember { mutableStateOf<List<TimeoffItem>>(emptyList()) }
                var timeoffStatus by remember { mutableStateOf<String?>(null) }
                var fullSchedules by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }
                var monthStatus by remember { mutableStateOf<MonthStatusResponse?>(null) }
                var scheduleFilter by remember { mutableStateOf(ScheduleFilter.PERSONAL) }
                var selectedLocationId by remember { mutableStateOf<Long?>(null) }
                var approvals by remember { mutableStateOf<ApprovalsResponse?>(null) }
                var approvalsStatus by remember { mutableStateOf<String?>(null) }
                var chatReloadKey by remember { mutableStateOf(0) }
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                val deviceId = remember { 
                    Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                }

                // Handle deep linking / notification clicks
                LaunchedEffect(pendingTargetUrl) {
                    pendingTargetUrl?.let { url ->
                        when {
                            url.contains("teamleider") -> currentTab = NavDest.TEAMLEADER
                            url.contains("/chat") -> currentTab = NavDest.CHAT
                            url.contains("/notifications") -> currentTab = NavDest.NOTIFICATIONS
                            url.contains("/schedule") -> currentTab = NavDest.SCHEDULE
                            url.contains("/availability") -> currentTab = NavDest.AVAILABILITY
                            url.contains("/ruilen") || url.contains("/swap") -> currentTab = NavDest.SWAPS
                            url.contains("/verlof") || url.contains("/timeoff") -> currentTab = NavDest.TIMEOFF
                        }
                        pendingTargetUrl = null
                    }
                }

                // Permission launcher for notifications (Android 13+)
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        Log.d("MainActivity", "Notification permission granted")
                    }
                }

                // Restore saved config and setup API
                LaunchedEffect(Unit) {
                    configStore.config.collect { stored ->
                        config = stored
                        if (stored != null) {
                            ApiClient.setToken(stored.accessToken)
                            val api = ApiClient.build(stored.baseUrl, true) {
                                scope.launch {
                                    val refreshResult = repo?.refreshTokens(RefreshRequest(stored.refreshToken ?: "", deviceId))
                                    refreshResult?.onSuccess { response ->
                                        ApiClient.setToken(response.access_token)
                                        configStore.saveTokens(response.access_token, response.refresh_token)
                                    }?.onFailure {
                                        configStore.clear()
                                        config = null
                                    }
                                }
                            }
                            repo = RoosterPlannerRepository(api)
                            
                            if (me == null) {
                                loading = true
                                repo?.fetchMe()?.onSuccess { 
                                    me = it
                                    loadInitialData(repo!!, { schedules = it }, { shifts = it }, { loading = it })

                                    // Fetch team phone numbers from API
                                    repo?.fetchTeamPhone()?.onSuccess { phoneResponse ->
                                        teamPhoneContacts = phoneResponse.contacts
                                    }

                                    // Check permission after successful data load (if user is logged in)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                }?.onFailure {
                                    error = "Sessie verlopen, log opnieuw in."
                                }
                                loading = false
                            }
                        }
                        delay(1000)
                        keepSplashScreen = false
                    }
                }

                // Chat Polling Handler
                LaunchedEffect(config, currentTab, chatReloadKey) {
                    if (config != null && currentTab == NavDest.CHAT) {
                        chatError = null
                        // Initial load
                        val historyResult = repo?.fetchChatHistory()
                        if (historyResult?.isSuccess == true) {
                            chatMessages = historyResult.getOrNull() ?: emptyList()
                        } else {
                            chatError = "Kon chatberichten niet laden"
                        }

                        // Poll loop
                        while (currentTab == NavDest.CHAT) {
                            delay(5000)
                            val lastId = chatMessages.lastOrNull()?.id ?: 0L
                            val pollResult = repo?.fetchChatHistory(lastId)
                            if (pollResult?.isSuccess == true) {
                                val newMessages = pollResult.getOrNull() ?: emptyList()
                                if (newMessages.isNotEmpty()) {
                                    chatMessages = (chatMessages + newMessages)
                                        .distinctBy { it.id }
                                        .sortedBy { it.id }
                                }
                                chatError = null
                            }
                        }
                    }
                }

                if (config == null) {
                    LoginScreen(
                        loading = loading,
                        error = error,
                        onLogin = { url, username, password ->
                            scope.launch {
                                loading = true
                                error = null
                                val tempApi = ApiClient.build(url, true)
                                val tempRepo = RoosterPlannerRepository(tempApi)
                                val loginResult = tempRepo.login(LoginRequest(username, password, deviceId))
                                
                                loginResult.onSuccess { response ->
                                    val newConfig = AuthConfig(url, username, response.access_token, response.refresh_token)
                                    configStore.save(newConfig)
                                    ApiClient.setToken(response.access_token)
                                    
                                    // Register FCM Token
                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val token = task.result
                                            scope.launch {
                                                tempRepo.registerMobileToken(token, "android")
                                            }
                                        }
                                    }
                                    
                                    // Ask for notification permission after login on Android 13+
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }

                                }.onFailure {
                                    error = "Inloggen mislukt: ${it.message}"
                                }
                                loading = false
                            }
                        }
                    )
                } else if (me != null) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "WerkRooster",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = me?.user?.display_name ?: "",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = me?.user?.email ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))
                                NavDest.values().filter { it != NavDest.TEAMLEADER || me?.employee?.is_admin == true }.forEach { dest ->
                                    NavigationDrawerItem(
                                        label = { Text(dest.label) },
                                        selected = currentTab == dest,
                                        onClick = {
                                            currentTab = dest
                                            scope.launch { drawerState.close() }
                                        },
                                        icon = { Icon(iconFor(dest), contentDescription = null) },
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                NavigationDrawerItem(
                                    label = { Text("Log uit") },
                                    selected = false,
                                    onClick = {
                                        scope.launch {
                                            repo?.logout(LogoutRequest(config?.refreshToken ?: ""))
                                            drawerState.close()
                                            configStore.clear()
                                            config = null
                                            me = null
                                            schedules = emptyList()
                                            repo = null
                                            error = null
                                        }
                                    },
                                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                TopAppBar(
                                    title = { Text(currentTab.label) },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    }
                                )
                            }
                        ) { padding ->
                            val hasLocations = !me?.locations.isNullOrEmpty()

                            // Load data when tab changes
                            LaunchedEffect(currentTab, selectedMonth, hasLocations) {
                                if (currentTab == NavDest.AVAILABILITY && hasLocations) {
                                    availabilityStatus = null
                                    availabilityEntries = emptyList()
                                    loading = true
                                    // locationId=null → server aggregates across all assigned locations
                                    repo?.fetchAvailability(selectedMonth.toString(), null)?.onSuccess { response ->
                                        availabilityEntries = response.items.map { item ->
                                            AvailabilityEntryRequest(
                                                date = item.work_date,
                                                is_available = item.is_available == 1,
                                                shift_preference = item.shift_preference,
                                                custom_start = item.custom_start,
                                                custom_end = item.custom_end,
                                                notes = item.notes,
                                                id = item.id
                                            )
                                        }
                                        // Use shifts from the aggregated response if available
                                        val responseShifts = response.shifts?.map { s ->
                                            Shift(
                                                id = s.id,
                                                location_id = s.location_id,
                                                name = s.name,
                                                start_time = s.start_time,
                                                end_time = s.end_time,
                                                color = s.color
                                            )
                                        } ?: emptyList()
                                        if (responseShifts.isNotEmpty()) shifts = responseShifts
                                    }?.onFailure {
                                        availabilityStatus = "Kon beschikbaarheid niet laden"
                                    }
                                    loading = false
                                }
                                if (currentTab == NavDest.NOTIFICATIONS) {
                                    repo?.fetchNotifications()?.onSuccess { notifications = it }
                                }
                                if (currentTab == NavDest.SWAPS) {
                                    swapStatus = null
                                    repo?.fetchMyShiftsForSwap()?.onSuccess { swapMyShifts = it.shifts }
                                    repo?.fetchAvailableShifts()?.onSuccess { swapAvailable = it.shifts }
                                    repo?.fetchSwapRequests()?.onSuccess {
                                        swapIncoming = it.incoming
                                        swapMine = it.mine
                                    }
                                    repo?.fetchColleagues()?.onSuccess { swapColleagues = it.colleagues }
                                }
                                if (currentTab == NavDest.TIMEOFF) {
                                    timeoffStatus = null
                                    repo?.fetchTimeoff()?.onSuccess { timeoffItems = it.items }
                                }
                                if (currentTab == NavDest.TEAMLEADER) {
                                    approvalsStatus = null
                                    repo?.fetchApprovals()?.onSuccess { approvals = it }
                                        ?.onFailure { approvalsStatus = it.apiMessage() ?: "Kon verzoeken niet laden" }
                                }
                            }

                            // Schedule: fetch data based on filter when month/filter/location changes
                            LaunchedEffect(currentTab, selectedMonth, scheduleFilter, selectedLocationId, me?.locations) {
                                if (currentTab == NavDest.SCHEDULE && me != null) {
                                    if (scheduleFilter == ScheduleFilter.FULL) {
                                        val locId = selectedLocationId ?: me?.locations?.firstOrNull()?.id
                                        refreshFullSchedule(
                                            repo, scope, selectedMonth, locId,
                                            { loading = it }, { fullSchedules = it }, { monthStatus = it }
                                        )
                                    } else {
                                        scope.launch {
                                            loading = true
                                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                            val start = selectedMonth.atDay(1).format(formatter)
                                            val end = selectedMonth.atEndOfMonth().format(formatter)
                                            repo?.fetchSchedules(start, end)?.onSuccess { schedules = it }
                                            loading = false
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .padding(padding)
                                    .pointerInput(Unit) {
                                        detectTapGestures { focusManager.clearFocus() }
                                    }
                            ) {
                                when (currentTab) {
                                    NavDest.DASHBOARD -> {
                                        val nextShift = schedules.firstOrNull { 
                                            runCatching { LocalDate.parse(it.work_date) }.getOrElse { LocalDate.MIN }.isAfter(LocalDate.now().minusDays(1))
                                        }
                                        DashboardScreen(
                                            me = me!!,
                                            nextShift = nextShift,
                                            onNavigate = { dest -> currentTab = dest },
                                            onLogout = {
                                                scope.launch {
                                                    repo?.logout(LogoutRequest(config?.refreshToken ?: ""))
                                                    configStore.clear()
                                                    config = null; me = null; schedules = emptyList(); repo = null; error = null
                                                }
                                            }
                                        )
                                    }
                                    NavDest.SCHEDULE -> ScheduleScreen(
                                        me = me!!,
                                        schedules = schedules,
                                        fullSchedules = fullSchedules,
                                        monthStatus = monthStatus,
                                        scheduleFilter = scheduleFilter,
                                        selectedLocationId = selectedLocationId ?: me?.locations?.firstOrNull()?.id,
                                        onFilterChange = { newFilter ->
                                            scheduleFilter = newFilter
                                            if (newFilter == ScheduleFilter.FULL && selectedLocationId == null) {
                                                selectedLocationId = me?.locations?.firstOrNull()?.id
                                            }
                                        },
                                        onLocationSelected = { locId -> selectedLocationId = locId },
                                        onRefresh = {
                                            if (scheduleFilter == ScheduleFilter.FULL) {
                                                refreshFullSchedule(
                                                    repo, scope, selectedMonth,
                                                    selectedLocationId ?: me?.locations?.firstOrNull()?.id,
                                                    { loading = it }, { fullSchedules = it }, { monthStatus = it }
                                                )
                                            } else {
                                                refreshSchedules(repo, scope, loadingSetter = { loading = it }) { schedules = it }
                                            }
                                        },
                                        onMonthChange = { ym -> selectedMonth = ym },
                                        currentMonth = selectedMonth,
                                        onBack = { currentTab = NavDest.DASHBOARD }
                                    )
                                    NavDest.AVAILABILITY -> AvailabilityScreen(
                                        month = selectedMonth.toString(),
                                        days = availabilityEntries,
                                        shifts = shifts,
                                        onMonthChange = { ym -> selectedMonth = ym },
                                        onSubmit = { entries ->
                                            scope.launch {
                                                loading = true
                                                availabilityStatus = null
                                                // location_id=null → server writes to all assigned locations
                                                val body = AvailabilityUpsertRequest(location_id = null, entries = entries)
                                                val result = repo?.upsertAvailability(body)
                                                if (result?.isSuccess == true) {
                                                    // Refresh availability data from server
                                                    repo?.fetchAvailability(selectedMonth.toString(), null)?.onSuccess { response ->
                                                        availabilityEntries = response.items.map { item ->
                                                            AvailabilityEntryRequest(
                                                                date = item.work_date,
                                                                is_available = item.is_available == 1,
                                                                shift_preference = item.shift_preference,
                                                                custom_start = item.custom_start,
                                                                custom_end = item.custom_end,
                                                                notes = item.notes,
                                                                id = item.id
                                                            )
                                                        }
                                                        availabilityStatus = "Opgeslagen"
                                                    }?.onFailure {
                                                        availabilityStatus = "Opgeslagen, maar kon niet verversen"
                                                    }
                                                } else {
                                                    availabilityStatus = "Opslaan mislukt"
                                                }
                                                loading = false
                                            }
                                        },
                                        loading = loading,
                                        status = availabilityStatus
                                    )
                                    NavDest.SWAPS -> SwapScreen(
                                        myShifts = swapMyShifts,
                                        availableShifts = swapAvailable,
                                        incomingRequests = swapIncoming,
                                        myRequests = swapMine,
                                        colleagues = swapColleagues,
                                        loading = loading,
                                        status = swapStatus,
                                        onRefresh = {
                                            scope.launch {
                                                repo?.fetchMyShiftsForSwap()?.onSuccess { swapMyShifts = it.shifts }
                                                repo?.fetchAvailableShifts()?.onSuccess { swapAvailable = it.shifts }
                                                repo?.fetchSwapRequests()?.onSuccess {
                                                    swapIncoming = it.incoming
                                                    swapMine = it.mine
                                                }
                                                repo?.fetchColleagues()?.onSuccess { swapColleagues = it.colleagues }
                                            }
                                        },
                                        onToggleSwappable = { scheduleId ->
                                            scope.launch {
                                                val result = repo?.toggleSwappable(scheduleId)
                                                if (result?.isSuccess == true) {
                                                    swapStatus = result.getOrNull()?.message
                                                    repo?.fetchMyShiftsForSwap()?.onSuccess { swapMyShifts = it.shifts }
                                                    repo?.fetchAvailableShifts()?.onSuccess { swapAvailable = it.shifts }
                                                } else {
                                                    swapStatus = "Kon ruilbaar-status niet wijzigen"
                                                }
                                            }
                                        },
                                        onRequestSwap = { scheduleId, colleagueId, reason, partStart, partEnd ->
                                            scope.launch {
                                                val result = repo?.requestSwap(scheduleId, colleagueId, reason, partStart, partEnd)
                                                if (result?.isSuccess == true) {
                                                    swapStatus = if (colleagueId == null) "Dienst aangeboden aan je collega's" else "Ruilverzoek verstuurd"
                                                    repo?.fetchSwapRequests()?.onSuccess {
                                                        swapIncoming = it.incoming
                                                        swapMine = it.mine
                                                    }
                                                } else {
                                                    swapStatus = result?.exceptionOrNull()?.apiMessage() ?: "Ruilverzoek indienen mislukt"
                                                }
                                            }
                                        },
                                        onRespondSwap = { swapId, action ->
                                            scope.launch {
                                                val result = repo?.respondSwap(swapId, action)
                                                if (result?.isSuccess == true) {
                                                    swapStatus = if (action == "accept") "Geaccepteerd, wacht op goedkeuring van de teamleider" else "Verzoek afgewezen"
                                                    repo?.fetchSwapRequests()?.onSuccess {
                                                        swapIncoming = it.incoming
                                                        swapMine = it.mine
                                                    }
                                                } else {
                                                    swapStatus = result?.exceptionOrNull()?.apiMessage() ?: "Kon niet reageren op verzoek"
                                                }
                                            }
                                        },
                                        onClaimShift = { scheduleId ->
                                            scope.launch {
                                                val result = repo?.claimShift(scheduleId)
                                                if (result?.isSuccess == true) {
                                                    swapStatus = "Aangevraagd, wacht op goedkeuring van de teamleider"
                                                    repo?.fetchAvailableShifts()?.onSuccess { swapAvailable = it.shifts }
                                                    repo?.fetchMyShiftsForSwap()?.onSuccess { swapMyShifts = it.shifts }
                                                } else {
                                                    swapStatus = result?.exceptionOrNull()?.apiMessage() ?: "Dienst overnemen mislukt"
                                                }
                                            }
                                        },
                                        onBack = { currentTab = NavDest.DASHBOARD }
                                    )
                                    NavDest.TIMEOFF -> TimeoffScreen(
                                        items = timeoffItems,
                                        loading = loading,
                                        status = timeoffStatus,
                                        onRefresh = {
                                            scope.launch {
                                                repo?.fetchTimeoff()?.onSuccess { timeoffItems = it.items }
                                            }
                                        },
                                        onSubmit = { startDate, endDate, type, reason ->
                                            scope.launch {
                                                loading = true
                                                val result = repo?.submitTimeoff(startDate, endDate, type, reason)
                                                if (result?.isSuccess == true) {
                                                    timeoffStatus = "Aanvraag verzonden!"
                                                    repo?.fetchTimeoff()?.onSuccess { timeoffItems = it.items }
                                                } else {
                                                    timeoffStatus = "Aanvraag indienen mislukt"
                                                }
                                                loading = false
                                            }
                                        },
                                        onBack = { currentTab = NavDest.DASHBOARD }
                                    )
                                    NavDest.PROFILE -> ProfileScreen(
                                        me = me!!,
                                        onBack = { currentTab = NavDest.DASHBOARD }
                                    )
                                    NavDest.CHAT -> ChatScreen(
                                        messages = chatMessages,
                                        currentUserId = me?.user?.id ?: 0L,
                                        onSendMessage = { text ->
                                            scope.launch {
                                                repo?.sendChatMessage(text)?.onSuccess { msg ->
                                                    chatMessages = (chatMessages + msg).distinctBy { it.id }.sortedBy { it.id }
                                                }
                                            }
                                        },
                                        onBack = { currentTab = NavDest.DASHBOARD },
                                        error = chatError,
                                        onRefresh = { chatReloadKey++ }
                                    )
                                    NavDest.SICK -> SickScreen(
                                        teamPhoneContacts = teamPhoneContacts,
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
                                    NavDest.TEAMLEADER -> TeamleaderScreen(
                                        approvals = approvals,
                                        status = approvalsStatus,
                                        onRefresh = {
                                            scope.launch {
                                                repo?.fetchApprovals()?.onSuccess { approvals = it }
                                            }
                                        },
                                        onDecideSwap = { id, approve, notes ->
                                            scope.launch {
                                                val result = repo?.decideSwap(id, approve, notes)
                                                approvalsStatus = if (result?.isSuccess == true) {
                                                    if (approve) "Ruil goedgekeurd, het rooster is bijgewerkt" else "Ruilverzoek afgewezen"
                                                } else {
                                                    result?.exceptionOrNull()?.apiMessage() ?: "Kon ruilverzoek niet verwerken"
                                                }
                                                repo?.fetchApprovals()?.onSuccess { approvals = it }
                                            }
                                        },
                                        onDecideTimeoff = { id, approve, notes ->
                                            scope.launch {
                                                val result = repo?.decideTimeoff(id, approve, notes)
                                                approvalsStatus = if (result?.isSuccess == true) {
                                                    if (approve) "Verlof goedgekeurd" else "Verlof afgewezen"
                                                } else {
                                                    result?.exceptionOrNull()?.apiMessage() ?: "Kon verlofaanvraag niet verwerken"
                                                }
                                                repo?.fetchApprovals()?.onSuccess { approvals = it }
                                            }
                                        }
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("target_url")?.let { url ->
            pendingTargetUrl = url
        }
    }

    private fun loadInitialData(
        repo: RoosterPlannerRepository,
        onSchedules: (List<ScheduleItem>) -> Unit,
        onShifts: (List<Shift>) -> Unit,
        onLoading: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            onLoading(true)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val start = LocalDate.now().format(formatter)
            val end = LocalDate.now().plusDays(30).format(formatter)
            
            repo.fetchSchedules(start, end).onSuccess { onSchedules(it) }
            
            repo.fetchMe().onSuccess { me ->
                val locId = me.locations.firstOrNull()?.id ?: 0L
                if (locId != 0L) {
                    repo.fetchShifts(locId).onSuccess { onShifts(it) }
                }
            }
            onLoading(false)
        }
    }
}

private fun iconFor(tab: NavDest) = when (tab) {
    NavDest.DASHBOARD -> Icons.Default.Dashboard
    NavDest.SCHEDULE -> Icons.Default.Schedule
    NavDest.AVAILABILITY -> Icons.Default.EventAvailable
    NavDest.SWAPS -> Icons.Default.SwapHoriz
    NavDest.TIMEOFF -> Icons.Default.BeachAccess
    NavDest.PROFILE -> Icons.Default.Person
    NavDest.CHAT -> Icons.AutoMirrored.Filled.Chat
    NavDest.SICK -> Icons.Default.MedicalInformation
    NavDest.NOTIFICATIONS -> Icons.Default.Notifications
    NavDest.TEAMLEADER -> Icons.Default.AssignmentTurnedIn
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
    val end = now.plusDays(30).format(formatter)
    scope.launch {
        loadingSetter(true)
        val result = repo?.fetchSchedules(start, end)
        loadingSetter(false)
        result?.onSuccess { onSchedules(it) }
    }
}

private fun refreshFullSchedule(
    repo: RoosterPlannerRepository?,
    scope: kotlinx.coroutines.CoroutineScope,
    month: java.time.YearMonth,
    locationId: Long?,
    loadingSetter: (Boolean) -> Unit,
    onFullSchedules: (List<ScheduleItem>) -> Unit,
    onMonthStatus: (MonthStatusResponse?) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val start = month.atDay(1).format(formatter)
    val end = month.atEndOfMonth().format(formatter)
    scope.launch {
        loadingSetter(true)
        val statusResult = repo?.fetchMonthStatus(month.toString(), locationId)
        val status = statusResult?.getOrNull()
        onMonthStatus(status)
        if (status?.is_finalized == true) {
            val fullResult = repo?.fetchSchedulesFull(start, end, locationId)
            fullResult?.onSuccess { onFullSchedules(it) }
                ?.onFailure { onFullSchedules(emptyList()) }
        } else {
            onFullSchedules(emptyList())
        }
        loadingSetter(false)
    }
}
