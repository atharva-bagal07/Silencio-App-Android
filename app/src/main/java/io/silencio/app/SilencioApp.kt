package io.silencio.app

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import io.silencio.app.alarm.AlarmVerificationJob
import io.silencio.app.alarm.CalendarObserverService
import io.silencio.app.data.prefs.SilencioPrefs
import io.silencio.app.data.repository.SilencioRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SilencioApp : Application() {

    @Inject
    lateinit var prefs: SilencioPrefs

    @Inject
    lateinit var repository: SilencioRepository

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        // TEMPORARY — remove before release
        applicationScope.launch {
            prefs.setPremium(false)
        }

        // RevenueCat init
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(
            PurchasesConfiguration.Builder(
                context = this,
                apiKey = BuildConfig.REVENUECAT_API_KEY
            ).build()
        )

        scheduleAlarmsIfReady()
        AlarmVerificationJob.schedule(this)
    }

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