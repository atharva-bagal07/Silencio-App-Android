package com.example.silencio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.silencio.ui.theme.Background
import com.example.silencio.ui.theme.Divider
import com.example.silencio.ui.theme.StatusActive
import com.example.silencio.ui.theme.Surface
import com.example.silencio.ui.theme.SurfaceVariant
import com.example.silencio.ui.theme.TextMuted
import com.example.silencio.ui.theme.TextPrimary
import com.example.silencio.ui.theme.TextSecondary


@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCalendarPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontSize = 32.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── Calendar ─────────────────────────────────────────────
            SectionLabel(text = "CALENDAR")

            SettingsCard {
                ToggleRow(
                    label = "Auto-silence during events",
                    checked = uiState.autoSilenceEnabled,
                    onCheckedChange = viewModel::setAutoSilenceEnabled
                )

                SettingsDivider()

                ChevronRow(
                    label = "Calendars to watch",
                    subtitle = uiState.watchedCalendarNames.ifEmpty { "All calendars" },
                    onClick = { showCalendarPicker = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── VIP Contacts ──────────────────────────────────────────
            SectionLabel(text = "VIP CONTACTS")

            SettingsCard {
                ChevronRow(
                    label = "Manage VIP contacts",
                    subtitle = when (uiState.vipContactCount) {
                        0 -> "None selected"
                        1 -> "1 contact"
                        else -> "${uiState.vipContactCount} contacts"
                    },
                    onClick = { /* navigate to VIP contact picker */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── About ────────────────────────────────────────────────
            SectionLabel(text = "ABOUT")

            SettingsCard {
                ChevronRow(
                    label = "How Silencio works",
                    subtitle = null,
                    onClick = { /* open explainer */ }
                )

                SettingsDivider()

                ChevronRow(
                    label = "Privacy",
                    subtitle = null,
                    onClick = { /* open privacy policy */ }
                )

                SettingsDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showCalendarPicker) {
        CalendarPickerSheet(
            availableCalendars = uiState.availableCalendars,
            selectedIds = uiState.watchedCalendarIds,
            onDismiss = { showCalendarPicker = false },
            onConfirm = { ids ->
                viewModel.setWatchedCalendarIds(ids)
                showCalendarPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarPickerSheet(
    availableCalendars: List<Pair<Long, String>>,
    selectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var selected by remember { mutableStateOf(selectedIds) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Calendars to watch",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            availableCalendars.forEach { (id, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (id in selected) selected - id else selected + id
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = id in selected,
                        onCheckedChange = {
                            selected = if (it) selected + id else selected - id
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = StatusActive,
                            uncheckedColor = TextSecondary
                        )
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Divider)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onConfirm(selected) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusActive,
                    disabledContainerColor = SurfaceVariant
                )
            ) {
                Text(
                    text = "Done",
                    color = if (selected.isNotEmpty()) TextPrimary else TextSecondary
                )
            }
        }
    }
}

// ─── Reusable Components ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
    ) {
        content()
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            fontSize = 17.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = StatusActive,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceVariant,
                uncheckedBorderColor = Divider
            )
        )
    }
}

@Composable
private fun ChevronRow(
    label: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontSize = 17.sp
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = Divider
    )
}