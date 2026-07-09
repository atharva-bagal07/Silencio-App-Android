package com.example.silencio.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.silencio.core.worker.CalendarWorker
import com.example.silencio.data.prefs.SilencioPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prefs: SilencioPrefs

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val isOnboarded = prefs.isOnboarded.first()
            val autoSilenceEnabled = prefs.autoSilenceEnabled.first()

            if (isOnboarded && autoSilenceEnabled) {
                CalendarWorker.schedule(context)
            }
        }
    }
}