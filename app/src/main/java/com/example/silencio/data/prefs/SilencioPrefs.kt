package com.example.silencio.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "silencio_prefs")

@Singleton
class SilencioPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val AUTO_SILENCE_ENABLED = booleanPreferencesKey("auto_silence_enabled")
        val VIBRATE_INSTEAD = booleanPreferencesKey("vibrate_instead")
        val PRE_MEETING_ALERT = booleanPreferencesKey("pre_meeting_alert")
        val VIP_CONTACT_IDS = stringPreferencesKey("vip_contact_ids")
        val WATCHED_CALENDAR_IDS = stringPreferencesKey("watched_calendar_ids")
        val ACTIVE_EVENT_ID = longPreferencesKey("active_event_id")
        val SILENCE_START_TIME = longPreferencesKey("silence_start_time")
        val NOTIFICATIONS_HELD_COUNT = longPreferencesKey("notifications_held_count")
        val SILENCE_ALL_EVENTS = booleanPreferencesKey("silence_all_events")
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data
        .map { it[IS_ONBOARDED] ?: false }

    val autoSilenceEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[AUTO_SILENCE_ENABLED] ?: true }

    val vibrateInstead: Flow<Boolean> = context.dataStore.data
        .map { it[VIBRATE_INSTEAD] ?: false }

    val preMeetingAlert: Flow<Boolean> = context.dataStore.data
        .map { it[PRE_MEETING_ALERT] ?: true }

    val vipContactIds: Flow<Set<Long>> = context.dataStore.data
        .map { prefs ->
            prefs[VIP_CONTACT_IDS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.map { it.toLong() }
                ?.toSet()
                ?: emptySet()
        }

    val watchedCalendarIds: Flow<Set<Long>> = context.dataStore.data
        .map { prefs ->
            prefs[WATCHED_CALENDAR_IDS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.map { it.toLong() }
                ?.toSet()
                ?: emptySet()
        }

    val activeEventId: Flow<Long?> = context.dataStore.data
        .map { prefs ->
            prefs[ACTIVE_EVENT_ID]?.takeIf { it != -1L }
        }

    val silenceStartTime: Flow<Long?> = context.dataStore.data
        .map { prefs ->
            prefs[SILENCE_START_TIME]?.takeIf { it != -1L }
        }

    val notificationsHeldCount: Flow<Long> = context.dataStore.data
        .map { it[NOTIFICATIONS_HELD_COUNT] ?: 0L }

    val silenceAllEvents: Flow<Boolean> = context.dataStore.data
        .map { it[SILENCE_ALL_EVENTS] ?: false }

    suspend fun setSilenceAllEvents(value: Boolean) {
        context.dataStore.edit { it[SILENCE_ALL_EVENTS] = value }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[IS_ONBOARDED] = value }
    }

    suspend fun setAutoSilenceEnabled(value: Boolean) {
        context.dataStore.edit { it[AUTO_SILENCE_ENABLED] = value }
    }

    suspend fun setVibrateInstead(value: Boolean) {
        context.dataStore.edit { it[VIBRATE_INSTEAD] = value }
    }

    suspend fun setPreMeetingAlert(value: Boolean) {
        context.dataStore.edit { it[PRE_MEETING_ALERT] = value }
    }

    suspend fun setVipContactIds(ids: Set<Long>) {
        context.dataStore.edit {
            it[VIP_CONTACT_IDS] = ids.joinToString(",")
        }
    }

    suspend fun setWatchedCalendarIds(ids: Set<Long>) {
        context.dataStore.edit {
            it[WATCHED_CALENDAR_IDS] = ids.joinToString(",")
        }
    }

    suspend fun setActiveEventId(id: Long?) {
        context.dataStore.edit {
            it[ACTIVE_EVENT_ID] = id ?: -1L
        }
    }

    suspend fun setSilenceStartTime(time: Long?) {
        context.dataStore.edit {
            it[SILENCE_START_TIME] = time ?: -1L
        }
    }

    suspend fun incrementNotificationsHeld() {
        context.dataStore.edit { prefs ->
            val current = prefs[NOTIFICATIONS_HELD_COUNT] ?: 0L
            prefs[NOTIFICATIONS_HELD_COUNT] = current + 1
        }
    }

    suspend fun resetNotificationsHeld() {
        context.dataStore.edit {
            it[NOTIFICATIONS_HELD_COUNT] = 0L
        }
    }
}