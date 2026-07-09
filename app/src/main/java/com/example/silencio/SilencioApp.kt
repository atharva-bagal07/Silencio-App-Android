package com.example.silencio

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.silencio.core.worker.CalendarWorker
import com.example.silencio.data.prefs.SilencioPrefs
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

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        scheduleWorkerIfReady()
    }

    /**
     * Only schedule the worker if the user has completed
     * onboarding. No point polling calendar if permissions
     * haven't been granted yet.
     */
    private fun scheduleWorkerIfReady() {
        applicationScope.launch {
            val isOnboarded = prefs.isOnboarded.first()
            val autoSilenceEnabled = prefs.autoSilenceEnabled.first()
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                this@SilencioApp,
                android.Manifest.permission.READ_CALENDAR
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (isOnboarded && autoSilenceEnabled && hasPermission) {
                CalendarWorker.schedule(this@SilencioApp)
            }
        }
    }
}