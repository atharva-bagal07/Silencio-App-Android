package com.example.silencio.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.silencio.ui.theme.AccentBlue
import com.example.silencio.ui.theme.Background
import com.example.silencio.ui.theme.Divider
import com.example.silencio.ui.theme.ProgressUnfilled
import com.example.silencio.ui.theme.StatusActive
import com.example.silencio.ui.theme.StatusActiveBackground
import com.example.silencio.ui.theme.StatusMonitoring
import com.example.silencio.ui.theme.Surface
import com.example.silencio.ui.theme.TextMuted
import com.example.silencio.ui.theme.TextPrimary
import com.example.silencio.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onSeeAllMeetings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh when screen resumes
    // Catches DND permission granted while app was in background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ─── Top bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Silencio",
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ─── Center content ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isLoading && !uiState.hasDndPermission) {
                DndPermissionCard(
                    onGrantClick = {
                        val intent = Intent(
                            Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                )
            } else if (uiState.isActive && uiState.currentEvent != null) {
                ActiveCard(
                    eventTitle = uiState.currentEvent!!.title,
                    endTime = uiState.currentEvent!!.endTime,
                    startTime = uiState.currentEvent!!.startTime,
                    nextEventTitle = uiState.nextEvent?.title,
                    nextEventStartTime = uiState.nextEvent?.startTime
                )
            } else {
                IdleCard(
                    nextEventTitle = uiState.nextEvent?.title,
                    nextEventStartTime = uiState.nextEvent?.startTime
                )
                TextButton(
                    onClick = onSeeAllMeetings,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "See all meetings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentBlue
                    )
                }
            }
        }
    }
}

// ─── Active Card ─────────────────────────────────────────────────

@Composable
private fun ActiveCard(
    eventTitle: String,
    startTime: Long,
    endTime: Long,
    nextEventTitle: String?,
    nextEventStartTime: Long?
) {
    // Ticking clock — updates every second
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            currentTime = System.currentTimeMillis()
        }
    }

    val progress = remember(currentTime, startTime, endTime) {
        ((currentTime - startTime).toFloat() / (endTime - startTime).toFloat())
            .coerceIn(0f, 1f)
    }

    val minutesRemaining = remember(currentTime, endTime) {
        ((endTime - currentTime) / 1000 / 60).toInt().coerceAtLeast(0)
    }
    val remainingText =
        if (minutesRemaining < 1) "<1 min" else if (minutesRemaining == 1) "1 min" else "$minutesRemaining mins"

    val endTimeFormatted = remember(endTime) {
        android.text.format.DateFormat.format("h:mm a", endTime).toString()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Status pill
            StatusPill(
                isActive = true,
                label = "Active"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Event title
            Text(
                text = eventTitle,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Time remaining
            Text(
                text = "Ends at $endTimeFormatted · $remainingText remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AccentBlue,
                trackColor = ProgressUnfilled
            )

            Spacer(modifier = Modifier.height(16.dp))

            // VIP note
            Text(
                text = "VIP contacts will still ring through",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }

    // Next event
    if (nextEventTitle != null && nextEventStartTime != null) {
        Spacer(modifier = Modifier.height(16.dp))
        val nextFormatted = remember(nextEventStartTime) {
            android.text.format.DateFormat.format(
                "h:mm a", nextEventStartTime
            ).toString()
        }
        Text(
            text = "Next: $nextEventTitle at $nextFormatted",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

// ─── Idle Card ───────────────────────────────────────────────────

@Composable
private fun IdleCard(
    nextEventTitle: String?,
    nextEventStartTime: Long?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusPill(
                isActive = false,
                label = "Monitoring"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No meetings right now",
                style = MaterialTheme.typography.headlineLarge,
                color = TextSecondary
            )

            if (nextEventTitle != null && nextEventStartTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val nextFormatted = remember(nextEventStartTime) {
                    android.text.format.DateFormat.format(
                        "h:mm a", nextEventStartTime
                    ).toString()
                }
                Text(
                    text = "Next: $nextEventTitle at $nextFormatted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Your phone is fully available",
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted
    )
}

// ─── DND Permission Card ─────────────────────────────────────────

@Composable
private fun DndPermissionCard(
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "One permission needed",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Silencio needs Do Not Disturb access to silence your phone during meetings.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onGrantClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Grant Access",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ─── Status Pill ─────────────────────────────────────────────────

@Composable
private fun StatusPill(
    isActive: Boolean,
    label: String
) {
    Row(
        modifier = Modifier
            .background(
                color = if (isActive) StatusActiveBackground
                else Surface,
                shape = RoundedCornerShape(50.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = if (isActive) StatusActive else StatusMonitoring,
                    shape = RoundedCornerShape(50.dp)
                )
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) StatusActive else StatusMonitoring
        )
    }
}