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
        val WATCHED_CALENDAR_IDS = stringPreferencesKey("watched_calendar_ids")
        val ACTIVE_EVENT_ID = longPreferencesKey("active_event_id")
        val SILENCE_START_TIME = longPreferencesKey("silence_start_time")
        val NOTIFICATIONS_HELD_COUNT = longPreferencesKey("notifications_held_count")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val CUSTOM_REPLY_MESSAGE = stringPreferencesKey("custom_reply_message")
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data
        .map { it[IS_ONBOARDED] ?: false }

    val isPremium: Flow<Boolean> = context.dataStore.data
        .map { it[IS_PREMIUM] ?: true } // true for testing

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

    val customReplyMessage: Flow<String> = context.dataStore.data
        .map {
            it[CUSTOM_REPLY_MESSAGE] ?: "I'm in a meeting right now. I'll get back to you soon."
        }

    suspend fun setCustomReplyMessage(message: String) {
        context.dataStore.edit { it[CUSTOM_REPLY_MESSAGE] = message }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[IS_ONBOARDED] = value }
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