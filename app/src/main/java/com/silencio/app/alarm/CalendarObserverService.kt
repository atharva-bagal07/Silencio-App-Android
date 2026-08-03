package com.silencio.app.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.silencio.app.R
import com.silencio.app.data.repository.SilencioRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CalendarObserverService : Service() {

    @Inject
    lateinit var repository: SilencioRepository

    private val scope = CoroutineScope(Dispatchers.IO)
    private var debounceJob: Job? = null

    private val calendarObserver = object : ContentObserver(
        Handler(Looper.getMainLooper())
    ) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d("CalendarObserver", "Calendar changed — rescheduling alarms")
            // Debounce — calendar fires multiple onChange events per change
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(3_000)
                repository.getUpcomingMeetings()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            stopSelf()
            return
        }

        contentResolver.registerContentObserver(
            CalendarContract.Events.CONTENT_URI,
            true,
            calendarObserver
        )
        Log.d("CalendarObserver", "Service started — watching calendar")
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): android.app.Notification {
        val channelId = "silencio_observer"
        val nm = getSystemService(NotificationManager::class.java)
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Silencio",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_wave)
            .setContentText("Watching for upcoming meetings")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 2001
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(calendarObserver)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


