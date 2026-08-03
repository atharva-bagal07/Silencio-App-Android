package com.silencio.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.silencio.app.data.repository.SilencioRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SilencioRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "Boot completed — rescheduling alarms")

        CoroutineScope(Dispatchers.IO).launch {
            val isOnboarded = repository.isOnboarded.first()
            if (!isOnboarded) return@launch

            repository.getUpcomingMeetings()
            Log.d("BootReceiver", "Alarms rescheduled after boot")
        }
    }
}