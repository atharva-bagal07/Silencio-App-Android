package com.example.silencio.core.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.silencio.core.calender.CalendarManager
import com.example.silencio.core.dnd.DndManager
import com.example.silencio.core.notification.NotificationHelper
import com.example.silencio.data.prefs.SilencioPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var calendarManager: CalendarManager

    @Inject
    lateinit var dndManager: DndManager

    @Inject
    lateinit var prefs: SilencioPrefs

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            val autoSilenceEnabled = prefs.autoSilenceEnabled.first()
            if (!autoSilenceEnabled) return@launch
            if (!dndManager.hasDndPermission()) return@launch

            val currentEvent = calendarManager.getCurrentEvent()
            if (currentEvent != null) {
                val vibrateInstead = prefs.vibrateInstead.first()
                dndManager.silence(vibrateInstead)
                prefs.setActiveEventId(currentEvent.id)
                prefs.setSilenceStartTime(System.currentTimeMillis())
                prefs.resetNotificationsHeld()

                // Schedule alarm for event end
                scheduleEndAlarm(context, currentEvent.endTime)
            } else {
                // End alarm fired — restore phone
                val silenceStartTime = prefs.silenceStartTime.first()
                val notificationsHeld = prefs.notificationsHeldCount.first()

                if (silenceStartTime != null) {
                    val durationMinutes = ((System.currentTimeMillis()
                            - silenceStartTime) / 1000 / 60).toInt()

                    dndManager.restore()

                    notificationHelper.showPostMeetingNotification(
                        durationMinutes = durationMinutes,
                        notificationsHeld = notificationsHeld.toInt()
                    )

                    prefs.setActiveEventId(null)
                    prefs.setSilenceStartTime(null)
                    prefs.resetNotificationsHeld()
                }
            }
        }
    }

    companion object {
        private const val START_REQUEST_CODE = 2001
        private const val END_REQUEST_CODE = 2002

        @RequiresApi(Build.VERSION_CODES.S)
        fun scheduleStartAlarm(context: Context, startTime: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE)
                    as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                START_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    startTime,
                    pendingIntent
                )
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        fun scheduleEndAlarm(context: Context, endTime: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE)
                    as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                END_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endTime,
                    pendingIntent
                )
            }
        }
    }
}