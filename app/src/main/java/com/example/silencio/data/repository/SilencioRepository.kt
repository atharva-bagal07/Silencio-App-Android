package com.example.silencio.data.repository

import com.example.silencio.core.calender.CalendarManager
import com.example.silencio.core.dnd.DndManager
import com.example.silencio.data.model.CalendarEvent
import com.example.silencio.data.prefs.SilencioPrefs
import android.content.Context
import com.example.silencio.alarm.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SilencioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarManager: CalendarManager,
    private val dndManager: DndManager,
    private val prefs: SilencioPrefs,
    private val alarmScheduler: AlarmScheduler
) {

    // ─── Calendar ────────────────────────────────────────────────

    suspend fun getCurrentEvent(): CalendarEvent? =
        calendarManager.getCurrentEvent()

    suspend fun getNextEvent(): CalendarEvent? =
        calendarManager.getNextEvent()

    fun getAvailableCalendars(): List<Pair<Long, String>> =
        calendarManager.getAvailableCalendars()

    // ─── Settings ────────────────────────────────────────────────

    val isOnboarded: Flow<Boolean> = prefs.isOnboarded

    val watchedCalendarIds: Flow<Set<Long>> = prefs.watchedCalendarIds


    suspend fun setOnboarded(value: Boolean) =
        prefs.setOnboarded(value)
    suspend fun setWatchedCalendarIds(ids: Set<Long>) =
        prefs.setWatchedCalendarIds(ids)

    suspend fun getUpcomingMeetings(): List<CalendarEvent> {
        val meetings = calendarManager.getUpcomingMeetings()
        val now = System.currentTimeMillis()
        meetings
            .filter { it.startTime > now }  // only schedule future events
            .forEach { alarmScheduler.scheduleMeeting(it) }
        return meetings
    }

    // ─── Session State ───────────────────────────────────────────

    val activeEventId: Flow<Long?> = prefs.activeEventId

    val silenceStartTime: Flow<Long?> = prefs.silenceStartTime

    val notificationsHeldCount: Flow<Long> = prefs.notificationsHeldCount

    val isCurrentlyActive: Flow<Boolean> = prefs.activeEventId
        .map { it != null }

    // ─── DND ─────────────────────────────────────────────────────

    fun hasDndPermission(): Boolean =
        dndManager.hasDndPermission()

    fun openDndPermissionSettings() =
        dndManager.openDndPermissionSettings()
}