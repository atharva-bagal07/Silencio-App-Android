package com.example.silencio

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.silencio.alarm.AlarmVerificationJob
import com.example.silencio.alarm.CalendarObserverService
import com.example.silencio.data.prefs.SilencioPrefs
import com.example.silencio.data.repository.SilencioRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SilencioApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var prefs: SilencioPrefs
    @Inject
    lateinit var repository: SilencioRepository  // ADD

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        scheduleAlarmsIfReady()
        AlarmVerificationJob.schedule(this)
    }

    /**
     * Only schedule the worker if the user has completed
     * onboarding. No point polling calendar if permissions
     * haven't been granted yet.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleAlarmsIfReady() {
        applicationScope.launch {
            val isOnboarded = prefs.isOnboarded.first()
            val hasPermission = ContextCompat.checkSelfPermission(
                this@SilencioApp,
                android.Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED

            if (isOnboarded && hasPermission) {
                repository.getUpcomingMeetings()
                startForegroundService(
                    Intent(
                        this@SilencioApp,
                        CalendarObserverService::class.java
                    )
                )
            }
        }
    }
}

