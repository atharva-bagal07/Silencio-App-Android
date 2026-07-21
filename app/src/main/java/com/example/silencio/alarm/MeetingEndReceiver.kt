package com.example.silencio.alarm

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MeetingEndReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prefs: SilencioPrefs

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val eventTitle = intent.getStringExtra("event_title") ?: "Meeting"
        val eventStart = intent.getLongExtra("event_start", -1L)
        val eventEnd = intent.getLongExtra("event_end", -1L)

        Log.d("AlarmScheduler", "END received for '$eventTitle'")

        disableDnd(context)
        dismissOngoingNotification(context)
        context.startService(Intent(context, CalendarObserverService::class.java))

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val count = prefs.notificationsHeldCount.first()
                showSummaryNotification(context, eventTitle, eventStart, eventEnd, count)
                prefs.setActiveEventId(null)
                prefs.setSilenceStartTime(null)
                prefs.resetNotificationsHeld()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun disableDnd(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            Log.d("AlarmScheduler", "DND disabled")
        } else {
            Log.e("AlarmScheduler", "No DND permission")
        }
    }

    private fun dismissOngoingNotification(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(MeetingStartReceiver.NOTIFICATION_ID)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showSummaryNotification(
        context: Context,
        title: String,
        startTime: Long,
        endTime: Long,
        notificationsHeld: Long
    ) {
        val nm = context.getSystemService(NotificationManager::class.java)

        val channel = android.app.NotificationChannel(
            MeetingStartReceiver.CHANNEL_ID,
            "Meeting Protection",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(channel)

        val durationMinutes = ((endTime - startTime) / 1000 / 60).toInt()
        val durationText = when {
            durationMinutes < 60 -> "${durationMinutes}m silent"
            durationMinutes % 60 == 0 -> "${durationMinutes / 60}h silent"
            else -> "${durationMinutes / 60}h ${durationMinutes % 60}m silent"
        }

        val notifText = if (notificationsHeld > 0) {
            "$durationText · $notificationsHeld notification${if (notificationsHeld > 1) "s" else ""} held · $title"
        } else {
            "$durationText · $title"
        }

        val notification = NotificationCompat.Builder(context, MeetingStartReceiver.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wave)
            .setContentTitle("Meeting ended")
            .setContentText(notifText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(MeetingStartReceiver.NOTIFICATION_ID, notification)
    }
}