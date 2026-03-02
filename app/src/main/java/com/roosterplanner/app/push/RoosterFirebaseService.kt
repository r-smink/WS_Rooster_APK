package com.roosterplanner.app.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.roosterplanner.app.data.ApiClient
import com.roosterplanner.app.data.AuthConfig
import com.roosterplanner.app.data.RoosterPlannerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class RoosterFirebaseService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("RoosterFCM", "New token: $token")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Rooster Planner"
        val body = message.notification?.body ?: message.data["message"] ?: ""
        val url = message.data["url"] ?: ""
        LocalNotifier.show(this, title, body, url)
    }

    private fun sendTokenToServer(token: String) {
        val appContext = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val store = com.roosterplanner.app.data.ConfigStore(appContext)
            val cfg = store.config.firstOrNull()
            if (cfg != null) {
                try {
                    val api = ApiClient.build(cfg)
                    val repo = RoosterPlannerRepository(api)
                    repo.registerMobileToken(token, platform = "android")
                } catch (e: Exception) {
                    Log.w("RoosterFCM", "Failed to send token", e)
                }
            }
        }
    }
}
