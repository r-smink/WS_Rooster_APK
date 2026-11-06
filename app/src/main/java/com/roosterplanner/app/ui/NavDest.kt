package com.roosterplanner.app.ui

enum class NavDest(val route: String, val label: String) {
    DASHBOARD("dashboard", "Dashboard"),
    SCHEDULE("schedule", "Rooster"),
    AVAILABILITY("availability", "Beschikbaarheid"),
    SWAPS("swaps", "Ruilen"),
    TIMEOFF("timeoff", "Verlof"),
    PROFILE("profile", "Mijn Profiel"),
    CHAT("chat", "Chat"),
    SICK("sick", "Ziek melden"),
    NOTIFICATIONS("notifications", "Meldingen"),
    TEAMLEADER("teamleader", "Teamleider")
}
