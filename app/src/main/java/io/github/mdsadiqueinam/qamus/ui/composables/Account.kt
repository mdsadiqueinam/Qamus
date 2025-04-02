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
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseUser
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.Settings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountDetailsDialog(
    user: FirebaseUser?,
    onDismiss: () -> Unit,
    signIn: () -> Unit,
    signOut: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.google_account),
                    style = MaterialTheme.typography.titleLarge
                )

                Column {
                    Text(
                        text = stringResource(R.string.name),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = user?.displayName ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.email),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = user?.email ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(onClick = signIn) {
                        Text(stringResource(R.string.change_account))
                    }

                    FilledTonalButton(onClick = signOut) {
                        Text(stringResource(R.string.logout))
                    }

                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            }
        }
    }
}

@Composable
fun AutomaticBackupSetting(
    currentFrequency: Settings.AutomaticBackupFrequency,
    onFrequencyChanged: (Settings.AutomaticBackupFrequency) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AutomaticBackupFrequencyDialog(
            currentFrequency = currentFrequency,
            onFrequencyChanged = {
                onFrequencyChanged(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth()
            .clickable { showDialog = true }
    ) {
        Text(
            text = stringResource(R.string.automatic_backup),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentFrequency.value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun AutomaticBackupFrequencyDialog(
    currentFrequency: Settings.AutomaticBackupFrequency,
    onFrequencyChanged: (Settings.AutomaticBackupFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.automatic_backup_frequency),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                val options = Settings.AutomaticBackupFrequency.entries

                options.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onFrequencyChanged(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFrequency == option,
                            onClick = { onFrequencyChanged(option) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = option.value,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}