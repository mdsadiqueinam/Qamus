package io.github.mdsadiqueinam.qamus.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseUser
import io.github.mdsadiqueinam.qamus.data.model.Settings
import io.github.mdsadiqueinam.qamus.data.repository.DataTransferState
import io.github.mdsadiqueinam.qamus.ui.viewmodel.BackupRestoreState
import io.github.mdsadiqueinam.qamus.ui.viewmodel.SettingsViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.lang.Math.pow
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val localContext = LocalContext.current

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
                    user = uiState.user,
                    onReminderIntervalChanged = { viewModel.updateReminderInterval(it) },
                    onBackupClicked = { viewModel.performBackup(localContext) },
                    onReminderStateChanged = { viewModel.updateReminderState(it) },
                    signIn = { viewModel.signIn(localContext) },
                    signOut = { viewModel.signOut() },
                    backupRestoreState = uiState.backupRestoreState,
                    onRestoreClicked = { viewModel.performRestore(localContext) },
                    onCancelClicked = { viewModel.cancelBackupRestore() },
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsContent(
    settings: Settings,
    user: FirebaseUser?,
    onReminderIntervalChanged: (Int) -> Unit,
    onBackupClicked: () -> Unit,
    onReminderStateChanged: (Boolean) -> Unit,
    signIn: () -> Unit,
    signOut: () -> Unit,
    backupRestoreState: BackupRestoreState = BackupRestoreState.Idle,
    onRestoreClicked: () -> Unit = {},
    onCancelClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)
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
            })

        // Backup Setting
        SettingCard(
            title = "Backup & Restore", description = "Backup and restore your dictionary to prevent data loss", content = {
                BackupSetting(
                    user = user,
                    lastBackupAt = settings.lastBackupAt,
                    lastBackupVersion = settings.lastBackupVersion,
                    onBackupClicked = onBackupClicked,
                    onRestoreClicked = onRestoreClicked,
                    onCancelClicked = onCancelClicked,
                    backupRestoreState = backupRestoreState,
                    signIn = signIn,
                    sinOut = signOut
                )
            })

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
            value = sliderPosition, onValueChange = {
            val snappedValue = ((it / step).roundToInt() * step).toFloat()
            sliderPosition = snappedValue.coerceIn(valueRange)
        }, onValueChangeFinished = {
            // Convert minutes back to milliseconds
            onIntervalChanged(sliderPosition.toInt())
        }, valueRange = valueRange, steps = steps - 1, modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "10m", style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "3h", style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reminder enable or disable settings
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enable Reminder", style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                checked = isEnabledReminder, onCheckedChange = { isEnabled ->
                    // Simply enable or disable the reminder without permission check
                    onReminderStateChanged(isEnabled)
                })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupSetting(
    user: FirebaseUser?,
    lastBackupAt: Instant?,
    lastBackupVersion: Long,
    onBackupClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    backupRestoreState: BackupRestoreState,
    signIn: () -> Unit,
    sinOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSignedIn = remember(user) { user != null }
    var openEditAccountDialog by remember { mutableStateOf(false) }

    if (openEditAccountDialog) {
        Dialog(onDismissRequest = { openEditAccountDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Google Account", style = MaterialTheme.typography.titleLarge)

                    Column {
                        Text(text = "Name", style = MaterialTheme.typography.titleMedium)
                        Text(text = user?.displayName ?: "Unknown", style = MaterialTheme.typography.bodyMedium)
                    }

                    Column {
                        Text(text = "Email", style = MaterialTheme.typography.titleMedium)
                        Text(text = user?.email ?: "Unknown", style = MaterialTheme.typography.bodyMedium)
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // change account button
                        ElevatedButton(onClick = signIn) {
                            Text("Change Account")
                        }

                        // logout button
                        FilledTonalButton(onClick = {
                            sinOut()
                            openEditAccountDialog = false
                        }) {
                            Text("Logout")
                        }

                        TextButton(onClick = { openEditAccountDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }

    if (isSignedIn) {
        Column(modifier = modifier.fillMaxWidth()) {
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

            Column(modifier = Modifier.fillMaxWidth().clickable { openEditAccountDialog = true }) {
                Text(text = "Google Account", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = user?.email ?: "Unknown", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Show progress or buttons based on state
            when (backupRestoreState) {
                is BackupRestoreState.InProgress -> {
                    // Show progress indicator and cancel button
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val operationType = if (backupRestoreState.transferType == DataTransferState.TransferType.BACKUP) 
                            "Backup" else "Restore"

                        Text(
                            text = "$operationType in progress: ${backupRestoreState.progress}%",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Show bytes transferred if available
                        if (backupRestoreState.bytesTransferred > 0) {
                            Text(
                                text = "Transferred: ${formatBytes(backupRestoreState.bytesTransferred)}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        LinearProgressIndicator(
                            progress = { backupRestoreState.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Cancel button
                        Button(
                            onClick = onCancelClicked,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
                else -> {
                    // Show normal buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Restore button
                        OutlinedButton(
                            onClick = onRestoreClicked,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Restore")
                        }

                        // Backup button
                        Button(
                            onClick = onBackupClicked
                        ) {
                            Text("Backup Now")
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "You need to sign in to backup your dictionary",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = signIn, modifier = Modifier.align(Alignment.End)
            ) {
                Text("Sign In")
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

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()

    return String.format(Locale.getDefault(), "%.1f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}
