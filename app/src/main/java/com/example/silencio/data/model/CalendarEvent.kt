package com.example.silencio.data.model

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val calendarId: Long
) {
    fun isHappeningNow(): Boolean {
        val now = System.currentTimeMillis()
        return now in startTime..endTime
    }

    fun durationMinutes(): Int {
        return ((endTime - startTime) / 1000 / 60).toInt()
    }
}