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
    val month: String,
    val items: List<AvailabilityItem>
)

data class AvailabilityItem(
    val id: Long,
    val work_date: String,
    val is_available: Int,
    val shift_preference: Long?,
    val custom_start: String?,
    val custom_end: String?,
    val notes: String?
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
    val notes: String? = null
)

data class AvailabilityUpsertRequest(
    val location_id: Long,
    val entries: List<AvailabilityEntryRequest>
)

data class PushTokenRequest(
    val token: String,
    val platform: String = "android"
)

data class SickReportRequest(
    val reason: String? = null
)
