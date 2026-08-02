package com.example.silencio.ui.premium


import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silencio.ui.theme.Background
import com.example.silencio.ui.theme.TextPrimary
import com.example.silencio.ui.theme.TextSecondary
import com.example.silencio.ui.theme.PremiumGold
import com.example.silencio.ui.theme.PremiumGoldDim
import com.example.silencio.ui.theme.Surface
import com.example.silencio.ui.theme.TextMuted

@Composable
fun NotificationAccessScreen(
    onDone: () -> Unit
) {
    val context = LocalContext.current

    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Surface,
            title = {
                Text(
                    text = "Skip notification access?",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Without this, Silencio won't be able to auto-reply to your selected contacts on WhatsApp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(
                        text = "Grant access",
                        color = PremiumGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onDone()
                }) {
                    Text(
                        text = "Skip anyway",
                        color = TextMuted
                    )
                }
            }
        )
    }

// update the skip button
    TextButton(
        onClick = { showConfirmDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "I'll do this later",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(top = 236.dp)
        ) {
            Text(
                text = "One last thing",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Silencio needs notification access to auto-reply on WhatsApp when you're in a meeting.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PremiumGoldDim)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "We only read sender names from\nnotifications. Messages are never read.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    onDone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Grant access",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF1A1400),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "I'll do this later",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 18.sp
                )
            }
        }
    }
}