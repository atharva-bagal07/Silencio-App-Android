package com.example.silencio.ui.onboarding

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.silencio.R
import com.example.silencio.alarm.CalendarObserverService
import com.example.silencio.ui.theme.AccentBlue
import com.example.silencio.ui.theme.Background
import com.example.silencio.ui.theme.Divider
import com.example.silencio.ui.theme.StatusActive
import com.example.silencio.ui.theme.Surface
import com.example.silencio.ui.theme.SurfaceVariant
import com.example.silencio.ui.theme.TextMuted
import com.example.silencio.ui.theme.TextPrimary
import com.example.silencio.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    onCalendarConnected: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    var permissionDenied by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val dndLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.completeOnboarding()
        context.startService(Intent(context, CalendarObserverService::class.java))
        onCalendarConnected()
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_CALENDAR] ?: false
        if (granted) {
            viewModel.onCalendarPermissionGranted()
            viewModel.loadCalendars()
            step = 1
        } else {
            permissionDenied = true
        }
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            }
        },
        label = "onboarding_step"
    ) { currentStep ->
        when (currentStep) {
            0 -> WelcomeStep(
                permissionDenied = permissionDenied,
                onConnect = {
                    calendarPermissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_CALENDAR)
                    )
                }
            )

            1 -> CalendarPickerStep(
                calendars = uiState.availableCalendars,
                selectedIds = uiState.selectedCalendarIds,
                onBack = { step = 0 },
                onToggle = viewModel::toggleCalendar,
                onConfirm = {
                    viewModel.saveCalendars()
                    Log.d("Onboarding", "moving to step 2")
                    step = 2
                }
            )

            2 -> DndExplanationStep(
                onContinue = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    dndLauncher.launch(intent)
                }
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    permissionDenied: Boolean,
    onConnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_wave),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Your phone learns when to disappear.",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Silencio reads your calendar and silences " +
                        "your phone automatically. No rules. " +
                        "No buttons. Nothing to remember.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (permissionDenied) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Calendar access is required for Silencio to work. " +
                            "Please allow it to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Connect Google Calendar",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Silencio only reads event times and titles. Nothing else.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarPickerStep(
    calendars: List<Pair<Long, String>>,
    selectedIds: Set<Long>,
    onBack: () -> Unit,
    onToggle: (Long) -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 48.dp, start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Which calendars should Silencio watch?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Only events from these calendars will trigger silence.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(12.dp))
                ) {
                    calendars.forEachIndexed { index, (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                                fontSize = 17.sp
                            )
                            Checkbox(
                                checked = id in selectedIds,
                                onCheckedChange = { onToggle(id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = StatusActive,
                                    uncheckedColor = TextSecondary
                                )
                            )
                        }
                        if (index < calendars.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = Divider
                            )
                        }
                    }
                }
            }

            // Done button pinned to bottom
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        disabledContainerColor = SurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selectedIds.isNotEmpty()) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DndExplanationStep(
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "One last step.",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Silencio needs Do Not Disturb access to silence your phone during meetings. You'll be taken to your phone's settings — just tap Allow.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Allow access",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}