package com.example.silencio.core.dnd

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DndManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

    /**
     * Returns true if the app has permission to control DND.
     * This must be true before any silence/restore call.
     */
    fun hasDndPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Opens the system settings screen where the user
     * can grant DND permission to Silencio.
     * Call this when hasDndPermission() returns false.
     */
    fun openDndPermissionSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Silences the phone.
     * If vibrateInstead is true, sets to vibrate.
     * Otherwise sets to total silence.
     */
    fun silence(vibrateInstead: Boolean = false) {
        if (!hasDndPermission()) return

        if (vibrateInstead) {
            // Vibrate only — alarms still work
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_ALARMS
            )
        } else {
            // Priority only — VIP calls ring through
            val policy = NotificationManager.Policy(
                NotificationManager.Policy.PRIORITY_CATEGORY_CALLS,
                NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                NotificationManager.Policy.PRIORITY_SENDERS_STARRED
            )
            notificationManager.notificationPolicy = policy
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            )
        }
    }

    /**
     * Restores the phone to normal sound mode.
     */
    fun restore() {
        if (!hasDndPermission()) return

        notificationManager.setInterruptionFilter(
            NotificationManager.INTERRUPTION_FILTER_ALL
        )
    }

    /**
     * Returns true if phone is currently in DND/silent mode
     * managed by Silencio.
     */
    fun isCurrentlySilenced(): Boolean {
        return notificationManager.currentInterruptionFilter !=
                NotificationManager.INTERRUPTION_FILTER_ALL
    }
}