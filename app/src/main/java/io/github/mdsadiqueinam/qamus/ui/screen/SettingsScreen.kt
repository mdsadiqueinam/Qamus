package io.github.mdsadiqueinam.qamus.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mdsadiqueinam.qamus.data.model.Settings
import io.github.mdsadiqueinam.qamus.ui.viewmodel.SettingsViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LocalContext.current

    // Show error message in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Reset confirmation dialog
    if (uiState.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.hideResetConfirmation() },
            title = { Text("Reset Settings") },
            text = { Text("Are you sure you want to reset all settings to default values?") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetSettings() }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideResetConfirmation() }) {
                    Text("Cancel")
                }
            })
    }

    // Account picker dialog
    if (uiState.showAccountPicker) {
        AccountPickerDialog(
            accounts = uiState.googleAccounts,
            selectedAccount = uiState.settings.googleAccount,
            onAccountSelected = { accountName ->
                viewModel.updateGoogleAccount(accountName)
                viewModel.hideAccountPicker()
            },
            onDismiss = { viewModel.hideAccountPicker() }
        )
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(
                text = "Settings", modifier = Modifier.fillMaxWidth()
            )
        }, navigationIcon = {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }, actions = {
            IconButton(onClick = { viewModel.showResetConfirmation() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Settings")
            }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                SettingsContent(
                    settings = uiState.settings,
                    onReminderIntervalChanged = { viewModel.updateReminderInterval(it) },
                    onBackupClicked = { viewModel.performBackup() },
                    onRestoreClicked = {
                        // Will automatically use the latest backup file
                        viewModel.performRestore()
                    },
                    onReminderStateChanged = { viewModel.updateReminderState(it) },
                    onSelectAccountClicked = {
                        // Show account picker dialog
                        viewModel.showAccountPicker()
                    },
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsContent(
    settings: Settings,
    onReminderIntervalChanged: (Int) -> Unit,
    onBackupClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onReminderStateChanged: (Boolean) -> Unit,
    onSelectAccountClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Reminder Interval Setting
        SettingCard(
            title = "Reminder Interval",
            description = "Set how often you want to be reminded to backup your dictionary",
            content = {
                ReminderSetting(
                    currentInterval = settings.reminderInterval,
                    onIntervalChanged = onReminderIntervalChanged,
                    isEnabledReminder = settings.isReminderEnabled,
                    onReminderStateChanged = onReminderStateChanged
                )
            }
        )

        // Backup Setting
        SettingCard(
            title = "Backup", description = "Backup your dictionary to prevent data loss", content = {
                BackupSetting(
                    lastBackupAt = settings.lastBackupAt,
                    lastBackupVersion = settings.lastBackupVersion,
                    googleAccount = settings.googleAccount,
                    onBackupClicked = onBackupClicked,
                    onRestoreClicked = onRestoreClicked,
                    onSelectAccountClicked = onSelectAccountClicked
                )
            }
        )

        // Reminder enable or disable settings
    }
}

@Composable
fun SettingCard(
    title: String, description: String, content: @Composable () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description, style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
fun ReminderSetting(
    currentInterval: Int,
    onIntervalChanged: (Int) -> Unit,
    isEnabledReminder: Boolean,
    onReminderStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val valueRange = 10f..180f
    val step = 10
    val steps = ((valueRange.endInclusive - valueRange.start) / step).toInt()
    LocalContext.current

    // Slider state (in minutes)
    var sliderPosition by remember(currentInterval) {
        mutableFloatStateOf(currentInterval.toFloat())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Remind every ${formatTime(sliderPosition)} minutes",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderPosition,
            onValueChange = {
                val snappedValue = ((it / step).roundToInt() * step).toFloat()
                sliderPosition = snappedValue.coerceIn(valueRange)
            },
            onValueChangeFinished = {
                // Convert minutes back to milliseconds
                onIntervalChanged(sliderPosition.toInt())
            },
            valueRange = valueRange,
            steps = steps - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "10m",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "3h",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reminder enable or disable settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enable Reminder",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                checked = isEnabledReminder,
                onCheckedChange = { isEnabled ->
                    // Simply enable or disable the reminder without permission check
                    onReminderStateChanged(isEnabled)
                }
            )
        }
    }
}

@Composable
fun BackupSetting(
    lastBackupAt: Instant?,
    lastBackupVersion: Long,
    googleAccount: String?,
    onBackupClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onSelectAccountClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasAccount = googleAccount != null

    Column(modifier = modifier.fillMaxWidth()) {
        // Google Account selection
        Box(
            modifier = Modifier.clickable(enabled = true) { onSelectAccountClicked() }.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(4.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Google account",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(googleAccount ?: "Select account")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Last backup info
        if (lastBackupAt != null) {
            val localDateTime = lastBackupAt.toLocalDateTime(TimeZone.currentSystemDefault())
            val formattedDate = "${localDateTime.date} ${localDateTime.time}"

            Text(
                text = "Last backup: $formattedDate (v$lastBackupVersion)",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            // Restore button
            Button(
                onClick = onRestoreClicked,
                enabled = hasAccount
            ) {
                Text("Restore Now")
            }

            // Backup button
            Button(
                onClick = onBackupClicked,
                enabled = hasAccount
            ) {
                Text("Backup Now")
            }
        }
    }
}

fun formatTime(minutes: Float): String {
    val hours = (minutes / 60).toInt()
    val mins = (minutes % 60).toInt()

    return if (hours > 0) {
        "$hours hours $mins minutes"
    } else {
        "$mins minutes"
    }
}

/**
 * A custom dialog to display the list of Google accounts and allow the user to select one.
 */
@Composable
fun AccountPickerDialog(
    accounts: List<String>,
    selectedAccount: String?,
    onAccountSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Google Account") },
        text = {
            if (accounts.isEmpty()) {
                Text("No Google accounts found on this device.")
            } else {
                LazyColumn {
                    items(accounts) { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAccountSelected(account) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = account == selectedAccount,
                                onClick = { onAccountSelected(account) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = account,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
