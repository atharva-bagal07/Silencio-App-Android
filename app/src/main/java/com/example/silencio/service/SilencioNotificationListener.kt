package com.example.silencio.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.silencio.data.prefs.SilencioPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SilencioNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var prefs: SilencioPrefs

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // ignore our own notifications
        if (sbn.packageName == packageName) return

        scope.launch {
            val activeEventId = prefs.activeEventId.first()
            // only count if a meeting is active
            if (activeEventId != null) {
                prefs.incrementNotificationsHeld()
                Log.d("NotificationListener", "Notification held from ${sbn.packageName}")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // nothing for now
    }
}