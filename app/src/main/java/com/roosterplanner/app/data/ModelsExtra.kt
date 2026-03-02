package com.roosterplanner.app.data

data class PushTokenRequest(val token: String, val platform: String = "android")
data class SickReportRequest(val reason: String? = null)
