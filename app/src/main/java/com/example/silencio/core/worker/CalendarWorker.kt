package com.example.silencio.core.worker

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.silencio.core.calender.CalendarManager
import com.example.silencio.core.dnd.DndManager
import com.example.silencio.core.notification.NotificationHelper
import com.example.silencio.core.receiver.AlarmReceiver
import com.example.silencio.data.prefs.SilencioPrefs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class CalendarWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val calendarManager: CalendarManager,
    private val dndManager: DndManager,
    private val notificationHelper: NotificationHelper,
    private val prefs: SilencioPrefs
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "silencio_calendar_worker"
        private const val REPEAT_INTERVAL_MINUTES = 5L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CalendarWorker>(
                REPEAT_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        android.util.Log.d("SilencioWorker", "=== Worker fired ===")

        return try {
            val autoSilenceEnabled = prefs.autoSilenceEnabled.first()
            android.util.Log.d("SilencioWorker", "autoSilenceEnabled: $autoSilenceEnabled")

            if (!autoSilenceEnabled) {
                restoreIfNeeded()
                return Result.success()
            }

            val hasDnd = dndManager.hasDndPermission()
            android.util.Log.d("SilencioWorker", "hasDndPermission: $hasDnd")

            if (!hasDnd) {
                return Result.success()
            }

            val currentEvent = calendarManager.getCurrentEvent()
            android.util.Log.d("SilencioWorker", "currentEvent: ${currentEvent?.title}")

            val activeEventId = prefs.activeEventId.first()
            android.util.Log.d("SilencioWorker", "activeEventId: $activeEventId")

            when {
                currentEvent != null && activeEventId == null -> {
                    android.util.Log.d("SilencioWorker", "Starting silence for: ${currentEvent.title}")
                    startSilence(currentEvent.id, currentEvent.title)
                }
                currentEvent != null && activeEventId != null -> {
                    android.util.Log.d("SilencioWorker", "Already in meeting")
                }
                currentEvent == null && activeEventId != null -> {
                    android.util.Log.d("SilencioWorker", "Meeting ended, restoring")
                    endSilence()
                }
                else -> {
                    android.util.Log.d("SilencioWorker", "No meeting, not tracking")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val nextEvent = calendarManager.getNextEvent()
                android.util.Log.d("SilencioWorker", "nextEvent: ${nextEvent?.title}")
                if (nextEvent != null) {
                    AlarmReceiver.scheduleStartAlarm(
                        applicationContext,
                        nextEvent.startTime
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SilencioWorker", "EXCEPTION: ${e.message}", e)
            restoreIfNeeded()
            Result.retry()
        }
    }

    private suspend fun startSilence(eventId: Long, eventTitle: String) {
        val vibrateInstead = prefs.vibrateInstead.first()
        dndManager.silence(vibrateInstead)
        prefs.setActiveEventId(eventId)
        prefs.setSilenceStartTime(System.currentTimeMillis())
        prefs.resetNotificationsHeld()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun endSilence() {
        val silenceStartTime = prefs.silenceStartTime.first() ?: return
        val notificationsHeld = prefs.notificationsHeldCount.first()
        val durationMinutes = ((System.currentTimeMillis() - silenceStartTime)
                / 1000 / 60).toInt()

        dndManager.restore()

        notificationHelper.showPostMeetingNotification(
            durationMinutes = durationMinutes,
            notificationsHeld = notificationsHeld.toInt()
        )

        prefs.setActiveEventId(null)
        prefs.setSilenceStartTime(null)
        prefs.resetNotificationsHeld()
    }

    private suspend fun restoreIfNeeded() {
        if (dndManager.isCurrentlySilenced()) {
            dndManager.restore()
            prefs.setActiveEventId(null)
            prefs.setSilenceStartTime(null)
            prefs.resetNotificationsHeld()
        }
    }
}