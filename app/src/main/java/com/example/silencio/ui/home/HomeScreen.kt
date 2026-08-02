package com.example.silencio.ui.home

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.silencio.data.model.CalendarEvent
import com.example.silencio.ui.theme.AccentBlue
import com.example.silencio.ui.theme.Background
import com.example.silencio.ui.theme.ProgressUnfilled
import com.example.silencio.ui.theme.StatusActive
import com.example.silencio.ui.theme.StatusMonitoring
import com.example.silencio.ui.theme.Surface
import com.example.silencio.ui.theme.TextMuted
import com.example.silencio.ui.theme.TextPrimary
import com.example.silencio.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnboarded by viewModel.isOnboarded.collectAsState()



    LaunchedEffect(Unit) {
        if (isOnboarded == false) viewModel.completeOnboarding()
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showCalendarSheet by rememberSaveable(uiState.hasCalendarPermission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(uiState.hasCalendarPermission) {
        if (!uiState.hasCalendarPermission) {
            delay(500)
            if (!uiState.hasCalendarPermission) {
                showCalendarSheet = true
            }
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_CALENDAR] ?: false
        if (granted) {
            showCalendarSheet = false
            viewModel.onResume()
        }
    }

    if (showCalendarSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCalendarSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "⚠️", fontSize = 40.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Calendar access required",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Silencio needs calendar access to detect your meetings and silence your phone automatically.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        calendarPermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_CALENDAR)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
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
    }

    var showDndSheet by rememberSaveable(uiState.hasDndPermission) {
        val granted = context.getSystemService(NotificationManager::class.java)
            .isNotificationPolicyAccessGranted
        mutableStateOf(!granted)
    }

    LaunchedEffect(uiState.hasDndPermission, uiState.isLoading) {
        Log.d(
            "HomeScreen",
            "LaunchedEffect — isLoading=${uiState.isLoading} hasDndPermission=${uiState.hasDndPermission} dndEverGranted=${uiState.dndEverGranted}"
        )
        if (!uiState.isLoading && !uiState.hasDndPermission && !uiState.dndEverGranted) {
            delay(500)
            Log.d(
                "HomeScreen",
                "After delay — hasDndPermission=${uiState.hasDndPermission} dndEverGranted=${uiState.dndEverGranted}"
            )
            if (!uiState.hasDndPermission && !uiState.dndEverGranted) {
                Log.d("HomeScreen", "Setting showDndSheet = true")
                showDndSheet = true
            }
        }
    }

    if (showDndSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDndSheet = false },
            sheetState = sheetState,
            containerColor = Surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 40.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DND access required",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Silencio won't be able to silence your phone during meetings without Do Not Disturb access.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        showDndSheet = false
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
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
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 24.dp)
    ) {
        // Top bar — date only, no settings icon
        Text(
            text = remember {
                DateFormat.format("EEEE, d MMM", System.currentTimeMillis()).toString()
            },
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 22.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 48.dp, bottom = 40.dp)
        )

        // Main card
        if (uiState.isActive && uiState.currentEvent != null) {
            ActiveCard(
                eventTitle = uiState.currentEvent!!.title,
                startTime = uiState.currentEvent!!.startTime,
                endTime = uiState.currentEvent!!.endTime,
                nextEventTitle = uiState.nextEvent?.title,
                nextEventStartTime = uiState.nextEvent?.startTime
            )
        } else {
            IdleCard(
                nextEventTitle = uiState.nextEvent?.title,
                nextEventStartTime = uiState.nextEvent?.startTime
            )
        }

        // Upcoming meetings list — shown in idle state only
        if (!uiState.isActive && uiState.upcomingEvents.isNotEmpty() && !uiState.isLoading) {
            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "UPCOMING",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            uiState.upcomingEvents.forEach { event ->
                UpcomingEventRow(event = event)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun UpcomingEventRow(event: CalendarEvent) {
    val timeFormatted = remember(event.startTime) {
        android.text.format.DateFormat.format("h:mm a", event.startTime).toString()
    }
    val durationMin = ((event.endTime - event.startTime) / 1000 / 60).toInt()
    val durationText = when {
        durationMin < 60 -> "${durationMin}m"
        durationMin % 60 == 0 -> "${durationMin / 60}h"
        else -> "${durationMin / 60}h ${durationMin % 60}m"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timeFormatted,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.width(64.dp)
        )
        Text(
            text = event.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = durationText,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
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
        DateFormat.format("h:mm a", endTime).toString()
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
            color = TextSecondary,
            fontSize = 18.sp
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
            color = TextMuted,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (nextEventTitle != null && nextEventStartTime != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val nextFormatted = remember(nextEventStartTime) {
                DateFormat.format("h:mm a", nextEventStartTime).toString()
            }
            Text(
                text = "Next: $nextEventTitle at $nextFormatted",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                fontSize = 16.sp
            )
        }
    }
}

// ─── Idle Card ───────────────────────────────────────────────────

@Composable
private fun IdleCard(
    nextEventTitle: String?,
    nextEventStartTime: Long?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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