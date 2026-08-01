package com.example.silencio.ui.premium

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.silencio.data.model.ReplyContact
import com.example.silencio.ui.theme.Background
import com.example.silencio.ui.theme.Divider
import com.example.silencio.ui.theme.Surface
import com.example.silencio.ui.theme.SurfaceVariant
import com.example.silencio.ui.theme.TextPrimary
import com.example.silencio.ui.theme.TextSecondary
import com.example.silencio.ui.theme.PremiumGold
import com.example.silencio.ui.theme.PremiumGoldDim

@Composable
fun ReplyContactPickerScreen(
    onDone: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var contactsPermissionGranted by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.loadContacts()
    }

    BackHandler(enabled = uiState.selectedReplyContactNames.isEmpty()) {
        // do nothing — block back until at least one contact is selected
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 48.dp)
            ) {

                Text(
                    text = "Who should get a reply?",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Only these people will get an auto-reply when you're in a meeting. Everyone else is ignored.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))
                // Trust message — prominent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PremiumGoldDim)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "We only read sender names from notifications. Never message content.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PremiumGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // contact list
            when {
                uiState.isLoadingContacts -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PremiumGold,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                !contactsPermissionGranted && uiState.replyContacts.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = uiState.replyContacts,
                            key = { it.id }
                        ) { contact ->
                            ReplyContactRow(
                                contact = contact,
                                isSelected = contact.name in uiState.selectedReplyContactNames,
                                onToggle = { viewModel.toggleContact(contact.name) }
                            )
                        }
                    }
                }
            }

            // done button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        viewModel.saveReplyContacts()
                        onDone()
                    },
                    enabled = uiState.selectedReplyContactNames.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumGold,
                        disabledContainerColor = PremiumGold.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1A1400),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ReplyContactRow(
    contact: ReplyContact,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = contact.name,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF1A1400),
                checkedTrackColor = PremiumGold,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceVariant,
                uncheckedBorderColor = Divider
            )
        )
    }
}