package com.example.silencio.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.silencio.R
import com.example.silencio.data.prefs.SilencioPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MeetingStartReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prefs: SilencioPrefs  // ADD

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val eventTitle = intent.getStringExtra("event_title") ?: "Meeting"
        val eventId = intent.getLongExtra("event_id", -1L)
        val eventEnd = intent.getLongExtra("event_end", -1L)
        val eventStart = intent.getLongExtra("event_start", -1L)

        Log.d("AlarmScheduler", "START received for '$eventTitle' id=$eventId")

        enableDnd(context)
        context.stopService(Intent(context, CalendarObserverService::class.java))
        showStartNotification(context, eventTitle, eventEnd)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                prefs.setActiveEventId(eventId)
                prefs.setSilenceStartTime(eventStart)
                prefs.resetNotificationsHeld()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun enableDnd(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            Log.d("AlarmScheduler", "DND enabled")
        } else {
            Log.e("AlarmScheduler", "No DND permission")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showStartNotification(
        context: Context,
        title: String,
        endTime: Long
    ) {
        val nm = context.getSystemService(NotificationManager::class.java)

        // Ensure channel exists
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Meeting Protection",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when Silencio activates DND for a meeting"
        }
        nm.createNotificationChannel(channel)

        val endFormatted = android.text.format.DateFormat
            .format("h:mm a", endTime).toString()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wave)
            .setContentTitle("Meeting protected")
            .setContentText("Silent until $endFormatted · $title")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)  // stays until end receiver dismisses it
            .setOnlyAlertOnce(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "silencio_meeting"
        const val NOTIFICATION_ID = 1001
    }
}