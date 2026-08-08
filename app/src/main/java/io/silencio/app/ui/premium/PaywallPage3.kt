package io.silencio.app.ui.premium

import android.app.Activity
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.silencio.app.ui.theme.Background
import io.silencio.app.ui.theme.Surface
import io.silencio.app.ui.theme.TextMuted
import io.silencio.app.ui.theme.TextPrimary
import io.silencio.app.ui.theme.TextSecondary

private val PremiumGold = Color(0xFFD4A847)

@Composable
fun PaywallPage3(
    onPurchase: () -> Unit,
    onSkip: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    var showExitSheet by remember { mutableStateOf(false) }
    val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Navigate on successful purchase
    LaunchedEffect(purchaseState) {
        if (purchaseState is PurchaseState.Success) {
            onPurchase()
        }
    }

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


            Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = "Silencio Premium",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You've already protected your meetings.\nThis protects your relationships too.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(25.dp))

            // price block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(16.dp))
                    .border(1.dp, PremiumGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$3.49",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp),
                    color = PremiumGold,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "once. yours forever.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // what's included
                listOf(
                    "Auto-reply on WhatsApp",
                    "Custom reply message",
                    "See what you missed after every meeting", "Lifetime Access"
                ).forEach { feature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✓", color = PremiumGold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    viewModel.purchasePremium(context as Activity)
                },
                enabled = purchaseState !is PurchaseState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (purchaseState is PurchaseState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color(0xFF1A1400),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Get Silencio Premium",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1A1400),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { viewModel.restorePurchases() }) {
                Text(
                    text = "Restore Purchase",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            TextButton(onClick = { showExitSheet = true }) {
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 17.sp
                )
            }
        }
    }

    // exit intent sheet
    if (showExitSheet) {
        ExitIntentSheet(
            onStay = { showExitSheet = false },
            onLeave = onSkip
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExitIntentSheet(
    onStay: () -> Unit,
    onLeave: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onStay,
        containerColor = Surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Are you sure?",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "People texted you during your last meeting. They got no reply. Next time could be different.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Get Silencio Premium",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF1A1400),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onLeave) {
                Text(
                    text = "I'll risk it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
    }
}