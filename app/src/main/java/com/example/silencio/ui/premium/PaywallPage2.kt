package com.example.silencio.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silencio.ui.theme.Background
import com.example.silencio.ui.theme.Surface
import com.example.silencio.ui.theme.TextMuted
import com.example.silencio.ui.theme.TextPrimary
import com.example.silencio.ui.theme.TextSecondary

private val PremiumGold = Color(0xFFD4A847)

@Composable
fun PaywallPage2(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "You've already set up the hard part.",
                style = MaterialTheme.typography.labelSmall,
                color = PremiumGold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "One last step to protect everything.",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // feature rows — outcome focused
            FeatureRow(
                emoji = "💬",
                title = "They get a reply. Automatically.",
                subtitle = "The moment your meeting starts, Silencio replies on your behalf."
            )

            Spacer(modifier = Modifier.height(20.dp))

            FeatureRow(
                emoji = "✏️",
                title = "You choose what it says.",
                subtitle = "Set a custom message. Change it anytime."
            )

            Spacer(modifier = Modifier.height(20.dp))

            FeatureRow(
                emoji = "📋",
                title = "Catch up the moment it ends.",
                subtitle = "See everyone who reached out, the second your meeting ends."
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "One last step",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF1A1400),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    emoji: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF2A2510), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}