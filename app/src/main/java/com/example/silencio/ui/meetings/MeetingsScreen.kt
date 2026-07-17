package com.example.silencio.ui.meetings

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.silencio.data.model.CalendarEvent
import com.example.silencio.ui.theme.AccentBlue
import com.example.silencio.ui.theme.Background
import com.example.silencio.ui.theme.StatusActive
import com.example.silencio.ui.theme.StatusMonitoring
import com.example.silencio.ui.theme.Surface
import com.example.silencio.ui.theme.SurfaceVariant
import com.example.silencio.ui.theme.TextMuted
import com.example.silencio.ui.theme.TextPrimary
import com.example.silencio.ui.theme.TextSecondary
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun MeetingsScreen(
    viewModel: MeetingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar — sticky
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontSize = 32.sp
            )
        }

        // Scrollable content
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = uiState.todayLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (uiState.meetings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No upcoming meetings today",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.meetings,
                        key = { it.id }
                    ) { event ->
                        MeetingCard(
                            event = event,
                            now = uiState.now
                        )
                    }
                }
            }

            // Sticky footer
            Text(
                text = "Silencio will automatically silence\nyour phone for each meeting",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Background)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            )
        }
    }
}
// ─── Meeting Card ─────────────────────────────────────────────────

@Composable
private fun MeetingCard(
    event: CalendarEvent,
    now: Long
) {
    val isNow = now >= event.startTime && now <= event.endTime
    val minutesUntil = ((event.startTime - now) / 1000 / 60).toInt()
    val isStartingSoon = minutesUntil in 1..90
    val durationMinutes = ((event.endTime - event.startTime) / 1000 / 60).toInt()

    val leftBarColor = when {
        isNow -> StatusActive
        isStartingSoon -> AccentBlue
        else -> StatusMonitoring
    }

    val cal = Calendar.getInstance()
    cal.timeInMillis = event.startTime
    val eventDay =
        Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    cal.timeInMillis = now
    val todayDay =
        Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    val isToday = eventDay == todayDay

    val startFormatted = if (isToday) {
        DateFormat.format("h:mm a", event.startTime).toString()
    } else {
        DateFormat.format("d MMM, h:mm a", event.startTime).toString()
    }
    val endFormatted = DateFormat
        .format("h:mm a", event.endTime).toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
    ) {
        // Left colored bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(88.dp)
                .background(leftBarColor)
        )

        // Card content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 14.dp,
                    bottom = 14.dp
                )
        ) {
            // Status label
            when {
                isNow -> {
                    Text(
                        text = "NOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusActive,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                isStartingSoon -> {
                    Text(
                        text = "In $minutesUntil min",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                else -> {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Title
            Text(
                text = event.title,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Time range
            Text(
                text = "$startFormatted – $endFormatted",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Duration pill
        Box(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(SurfaceVariant)
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = formatDuration(durationMinutes),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

private fun formatDuration(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} hr"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}