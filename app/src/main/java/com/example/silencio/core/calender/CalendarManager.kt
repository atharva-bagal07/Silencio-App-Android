package com.example.silencio.core.calender

import android.content.Context
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.silencio.data.model.CalendarEvent
import com.example.silencio.data.prefs.SilencioPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SilencioPrefs
) {


    companion object {
        private val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_ID
        )

        private const val PROJECTION_ID_INDEX = 0
        private const val PROJECTION_TITLE_INDEX = 1
        private const val PROJECTION_DTSTART_INDEX = 2
        private const val PROJECTION_DTEND_INDEX = 3
        private const val PROJECTION_CALENDAR_ID_INDEX = 4

        // How far ahead to look for upcoming events
        private const val LOOK_AHEAD_MS = 48 * 60 * 60 * 1000L // 48 hours
    }

    /**
     * Returns the currently active event if one exists,
     * null if no event is happening right now.
     */
    suspend fun getCurrentEvent(): CalendarEvent? {
        if (!hasCalendarPermission()) return null

        val now = System.currentTimeMillis()
        val watchedCalendarIds = prefs.watchedCalendarIds.first()

        val selection = buildString {
            append("${CalendarContract.Events.DTSTART} <= ? AND ")
            append("${CalendarContract.Events.DTEND} >= ? AND ")
            append("${CalendarContract.Events.DELETED} = 0")
            if (watchedCalendarIds.isNotEmpty()) {
                append(" AND ${CalendarContract.Events.CALENDAR_ID} IN (")
                append(watchedCalendarIds.joinToString(","))
                append(")")
            }
        }

        val selectionArgs = arrayOf(
            now.toString(),
            now.toString()
        )

        val events = queryEvents(selection, selectionArgs)

        return events.distinctBy { "${it.title}_${it.startTime}" }.firstOrNull()
    }

    /**
     * Returns the next upcoming event within the next 24 hours,
     * null if nothing is scheduled.
     */
    suspend fun getNextEvent(): CalendarEvent? {
        // Guard — never query calendar without permission
        if (!hasCalendarPermission()) return null
        val now = System.currentTimeMillis()
        val lookAhead = now + LOOK_AHEAD_MS
        val watchedCalendarIds = prefs.watchedCalendarIds.first()

        val selection = buildString {
            append("${CalendarContract.Events.DTSTART} > ? AND ")
            append("${CalendarContract.Events.DTSTART} <= ? AND ")
            append("${CalendarContract.Events.DELETED} = 0")
            if (watchedCalendarIds.isNotEmpty()) {
                append(" AND ${CalendarContract.Events.CALENDAR_ID} IN (")
                append(watchedCalendarIds.joinToString(","))
                append(")")
            }
        }

        val selectionArgs = arrayOf(
            now.toString(),
            lookAhead.toString()
        )

        return queryEvents(selection, selectionArgs)
            .distinctBy { "${it.title}_${it.startTime}" }
            .minByOrNull { it.startTime }
    }

    /**
     * Returns all available calendars on the device.
     * Used in settings to let user choose which calendars to watch.
     */
    fun getAvailableCalendars(): List<Pair<Long, String>> {
        val calendars = mutableListOf<Pair<Long, String>>()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT
        )

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val name = cursor.getString(1) ?: "Unnamed Calendar"
                val owner = cursor.getString(2) ?: ""
                if (owner.endsWith("@gmail.com")) {
                    calendars.add(Pair(id, name))
                }
            }
        }

        return calendars
    }

    suspend fun getUpcomingMeetings(): List<CalendarEvent> {
        if (!hasCalendarPermission()) return emptyList()

        val now = System.currentTimeMillis()
        val lookAhead = now + (24 * 60 * 60 * 1000L)
        val watchedCalendarIds = prefs.watchedCalendarIds.first()

        val selection = buildString {
            append("${CalendarContract.Events.DTEND} >= ? AND ")
            append("${CalendarContract.Events.DTSTART} <= ? AND ")
            append("${CalendarContract.Events.DELETED} = 0")
            if (watchedCalendarIds.isNotEmpty()) {
                append(" AND ${CalendarContract.Events.CALENDAR_ID} IN (")
                append(watchedCalendarIds.joinToString(","))
                append(")")
            }
        }

        val selectionArgs = arrayOf(
            now.toString(),
            lookAhead.toString()
        )

        return queryEvents(selection, selectionArgs)
            .distinctBy { "${it.title}_${it.startTime}" }  // replace distinctBy { it.id }
            .sortedBy { it.startTime }
            .take(10)
    }

    private fun queryEvents(
        selection: String,
        selectionArgs: Array<String>
    ): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()


        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            EVENT_PROJECTION,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {

                val id = cursor.getLong(PROJECTION_ID_INDEX)
                val title = cursor.getString(PROJECTION_TITLE_INDEX)
                    ?: "Untitled Event"
                val startTime = cursor.getLong(PROJECTION_DTSTART_INDEX)
                val endTime = cursor.getLong(PROJECTION_DTEND_INDEX)
                val calendarId = cursor.getLong(PROJECTION_CALENDAR_ID_INDEX)

                // Skip all-day events
                val durationHours = (endTime - startTime) / 1000 / 60 / 60
                if (durationHours >= 24) continue

                // Skip events with no duration
                if (endTime <= startTime) continue

//                // Skip solo events unless user wants all events silenced
//                val shouldFilter = !prefs.silenceAllEvents.first()
//                if (shouldFilter && !hasAttendees(id)) continue

                events.add(
                    CalendarEvent(
                        id = id,
                        title = title,
                        startTime = startTime,
                        endTime = endTime,
                        calendarId = calendarId
                    )
                )
            }
        }

        return events
    }

    private fun hasAttendees(eventId: Long): Boolean {
        val attendeeProjection = arrayOf(
            CalendarContract.Attendees.EVENT_ID
        )

        val cursor = context.contentResolver.query(
            CalendarContract.Attendees.CONTENT_URI,
            attendeeProjection,
            "${CalendarContract.Attendees.EVENT_ID} = ?",
            arrayOf(eventId.toString()),
            null
        )

        val count = cursor?.count ?: 0
        cursor?.close()

        // More than 1 attendee means other people are invited
        // 1 attendee is just the calendar owner themselves
        return count > 1
    }

    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}