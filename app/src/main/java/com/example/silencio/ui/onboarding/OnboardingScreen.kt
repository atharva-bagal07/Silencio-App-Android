package com.example.silencio.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.silencio.R
import com.example.silencio.ui.theme.AccentBlue
import com.example.silencio.ui.theme.Background
import com.example.silencio.ui.theme.TextMuted
import com.example.silencio.ui.theme.TextPrimary
import com.example.silencio.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    onCalendarConnected: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var permissionDenied by remember { mutableStateOf(false) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val calendarGranted = permissions[Manifest.permission.READ_CALENDAR]
            ?: false

        if (calendarGranted) {
            viewModel.onCalendarPermissionGranted()
            onCalendarConnected()
        } else {
            permissionDenied = true
        }
    }

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
            // Wave icon
            Icon(
                painter = painterResource(id = R.drawable.ic_wave),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Headline
            Text(
                text = "Your phone learns when to disappear.",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body
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

        // Bottom section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CTA button
            Button(
                onClick = {
                    calendarPermissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_CALENDAR)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Connect Google Calendar",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy note
            Text(
                text = "Silencio only reads event times and titles. Nothing else.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}