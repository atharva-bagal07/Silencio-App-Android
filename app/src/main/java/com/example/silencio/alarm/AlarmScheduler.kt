package com.example.silencio.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.silencio.data.model.CalendarEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleMeeting(event: CalendarEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "No SCHEDULE_EXACT_ALARM permission")
                return
            }
        }
        val now = System.currentTimeMillis()
        if (event.startTime > now) {
            scheduleStart(event)
            scheduleEnd(event)
        } else {
            Log.d("AlarmScheduler", "Skipping '${event.title}' — already started")
        }
    }

    fun cancelMeeting(event: CalendarEvent) {
        cancelStart(event)
        cancelEnd(event)
        Log.d("AlarmScheduler", "Cancelled alarms for '${event.title}'")
    }

    fun cancelAll() {
        // Called on settings change / calendar deselect
        // We can't enumerate all alarms so this is best-effort
        // In practice, rescheduling overwrites existing ones
        Log.d("AlarmScheduler", "cancelAll called")
    }

    private fun scheduleStart(event: CalendarEvent) {
        val intent = startIntent(event)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            event.startTime,
            intent
        )
        Log.d(
            "AlarmScheduler",
            "Scheduled START for '${event.title}' at ${java.util.Date(event.startTime)}"
        )
    }

    private fun scheduleEnd(event: CalendarEvent) {
        val intent = endIntent(event)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            event.endTime,
            intent
        )
        Log.d(
            "AlarmScheduler",
            "Scheduled END for '${event.title}' at ${java.util.Date(event.endTime)}"
        )

    }

    private fun cancelStart(event: CalendarEvent) {
        alarmManager.cancel(startIntent(event))
    }

    private fun cancelEnd(event: CalendarEvent) {
        alarmManager.cancel(endIntent(event))
    }

    private fun startIntent(event: CalendarEvent): PendingIntent {
        val intent = Intent(context, MeetingStartReceiver::class.java).apply {
            action = "com.example.silencio.MEETING_START"
            putExtra("event_id", event.id)
            putExtra("event_title", event.title)
            putExtra("event_start", event.startTime)
            putExtra("event_end", event.endTime)
        }
        return PendingIntent.getBroadcast(
            context,
            // Unique request code per event start
            ("start_${event.id}").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun endIntent(event: CalendarEvent): PendingIntent {
        val intent = Intent(context, MeetingEndReceiver::class.java).apply {
            action = "com.example.silencio.MEETING_END"
            putExtra("event_id", event.id)
            putExtra("event_title", event.title)
            putExtra("event_start", event.startTime)
            putExtra("event_end", event.endTime)
        }
        return PendingIntent.getBroadcast(
            context,
            ("end_${event.id}").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}