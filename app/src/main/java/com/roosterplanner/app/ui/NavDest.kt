package com.roosterplanner.app.ui

enum class NavDest(val route: String, val label: String, val icon: String) {
    DASHBOARD("dashboard", "Rooster", "schedule"),
    AVAILABILITY("availability", "Beschikb.", "event_available"),
    SICK("sick", "Ziek", "medical_information"),
    NOTIFICATIONS("notifications", "Meldingen", "notifications")
}
