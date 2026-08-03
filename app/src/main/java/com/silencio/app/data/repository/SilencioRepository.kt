package com.silencio.app.data.repository

import android.Manifest
import android.content.ContentValues
import com.silencio.app.core.calender.CalendarManager
import com.silencio.app.core.dnd.DndManager
import com.silencio.app.data.model.CalendarEvent
import com.silencio.app.data.prefs.SilencioPrefs
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.silencio.app.alarm.AlarmScheduler
import com.silencio.app.data.model.ReplyContact
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

    val isPremium: Flow<Boolean> = prefs.isPremium
    val customReplyMessage: Flow<String> = prefs.customReplyMessage

    val dndPermissionGranted: Flow<Boolean> = prefs.dndPermissionGranted

    val vipContacts: Flow<Map<Long, String>> = prefs.vipContacts
    suspend fun setVipContacts(contacts: Map<Long, String>) = prefs.setVipContacts(contacts)

    fun starContact(contactId: Long) {
        val values = ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, 1)
        }
        context.contentResolver.update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString())
        )
    }

    fun unstarContact(contactId: Long) {
        val values = ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, 0)
        }
        context.contentResolver.update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString())
        )
    }

    suspend fun setDndPermissionGranted(value: Boolean) = prefs.setDndPermissionGranted(value)


    suspend fun setPremium(value: Boolean) = prefs.setPremium(value)
    suspend fun setCustomReplyMessage(message: String) = prefs.setCustomReplyMessage(message)


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

    fun getDeviceContacts(): List<ReplyContact> {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("SilencioRepository", "READ_CONTACTS not granted — skipping")
            return emptyList()
        }
        val contacts = mutableListOf<ReplyContact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL"
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex =
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val seenIds = mutableSetOf<Long>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                if (seenIds.contains(id)) continue
                seenIds.add(id)
                contacts.add(
                    ReplyContact(
                        id = id,
                        name = cursor.getString(nameIndex) ?: "Unknown",
                        avatarUri = cursor.getString(photoIndex)
                    )
                )
            }
        }
        return contacts
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