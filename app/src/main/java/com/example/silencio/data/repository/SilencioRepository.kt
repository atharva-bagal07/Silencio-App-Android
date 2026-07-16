package com.example.silencio.data.repository

import com.example.silencio.core.calender.CalendarManager
import com.example.silencio.core.dnd.DndManager
import com.example.silencio.data.model.CalendarEvent
import com.example.silencio.data.model.VipContact
import com.example.silencio.data.prefs.SilencioPrefs
import android.content.Context
import android.provider.ContactsContract
import com.example.silencio.alarm.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    val autoSilenceEnabled: Flow<Boolean> = prefs.autoSilenceEnabled

    val vibrateInstead: Flow<Boolean> = prefs.vibrateInstead

    val preMeetingAlert: Flow<Boolean> = prefs.preMeetingAlert

    val watchedCalendarIds: Flow<Set<Long>> = prefs.watchedCalendarIds

    val vipContactIds: Flow<Set<Long>> = prefs.vipContactIds

    suspend fun setOnboarded(value: Boolean) =
        prefs.setOnboarded(value)

    suspend fun setAutoSilenceEnabled(value: Boolean) =
        prefs.setAutoSilenceEnabled(value)

    suspend fun setVibrateInstead(value: Boolean) =
        prefs.setVibrateInstead(value)

    suspend fun setPreMeetingAlert(value: Boolean) =
        prefs.setPreMeetingAlert(value)

    suspend fun setWatchedCalendarIds(ids: Set<Long>) =
        prefs.setWatchedCalendarIds(ids)

    suspend fun setVipContactIds(ids: Set<Long>) =
        prefs.setVipContactIds(ids)

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

    // ─── Contacts ────────────────────────────────────────────────

    /**
     * Reads all contacts from the device.
     * Used in onboarding to let user select VIP contacts.
     */
    fun getDeviceContacts(): List<VipContact> {
        val contacts = mutableListOf<VipContact>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        val selection =
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL"

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            )
            val nameIndex = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val numberIndex = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val photoIndex = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val seenIds = mutableSetOf<Long>()

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)

                // Skip duplicates — same contact
                // can have multiple phone numbers
                if (seenIds.contains(id)) continue
                seenIds.add(id)

                contacts.add(
                    VipContact(
                        id = id,
                        name = cursor.getString(nameIndex) ?: "Unknown",
                        phoneNumber = cursor.getString(numberIndex) ?: "",
                        avatarUri = cursor.getString(photoIndex)
                    )
                )
            }
        }

        return contacts
    }

    /**
     * Returns full VipContact objects for the saved VIP contact IDs.
     * Used to display VIP contacts in settings.
     */
    suspend fun getVipContacts(): List<VipContact> {
        val savedIds = prefs.vipContactIds.first()
        if (savedIds.isEmpty()) return emptyList()

        return getDeviceContacts().filter { it.id in savedIds }
    }
}