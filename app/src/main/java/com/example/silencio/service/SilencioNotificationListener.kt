package com.example.silencio.service


import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.app.RemoteInput
import com.example.silencio.data.prefs.SilencioPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SilencioNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var prefs: SilencioPrefs

    private val scope = CoroutineScope(Dispatchers.IO)
    private val repliedConversations = mutableSetOf<String>()

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        val WHATSAPP_PACKAGES = setOf(WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE)
        const val TAG = "NotificationListener"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        scope.launch {
            val activeEventId = prefs.activeEventId.first()
            if (activeEventId == null) {
                repliedConversations.clear() // clear when no meeting active
                return@launch
            }
            // only auto-reply if DND is actually active
            val nm = getSystemService(NotificationManager::class.java)
            val isDndActive =
                nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            if (!isDndActive) return@launch

            prefs.incrementNotificationsHeld()

            val isPremium = prefs.isPremium.first()
            if (isPremium && sbn.packageName in WHATSAPP_PACKAGES) {
                val conversationKey = "${sbn.packageName}_${sbn.tag}_${sbn.id}"
                if (conversationKey !in repliedConversations) {
                    repliedConversations.add(conversationKey)
                    replyToWhatsApp(sbn)
                } else {
                    Log.d(TAG, "Already replied to conversation $conversationKey — skipping")
                }
            }
        }
    }

    private fun replyToWhatsApp(sbn: StatusBarNotification) {
        val actions = sbn.notification?.actions
        Log.d(TAG, "WhatsApp notification — actions count: ${actions?.size ?: 0}")
        actions?.forEachIndexed { index, action ->
            Log.d(
                TAG,
                "Action $index: title=${action.title} remoteInputs=${action.remoteInputs?.size ?: 0}"
            )
        }

        val action = findReplyAction(sbn) ?: run {
            Log.d(TAG, "No reply action found")
            return
        }

        val remoteInput = action.remoteInputs?.firstOrNull() ?: run {
            Log.d(TAG, "No remote input found")
            return
        }

        val replyMessage = "I'm in a meeting right now. I'll get back to you soon."

        val sendIntent = Intent()
        val bundle = Bundle()
        bundle.putCharSequence(remoteInput.resultKey, replyMessage)
        RemoteInput.addResultsToIntent(arrayOf(remoteInput), sendIntent, bundle)

        try {
            action.actionIntent.send(this, 0, sendIntent)
            Log.d(TAG, "Auto-replied to WhatsApp: ${sbn.packageName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send WhatsApp reply: ${e.message}")
        }
    }

    private fun findReplyAction(sbn: StatusBarNotification): android.app.Notification.Action? {
        val actions = sbn.notification?.actions ?: return null
        return actions.firstOrNull { action ->
            action.remoteInputs?.isNotEmpty() == true &&
                    (action.title?.toString()?.contains("reply", ignoreCase = true) == true ||
                            action.title?.toString()
                                ?.contains("respond", ignoreCase = true) == true)
        } ?: actions.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // nothing for now
    }
}