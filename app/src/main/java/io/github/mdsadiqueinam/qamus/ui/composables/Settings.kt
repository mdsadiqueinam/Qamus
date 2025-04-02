package io.github.mdsadiqueinam.qamus.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.Settings
import io.github.mdsadiqueinam.qamus.ui.viewmodel.BackupRestoreState

@Composable
fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_confirmation_title)) },
        text = { Text(stringResource(R.string.reset_confirmation_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.reset))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    onNavigateBack: () -> Unit,
    onResetClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.settings_title),
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = {
            IconButton(onClick = onResetClicked) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_settings))
            }
        }
    )
}

@Composable
fun SettingsContent(
    settings: Settings,
    user: FirebaseUser?,
    onReminderIntervalChanged: (Int) -> Unit,
    onBackupClicked: () -> Unit,
    onReminderStateChanged: (Boolean) -> Unit,
    onAutomaticBackupFrequencyChanged: (Settings.AutomaticBackupFrequency) -> Unit,
    onUseMobileDataChanged: (Boolean) -> Unit,
    signIn: () -> Unit,
    signOut: () -> Unit,
    backupRestoreState: BackupRestoreState = BackupRestoreState.Idle,
    onRestoreClicked: () -> Unit = {},
    onCancelClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Reminder Interval Setting
        SettingCard(
            title = stringResource(R.string.reminder_interval_title),
            description = stringResource(R.string.reminder_interval_desc),
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
            title = stringResource(R.string.backup_restore_title),
            description = stringResource(R.string.backup_restore_desc),
            content = {
                BackupRestoreContent(
                    user = user,
                    lastBackupAt = settings.lastBackupAt,
                    lastBackupVersion = settings.lastBackupVersion,
                    onBackupClicked = onBackupClicked,
                    onRestoreClicked = onRestoreClicked,
                    onCancelClicked = onCancelClicked,
                    backupRestoreState = backupRestoreState,
                    signIn = signIn,
                    signOut = signOut,
                    currentFrequency = settings.automaticBackupFrequency,
                    onFrequencyChanged = onAutomaticBackupFrequencyChanged,
                    useMobileData = settings.useMobileData,
                    onUseMobileDataChanged = onUseMobileDataChanged
                )
            }
        )
    }
}

@Composable
fun SettingCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}