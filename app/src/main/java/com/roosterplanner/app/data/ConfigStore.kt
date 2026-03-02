package com.roosterplanner.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "roosterplanner")

class ConfigStore(private val context: Context) {
    private val KEY_URL = stringPreferencesKey("base_url")
    private val KEY_USER = stringPreferencesKey("username")
    private val KEY_PASS = stringPreferencesKey("app_password")
    private val KEY_TEAM_PHONE = stringPreferencesKey("team_phone")

    val config: Flow<AuthConfig?> = context.dataStore.data.map { prefs ->
        val url = prefs[KEY_URL]
        val user = prefs[KEY_USER]
        val pass = prefs[KEY_PASS]
        if (url != null && user != null && pass != null) AuthConfig(url, user, pass) else null
    }

    val teamPhone: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_TEAM_PHONE] }

    suspend fun save(config: AuthConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_URL] = config.baseUrl
            prefs[KEY_USER] = config.username
            prefs[KEY_PASS] = config.appPassword
        }
    }

    suspend fun saveTeamPhone(phone: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TEAM_PHONE] = phone
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
