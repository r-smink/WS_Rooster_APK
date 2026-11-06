package com.roosterplanner.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

/** The "message" field of a WordPress REST error response, if any. */
fun Throwable.apiMessage(): String? {
    val body = (this as? HttpException)?.response()?.errorBody()?.string() ?: return null
    return runCatching { JSONObject(body).optString("message").takeIf { it.isNotBlank() } }.getOrNull()
}

class RoosterPlannerRepository(private var api: ApiService) {
    
    fun updateApi(newApi: ApiService) {
        this.api = newApi
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> = runApi {
        api.login(request)
    }

    suspend fun refreshTokens(request: RefreshRequest): Result<LoginResponse> = runApi {
        api.refresh(request)
    }

    suspend fun logout(request: LogoutRequest): Result<Unit> = runApi {
        api.logout(request)
    }

    suspend fun fetchMe(): Result<MeResponse> = runApi { api.me() }

    suspend fun fetchSchedules(startDate: String, endDate: String): Result<List<ScheduleItem>> =
        runApi { api.schedules(startDate, endDate) }

    suspend fun fetchMonthStatus(month: String, locationId: Long? = null): Result<MonthStatusResponse> =
        runApi { api.monthStatus(month, locationId) }

    suspend fun fetchSchedulesFull(startDate: String, endDate: String, locationId: Long? = null): Result<List<ScheduleItem>> =
        runApi { api.schedulesFull(startDate, endDate, locationId) }

    suspend fun fetchNotifications(limit: Int = 50, unreadOnly: Boolean = false): Result<List<NotificationItem>> =
        runApi { api.notifications(limit, if (unreadOnly) 1 else 0) }

    suspend fun markNotificationRead(id: Long): Result<Unit> = runApi {
        api.markNotificationRead(id)
        Unit
    }

    suspend fun fetchAvailability(month: String, locationId: Long? = null): Result<AvailabilityResponse> =
        runApi { api.availability(month, locationId) }

    suspend fun upsertAvailability(body: AvailabilityUpsertRequest): Result<Unit> = runApi {
        api.upsertAvailability(body)
        Unit
    }

    suspend fun registerMobileToken(token: String, platform: String): Result<Unit> = runApi {
        api.registerPushToken(PushTokenRequest(token = token, platform = platform))
        Unit
    }

    suspend fun reportSick(reason: String?): Result<Unit> = runApi {
        api.reportSick(SickReportRequest(reason))
        Unit
    }

    suspend fun fetchShifts(locationId: Long): Result<List<Shift>> = runApi {
        api.shifts(locationId)
    }

    suspend fun fetchChatHistory(lastId: Long? = null, limit: Int = 50): Result<List<ChatMessage>> = runApi {
        api.getChatHistory(lastId, limit)
    }

    suspend fun sendChatMessage(message: String, locationId: Long? = null, isAnnouncement: Boolean = false): Result<ChatMessage> = runApi {
        api.sendChatMessage(SendMessageRequest(message, locationId, if (isAnnouncement) 1 else 0))
    }

    suspend fun fetchTeamPhone(): Result<TeamPhoneResponse> = runApi {
        api.teamPhone()
    }

    // ---- Shift swaps ----

    suspend fun fetchMyShiftsForSwap(): Result<MyShiftsResponse> = runApi { api.swapsMyShifts() }

    suspend fun fetchAvailableShifts(): Result<AvailableShiftsResponse> = runApi { api.swapsAvailable() }

    suspend fun fetchSwapRequests(): Result<SwapsRequestsResponse> = runApi { api.swapsRequests() }

    suspend fun fetchColleagues(): Result<ColleaguesResponse> = runApi { api.swapsColleagues() }

    suspend fun requestSwap(
        scheduleId: Long,
        requestedEmployeeId: Long? = null,
        reason: String? = null,
        partStart: String? = null,
        partEnd: String? = null
    ): Result<Unit> = runApi {
        api.requestSwap(RequestSwapBody(scheduleId, requestedEmployeeId, reason, partStart, partEnd))
        Unit
    }

    suspend fun respondSwap(swapId: Long, action: String): Result<Unit> = runApi {
        api.respondSwap(swapId, RespondSwapBody(action))
        Unit
    }

    suspend fun claimShift(scheduleId: Long): Result<Unit> = runApi {
        api.claimSwap(ClaimSwapBody(scheduleId))
        Unit
    }

    suspend fun toggleSwappable(scheduleId: Long): Result<ToggleSwappableResponse> = runApi {
        api.toggleSwappable(scheduleId)
    }

    // ---- Time off (verlof) ----

    suspend fun fetchTimeoff(): Result<TimeoffResponse> = runApi { api.timeoff() }

    suspend fun submitTimeoff(startDate: String, endDate: String, type: String, reason: String?): Result<Unit> = runApi {
        api.submitTimeoff(TimeoffRequest(startDate, endDate, type, reason))
        Unit
    }

    // ---- Teamleider approvals ----

    suspend fun fetchApprovals(): Result<ApprovalsResponse> = runApi { api.approvals() }

    suspend fun decideSwap(swapId: Long, approve: Boolean, notes: String? = null): Result<Unit> = runApi {
        api.decideSwap(swapId, DecisionBody(if (approve) "approve" else "reject", notes))
        Unit
    }

    suspend fun decideTimeoff(timeoffId: Long, approve: Boolean, notes: String? = null): Result<Unit> = runApi {
        api.decideTimeoff(timeoffId, DecisionBody(if (approve) "approve" else "reject", notes))
        Unit
    }

    private suspend fun <T> runApi(block: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(block())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
