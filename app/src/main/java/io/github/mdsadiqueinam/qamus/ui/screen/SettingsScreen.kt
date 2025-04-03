package io.github.mdsadiqueinam.qamus.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.mdsadiqueinam.qamus.extension.ShowSnackbar
import io.github.mdsadiqueinam.qamus.ui.composables.*
import io.github.mdsadiqueinam.qamus.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val localContext = LocalContext.current

    snackbarHostState.ShowSnackbar(errorMessage) {
        viewModel.clearError()
    }

    if (uiState.showResetConfirmation) {
        ResetConfirmationDialog(
            onConfirm = { viewModel.resetSettings() },
            onDismiss = { viewModel.hideResetConfirmation() }
        )
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                onNavigateBack = { viewModel.navigateBack() },
                onResetClicked = { viewModel.showResetConfirmation() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                SettingsContent(
                    settings = uiState.settings,
                    user = uiState.user,
                    onReminderIntervalChanged = { viewModel.updateReminderInterval(it) },
                    onBackupClicked = { viewModel.performBackup(localContext) },
                    onReminderStateChanged = { viewModel.updateReminderState(it) },
                    onAutomaticBackupFrequencyChanged = { viewModel.updateAutomaticBackupFrequency(it) },
                    onUseMobileDataChanged = { viewModel.updateUseMobileData(it) },
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