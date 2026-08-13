package com.roosterplanner.app.data

data class AuthConfig(
    val baseUrl: String,
    val username: String,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

data class LoginRequest(
    val username: String,
    val password: String,
    val device_id: String? = null
)

data class LoginResponse(
    val access_token: String,
    val access_expires_in: Long,
    val refresh_token: String,
    val refresh_expires_in: Long,
    val user_id: Long
)

data class RefreshRequest(
    val refresh_token: String,
    val device_id: String? = null
)

data class LogoutRequest(
    val refresh_token: String
)

data class MeResponse(
    val user: User,
    val employee: Employee,
    val locations: List<Location>,
    val ical_url: String? = null
)

data class User(val id: Long, val display_name: String, val email: String)

data class Employee(
    val id: Long,
    val is_admin: Boolean,
    val is_fixed: Boolean,
    val theme_preference: String?,
    val email_notifications: Boolean,
    val push_notifications: Boolean
)

data class Location(val id: Long, val name: String, val address: String?)

data class ScheduleItem(
    val id: Long,
    val work_date: String,
    val start_time: String?,
    val end_time: String?,
    val status: String,
    val notes: String?,
    val is_swappable: Int,
    val actual_start_time: String?,
    val actual_end_time: String?,
    val break_minutes: Int?,
    val shift_id: Long?,
    val shift_name: String?,
    val color: String?,
    val location_id: Long?,
    val location_name: String?
)

data class AvailabilityResponse(
    val location_id: Long,
    val location_ids: List<Long>? = null,
    val month: String,
    val items: List<AvailabilityItem>,
    val shifts: List<AvailabilityShift>? = null
)

data class AvailabilityShift(
    val id: Long,
    val location_id: Long?,
    val name: String,
    val start_time: String?,
    val end_time: String?,
    val color: String?,
    val location_name: String? = null
)

data class AvailabilityItem(
    val id: Long,
    val work_date: String,
    val is_available: Int,
    val is_all_day: Int? = null,
    val shift_preference: Long?,
    val custom_start: String?,
    val custom_end: String?,
    val notes: String?,
    val submitted_at: String? = null
)

data class NotificationItem(
    val id: Long,
    val type: String,
    val title: String,
    val message: String?,
    val is_read: Int,
    val created_at: String
)

data class Shift(
    val id: Long,
    val location_id: Long?,
    val name: String,
    val start_time: String?,
    val end_time: String?,
    val color: String?
)

data class ChatMessage(
    val id: Long,
    val sender_id: Long,
    val sender_name: String,
    val message: String,
    val is_announcement: Int,
    val location_id: Long?,
    val created_at: String
)

data class SendMessageRequest(
    val message: String,
    val location_id: Long? = null,
    val is_announcement: Int = 0
)

// Request models

data class AvailabilityEntryRequest(
    val date: String,
    val is_available: Boolean,
    val shift_preference: Long? = null,
    val custom_start: String? = null,
    val custom_end: String? = null,
    val notes: String? = null,
    val id: Long? = null
)

data class TeamPhoneContact(
    val name: String,
    val phone: String
)

data class TeamPhoneResponse(
    val contacts: List<TeamPhoneContact> = emptyList()
)

data class AvailabilityUpsertRequest(
    val location_id: Long? = null,
    val entries: List<AvailabilityEntryRequest>
)

data class PushTokenRequest(
    val token: String,
    val platform: String = "android"
)

data class SickReportRequest(
    val reason: String? = null
)

// ---- Shift swap models ----

data class SwapShift(
    val id: Long,
    val work_date: String,
    val start_time: String?,
    val end_time: String?,
    val status: String,
    val is_swappable: Int,
    val notes: String?,
    val shift_name: String?,
    val color: String?,
    val location_id: Long?,
    val location_name: String?,
    val owner_name: String? = null
)

data class MyShiftsResponse(val shifts: List<SwapShift> = emptyList())
data class AvailableShiftsResponse(val shifts: List<SwapShift> = emptyList())

data class SwapRequestItem(
    val id: Long,
    val schedule_id: Long,
    val reason: String?,
    val status: String,
    val requested_at: String?,
    val responded_at: String?,
    val work_date: String?,
    val start_time: String?,
    val end_time: String?,
    val shift_name: String?,
    val location_id: Long?,
    val location_name: String?,
    val requester_name: String? = null,
    val requested_name: String? = null
)

data class SwapsRequestsResponse(
    val incoming: List<SwapRequestItem> = emptyList(),
    val mine: List<SwapRequestItem> = emptyList()
)

data class Colleague(
    val id: Long,
    val name: String
)

data class ColleaguesResponse(val colleagues: List<Colleague> = emptyList())

data class RequestSwapBody(
    val schedule_id: Long,
    val requested_employee_id: Long? = null,
    val reason: String? = null
)

data class RespondSwapBody(val action: String)

data class ClaimSwapBody(val schedule_id: Long)

data class ToggleSwappableResponse(
    val is_swappable: Int,
    val message: String? = null
)

// ---- Time off (verlof) models ----

data class TimeoffItem(
    val id: Long,
    val start_date: String,
    val end_date: String,
    val type: String,
    val status: String,
    val reason: String?,
    val admin_notes: String?,
    val requested_at: String?,
    val responded_at: String?,
    val days_requested: Int?
)

data class TimeoffResponse(val items: List<TimeoffItem> = emptyList())

data class TimeoffRequest(
    val start_date: String,
    val end_date: String,
    val type: String = "vacation",
    val reason: String? = null
)
