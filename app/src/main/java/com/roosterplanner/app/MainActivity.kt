package com.roosterplanner.app

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import okhttp3.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val configStore by lazy { ConfigStore(this) }
    private var keepSplashScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
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
                var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
                var currentTab by remember { mutableStateOf(NavDest.DASHBOARD) }
                var teamPhone by remember { mutableStateOf<String?>("+31638993687") }
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                val deviceId = remember { 
                    Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
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

                // SSE Chat Handler
                LaunchedEffect(config, currentTab) {
                    if (config != null && currentTab == NavDest.CHAT) {
                        withContext(Dispatchers.IO) {
                            // 1. Load history
                            val historyResult = repo?.fetchChatHistory()
                            val history = historyResult?.getOrNull() ?: emptyList()
                            withContext(Dispatchers.Main) {
                                chatMessages = history
                            }
                            
                            // 2. Start SSE loop
                            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                            val adapter = moshi.adapter(ChatMessage::class.java)
                            val client = ApiClient.getChatClient()
                            val baseUrl = if (config!!.baseUrl.endsWith("/")) config!!.baseUrl else "${config!!.baseUrl}/"

                            while (currentTab == NavDest.CHAT) {
                                val lastId = chatMessages.lastOrNull()?.id ?: 0L
                                val url = "${baseUrl}wp-json/roosterplanner/v1/chat/stream?last_id=$lastId"
                                
                                val request = Request.Builder()
                                    .url(url)
                                    .header("Accept", "text/event-stream")
                                    .build()

                                try {
                                    client.newCall(request).execute().use { response ->
                                        if (!response.isSuccessful) {
                                            delay(5000)
                                            return@use
                                        }
                                        
                                        response.body?.source()?.let { source ->
                                            while (!source.exhausted() && currentTab == NavDest.CHAT) {
                                                val line = source.readUtf8Line() ?: break
                                                if (line.startsWith("data: ")) {
                                                    val jsonData = line.removePrefix("data: ")
                                                    try {
                                                        adapter.fromJson(jsonData)?.let { msg ->
                                                            withContext(Dispatchers.Main) {
                                                                chatMessages = (chatMessages + msg)
                                                                    .distinctBy { it.id }
                                                                    .sortedBy { it.id }
                                                            }
                                                        }
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    delay(3000)
                                }
                            }
                        }
                    }
                }

                if (config == null) {
                    LoginScreen(
                        initialUrl = null,
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
                                NavDest.values().forEach { dest ->
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
                            val locationId = me?.locations?.firstOrNull()?.id?.toLong() ?: 0L

                            // Load data when tab changes
                            LaunchedEffect(currentTab, selectedMonth, locationId) {
                                if (currentTab == NavDest.AVAILABILITY && locationId != 0L) {
                                    availabilityStatus = null
                                    availabilityEntries = emptyList()
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
                                        onRefresh = {
                                            refreshSchedules(repo, scope, loadingSetter = { loading = it }) { schedules = it }
                                        },
                                        onDateRangeSelected = { start, end ->
                                            scope.launch {
                                                loading = true
                                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                                repo?.fetchSchedules(start.format(formatter), end.format(formatter))?.onSuccess {
                                                    schedules = it
                                                }
                                                loading = false
                                            }
                                        },
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
                                                val body = AvailabilityUpsertRequest(location_id = me!!.locations.firstOrNull()?.id ?: 0, entries = entries)
                                                repo?.upsertAvailability(body)
                                                loading = false
                                                availabilityStatus = "Opgeslagen"
                                            }
                                        },
                                        loading = loading,
                                        status = availabilityStatus
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
                                        onBack = { currentTab = NavDest.DASHBOARD }
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
    NavDest.PROFILE -> Icons.Default.Person
    NavDest.CHAT -> Icons.AutoMirrored.Filled.Chat
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
    val end = now.plusDays(30).format(formatter)
    scope.launch {
        loadingSetter(true)
        val result = repo?.fetchSchedules(start, end)
        loadingSetter(false)
        result?.onSuccess { onSchedules(it) }
    }
}
