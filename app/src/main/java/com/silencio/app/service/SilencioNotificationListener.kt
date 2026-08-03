package com.silencio.app.service

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.silencio.app.data.prefs.SilencioPrefs
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

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        val WHATSAPP_PACKAGES = setOf(WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE)
        const val TAG = "NotificationListener"

        private val PROMO_KEYWORDS = setOf(
            "otp", "offer", "deal", "discount", "expires", "% off",
            "order", "delivered", "delivery", "shipped", "out for delivery",
            "payment", "invoice", "receipt", "transaction", "credited",
            "debited", "cashback", "coupon", "code", "verify", "verification",
            "alert", "reminder", "subscription", "bill", "due", "recharge"
        )

    }

    override fun onListenerConnected() {
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        Log.d(TAG, "Notification listener disconnected — requesting rebind")
        requestRebind(ComponentName(this, SilencioNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        scope.launch {
            val activeEventId = prefs.activeEventId.first()
            if (activeEventId == null) return@launch

            // only process if DND is actually active
            val nm = getSystemService(NotificationManager::class.java)
            val isDndActive =
                nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            if (!isDndActive) return@launch

            // filter out bots and promo messages
            if (!isRealPerson(sbn)) return@launch

            prefs.incrementNotificationsHeld()
            Log.d(TAG, "Real notification held from ${sbn.packageName}")

            val isPremium = prefs.isPremium.first()
            if (isPremium && sbn.packageName in WHATSAPP_PACKAGES) {
                if (isGroupMessage(sbn)) {
                    Log.d(TAG, "Skipping group message")
                    return@launch
                }

                val vipContacts = prefs.vipContacts.first()
                val vipNames = vipContacts.values.toSet()
                val senderName = extractSenderName(sbn) ?: return@launch
                if (vipNames.isNotEmpty() && senderName !in vipNames) {
                    Log.d(TAG, "Skipping — $senderName not in VIP contacts")
                    return@launch
                }

                val conversationKey = "${sbn.packageName}_${sbn.tag}_${sbn.id}"
                val repliedConversations = prefs.repliedConversations.first()
                if (conversationKey !in repliedConversations) {
                    prefs.addRepliedConversation(conversationKey)
                    val replyMessage = prefs.customReplyMessage.first()
                        .ifEmpty { "I'm in a meeting right now. I'll get back to you soon." }
                    replyToWhatsApp(sbn, replyMessage)
                } else {
                    Log.d(TAG, "Already replied to $conversationKey — skipping")
                }
            }
        }
    }

    private fun isRealPerson(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false

        // Rule 1 — no reply action means not a real conversation
        val actions = notification.actions ?: return false
        val hasReplyAction = actions.any { it.remoteInputs?.isNotEmpty() == true }
        if (!hasReplyAction) {
            Log.d(TAG, "Skipping — no reply action")
            return false
        }


        val extras = notification.extras
        val messageText = extras?.getCharSequence("android.text")
            ?.toString()?.lowercase() ?: ""
        val titleText = extras?.getCharSequence("android.title")
            ?.toString() ?: ""

        // Rule 2 — promo/OTP keywords in message body
        if (PROMO_KEYWORDS.any { messageText.contains(it) }) {
            Log.d(TAG, "Skipping — promo keywords in message: $titleText")
            return false
        }
        // skip outgoing messages — WhatsApp labels them as "You"
        val selfName = extras?.getString("android.selfDisplayName")
        if (titleText == "You" || (selfName != null && titleText == selfName)) {
            Log.d(TAG, "Skipping — outgoing message")
            return false
        }

        return true
    }

    private fun isGroupMessage(sbn: StatusBarNotification): Boolean {
        val extras = sbn.notification?.extras ?: return false
        // group messages have android.subText set to the group name
        val subText = extras.getCharSequence("android.subText")?.toString()
        return subText != null
    }

    private fun extractSenderName(sbn: StatusBarNotification): String? {
        val extras = sbn.notification?.extras ?: return null
        // only read title (sender name), never message body for privacy
        return extras.getCharSequence("android.title")?.toString()
    }

    private fun replyToWhatsApp(sbn: StatusBarNotification, replyMessage: String) {
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