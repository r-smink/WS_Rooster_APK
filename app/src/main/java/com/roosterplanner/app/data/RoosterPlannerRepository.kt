package com.roosterplanner.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoosterPlannerRepository(private val api: ApiService) {
    suspend fun fetchMe(): Result<MeResponse> = runApi { api.me() }

    suspend fun fetchSchedules(startDate: String, endDate: String): Result<List<ScheduleItem>> =
        runApi { api.schedules(startDate, endDate) }

    suspend fun fetchNotifications(limit: Int = 50, unreadOnly: Boolean = false): Result<List<NotificationItem>> =
        runApi { api.notifications(limit, if (unreadOnly) 1 else 0) }

    suspend fun markNotificationRead(id: Long): Result<Unit> = runApi {
        api.markNotificationRead(id)
        Unit
    }

    suspend fun fetchAvailability(month: String, locationId: Long): Result<AvailabilityResponse> =
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
