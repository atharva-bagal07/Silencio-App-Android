package com.example.silencio.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.silencio.MainActivity
import com.example.silencio.R
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val POST_MEETING_CHANNEL_ID = "silencio_post_meeting"
        const val FOREGROUND_CHANNEL_ID = "silencio_foreground"
        const val POST_MEETING_NOTIFICATION_ID = 1001
        const val FOREGROUND_NOTIFICATION_ID = 1002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

    init {
        createChannels()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannels() {
        // Post meeting report channel
        // High importance so it shows as a heads-up notification
        val postMeetingChannel = NotificationChannel(
            POST_MEETING_CHANNEL_ID,
            "Meeting Reports",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows what happened while your phone was silent"
            setShowBadge(false)
        }

        // Foreground service channel
        // Low importance — just keeps the worker alive, user shouldn't see this
        val foregroundChannel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            "Silencio Active",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Silencio is monitoring your calendar"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(
            listOf(postMeetingChannel, foregroundChannel)
        )
    }

    /**
     * The most important notification in the app.
     * This is the aha moment — proof that Silencio worked.
     * Fires at the end of every silenced meeting.
     */
    fun showPostMeetingNotification(
        durationMinutes: Int,
        notificationsHeld: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            POST_MEETING_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_wave)
            .setContentTitle("Meeting protected")
            .setContentText(
                "${durationMinutes}m silent " +
                        "• ${notificationsHeld} held " +
                        "• 0 interruptions"
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Your phone was silent for ${durationMinutes} minutes.\n" +
                                "${notificationsHeld} notifications held. 0 interruptions."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // No sound on this notification
            // It would be ironic to interrupt someone with a sound
            // telling them they weren't interrupted
            .setSilent(true)
            .build()

        notificationManager.notify(POST_MEETING_NOTIFICATION_ID, notification)
    }

    /**
     * Silent foreground notification shown while Silencio
     * is actively silencing the phone during a meeting.
     * Required by Android to keep the foreground service alive.
     */
    fun buildForegroundNotification(eventTitle: String) =
        NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wave)
            .setContentTitle("Silencio active")
            .setContentText("Protecting: $eventTitle")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()
}