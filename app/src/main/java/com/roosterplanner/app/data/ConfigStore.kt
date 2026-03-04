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
    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val KEY_TEAM_PHONE = stringPreferencesKey("team_phone")

    val config: Flow<AuthConfig?> = context.dataStore.data.map { prefs ->
        val url = prefs[KEY_URL]
        val user = prefs[KEY_USER]
        val access = prefs[KEY_ACCESS_TOKEN]
        val refresh = prefs[KEY_REFRESH_TOKEN]
        if (url != null && user != null) {
            AuthConfig(url, user, access, refresh)
        } else null
    }

    val teamPhone: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_TEAM_PHONE] }

    suspend fun save(config: AuthConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_URL] = config.baseUrl
            prefs[KEY_USER] = config.username
            config.accessToken?.let { prefs[KEY_ACCESS_TOKEN] = it }
            config.refreshToken?.let { prefs[KEY_REFRESH_TOKEN] = it }
        }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
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
