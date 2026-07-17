package com.example.silencio.ui.home

import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.silencio.ui.theme.AccentBlue
import com.example.silencio.ui.theme.Background
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 24.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = remember {
                    DateFormat.format("EEEE, d MMM", System.currentTimeMillis()).toString()
                },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 22.sp,
                color = TextMuted
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextMuted,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        // Main content — vertically centered
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isLoading && !uiState.hasDndPermission) {
                DndPermissionCard(
                    onGrantClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
            } else if (uiState.isActive && uiState.currentEvent != null) {
                ActiveCard(
                    eventTitle = uiState.currentEvent!!.title,
                    startTime = uiState.currentEvent!!.startTime,
                    endTime = uiState.currentEvent!!.endTime,
                    nextEventTitle = uiState.nextEvent?.title,
                    nextEventStartTime = uiState.nextEvent?.startTime
                )
            } else if (!uiState.isLoading) {
                IdleCard(
                    nextEventTitle = uiState.nextEvent?.title,
                    nextEventStartTime = uiState.nextEvent?.startTime,
                    onSeeAllMeetings = onSeeAllMeetings
                )
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
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            currentTime = System.currentTimeMillis()
        }
    }

    val progress = ((currentTime - startTime).toFloat() / (endTime - startTime).toFloat())
        .coerceIn(0f, 1f)

    val minutesRemaining = ((endTime - currentTime) / 1000 / 60).toInt().coerceAtLeast(0)
    val remainingText = when {
        minutesRemaining < 1 -> "<1 min remaining"
        minutesRemaining == 1 -> "1 min remaining"
        else -> "$minutesRemaining mins remaining"
    }

    val endTimeFormatted = remember(endTime) {
        android.text.format.DateFormat.format("h:mm a", endTime).toString()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Status label
        StatusPill(isActive = true, label = "Active")

        Spacer(modifier = Modifier.height(24.dp))

        // Event title — large and prominent
        Text(
            text = eventTitle,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Silent until $endTimeFormatted",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AccentBlue,
            trackColor = ProgressUnfilled
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = remainingText,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Surface)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "VIP contacts can still reach you",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        if (nextEventTitle != null && nextEventStartTime != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val nextFormatted = remember(nextEventStartTime) {
                android.text.format.DateFormat.format("h:mm a", nextEventStartTime).toString()
            }
            Text(
                text = "Next: $nextEventTitle at $nextFormatted",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

// ─── Idle Card ───────────────────────────────────────────────────

@Composable
private fun IdleCard(
    nextEventTitle: String?,
    nextEventStartTime: Long?,
    onSeeAllMeetings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        StatusPill(isActive = false, label = "Monitoring")

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No meetings\nright now",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (nextEventTitle != null && nextEventStartTime != null) {
            val nextFormatted = remember(nextEventStartTime) {
                android.text.format.DateFormat.format("h:mm a", nextEventStartTime).toString()
            }
            Text(
                text = "Next: $nextEventTitle at $nextFormatted",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        } else {
            Text(
                text = "Nothing scheduled today",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSeeAllMeetings,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "See all meetings",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

// ─── DND Permission Card ─────────────────────────────────────────

@Composable
private fun DndPermissionCard(onGrantClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "One step\nleft",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Silencio needs Do Not Disturb access to silence your phone during meetings.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onGrantClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Grant access",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ─── Status Pill ─────────────────────────────────────────────────

@Composable
private fun StatusPill(isActive: Boolean, label: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(if (isActive) alpha else 1f)
                .background(
                    color = if (isActive) StatusActive else StatusMonitoring,
                    shape = RoundedCornerShape(50.dp)
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) StatusActive else StatusMonitoring,
            letterSpacing = 1.5.sp
        )
    }
}