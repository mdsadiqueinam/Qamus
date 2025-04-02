package io.github.mdsadiqueinam.qamus.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.Settings
import io.github.mdsadiqueinam.qamus.data.repository.DataTransferState
import io.github.mdsadiqueinam.qamus.ui.viewmodel.BackupRestoreState
import io.github.mdsadiqueinam.qamus.util.formatBytes
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun BackupRestoreContent(
    user: FirebaseUser?,
    lastBackupAt: Instant?,
    lastBackupVersion: Long,
    onBackupClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    backupRestoreState: BackupRestoreState,
    signIn: () -> Unit,
    signOut: () -> Unit,
    currentFrequency: Settings.AutomaticBackupFrequency,
    onFrequencyChanged: (Settings.AutomaticBackupFrequency) -> Unit,
    useMobileData: Boolean,
    onUseMobileDataChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSignedIn = remember(user) { user != null }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isSignedIn) {
            // Show signed-in UI
            LastBackupInfo(lastBackupAt, lastBackupVersion)

            Spacer(modifier = Modifier.height(8.dp))

            AccountSection(
                user = user,
                signIn = signIn,
                signOut = signOut,
                backupRestoreState = backupRestoreState
            )

            Spacer(modifier = Modifier.height(8.dp))

            AutomaticBackupSetting(
                currentFrequency = currentFrequency,
                onFrequencyChanged = onFrequencyChanged
            )

            Spacer(modifier = Modifier.height(8.dp))

            BackupControlsSection(
                backupRestoreState = backupRestoreState,
                onBackupClicked = onBackupClicked,
                onRestoreClicked = onRestoreClicked,
                onCancelClicked = onCancelClicked
            )

            Spacer(modifier = Modifier.height(16.dp))

            MobileDataSection(
                useMobileData = useMobileData,
                onUseMobileDataChanged = onUseMobileDataChanged
            )
        } else {
            // Show signed-out UI
            Text(
                text = stringResource(R.string.sign_in_required),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = signIn,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.sign_in))
            }
        }
    }
}

@Composable
fun LastBackupInfo(
    lastBackupAt: Instant?,
    lastBackupVersion: Long
) {
    if (lastBackupAt != null) {
        val localDateTime = lastBackupAt.toLocalDateTime(TimeZone.currentSystemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val formattedDate = formatter.format(localDateTime.toJavaLocalDateTime())

        Text(
            text = stringResource(R.string.last_backup, formattedDate, lastBackupVersion),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun AccountSection(
    user: FirebaseUser?,
    signIn: () -> Unit,
    signOut: () -> Unit,
    backupRestoreState: BackupRestoreState
) {
    var openEditAccountDialog by remember { mutableStateOf(false) }

    if (openEditAccountDialog) {
        AccountDetailsDialog(
            user = user,
            onDismiss = { openEditAccountDialog = false },
            signIn = signIn,
            signOut = {
                signOut()
                openEditAccountDialog = false
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .clickable {
                if (backupRestoreState !is BackupRestoreState.InProgress) {
                    openEditAccountDialog = true
                }
            }
    ) {
        Text(
            text = stringResource(R.string.google_account),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = user?.email ?: stringResource(R.string.unknown),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun BackupControlsSection(
    backupRestoreState: BackupRestoreState,
    onBackupClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onCancelClicked: () -> Unit
) {
    when (backupRestoreState) {
        is BackupRestoreState.InProgress -> {
            TransferProgressSection(
                backupRestoreState = backupRestoreState,
                onCancelClicked = onCancelClicked
            )
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRestoreClicked,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(stringResource(R.string.restore))
                }

                Button(onClick = onBackupClicked) {
                    Text(stringResource(R.string.backup_now))
                }
            }
        }
    }
}

@Composable
fun TransferProgressSection(
    backupRestoreState: BackupRestoreState.InProgress,
    onCancelClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val isBackup = backupRestoreState.transferType == DataTransferState.TransferType.BACKUP
        val progressText = if (isBackup) {
            stringResource(R.string.backup_in_progress, backupRestoreState.progress)
        } else {
            stringResource(R.string.restore_in_progress, backupRestoreState.progress)
        }

        Text(
            text = progressText,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (backupRestoreState.bytesTransferred > 0) {
            Text(
                text = stringResource(
                    R.string.transferred,
                    formatBytes(backupRestoreState.bytesTransferred)
                ),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        LinearProgressIndicator(
            progress = { backupRestoreState.progress / 100f },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onCancelClicked,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
fun MobileDataSection(
    useMobileData: Boolean,
    onUseMobileDataChanged: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.allow_mobile_data),
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.enabled),
                style = MaterialTheme.typography.bodyMedium
            )

            Switch(
                checked = useMobileData,
                onCheckedChange = onUseMobileDataChanged
            )
        }
    }
}