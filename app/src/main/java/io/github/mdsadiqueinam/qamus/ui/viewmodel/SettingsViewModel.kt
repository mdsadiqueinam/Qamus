package io.github.mdsadiqueinam.qamus.ui.viewmodel

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.Settings
import io.github.mdsadiqueinam.qamus.data.repository.BackupRestoreRepository
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.Collections
import javax.inject.Inject

/**
 * Data class representing the UI state for the settings screen.
 */
data class SettingsUIState(
    val settings: Settings = Settings(),
    val isLoading: Boolean = true,
    val showResetConfirmation: Boolean = false,
    val showPermissionDialog: Boolean = false,
    val showAccountPicker: Boolean = false
)

/**
 * ViewModel for the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRestoreRepository: BackupRestoreRepository,
    private val navigator: QamusNavigator
) : ViewModel() {

    // UI state for the settings screen
    private val _uiState = MutableStateFlow(SettingsUIState())
    val uiState: StateFlow<SettingsUIState> = _uiState.asStateFlow()

    // State for error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Load settings when ViewModel is created
        loadSettings()
    }

    /**
     * Load settings from the repository.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            settingsRepository.settings.collectLatest { settings ->
                _uiState.value = SettingsUIState(
                    settings = settings,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Update the reminder interval.
     */
    fun updateReminderInterval(interval: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.updateReminderInterval(interval)
            } catch (e: Exception) {
                _errorMessage.value = "Error updating reminder interval: ${e.message}"
            }
        }
    }

    /**
     * Update the reminder state.
     * disable or enable the reminder.
     */
    fun updateReminderState(isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setReminderEnabled(isEnabled)
            } catch (e: Exception) {
                _errorMessage.value = "Error updating reminder state: ${e.message}"
            }
        }
    }

    /**
     * Update the Google account used for backup/restore.
     */
    fun updateGoogleAccount(accountName: String?) {
        viewModelScope.launch {
            try {
                settingsRepository.updateGoogleAccount(accountName)
            } catch (e: Exception) {
                _errorMessage.value = "Error updating Google account: ${e.message}"
            }
        }
    }

    /**
     * Perform a backup of the database to Google Drive and update the last backup information.
     */
    fun performBackup() {
        viewModelScope.launch {
            try {
                val accountName = uiState.value.settings.googleAccount

                if (accountName == null) {
                    _errorMessage.value = "Please select a Google account first"
                    return@launch
                }

                // Perform the backup
                val backupInfo = backupRestoreRepository.backupToGoogleDrive(accountName)

                if (backupInfo != null) {
                    // Update the last backup information
                    val currentVersion = uiState.value.settings.lastBackupVersion + 1
                    settingsRepository.updateLastBackup(backupInfo.backupAt, currentVersion)
                    _errorMessage.value = "Backup successful!"
                } else {
                    _errorMessage.value = "Backup failed. Please try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error performing backup: ${e.message}"
            }
        }
    }

    /**
     * Show the reset confirmation dialog.
     */
    fun showResetConfirmation() {
        _uiState.value = _uiState.value.copy(showResetConfirmation = true)
    }

    /**
     * Hide the reset confirmation dialog.
     */
    fun hideResetConfirmation() {
        _uiState.value = _uiState.value.copy(showResetConfirmation = false)
    }

    /**
     * Reset settings to default values.
     */
    fun resetSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.resetSettings()
                hideResetConfirmation()
            } catch (e: Exception) {
                _errorMessage.value = "Error resetting settings: ${e.message}"
            }
        }
    }

    /**
     * Perform a restore of the database from Google Drive.
     * Uses the latest backup file with the BACKUP_FILE_PREFIX.
     */
    fun performRestore() {
        viewModelScope.launch {
            try {
                val accountName = uiState.value.settings.googleAccount

                if (accountName == null) {
                    _errorMessage.value = "Please select a Google account first"
                    return@launch
                }

                // Perform the restore using the latest backup
                val success = backupRestoreRepository.restoreFromGoogleDrive(accountName)

                if (success) {
                    _errorMessage.value = "Restore successful!"
                } else {
                    _errorMessage.value = "Restore failed. Please try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error performing restore: ${e.message}"
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Navigate back to the previous screen.
     */
    fun navigateBack() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    /**
     * Show the account picker dialog.
     */
    fun showAccountPicker() {
        _uiState.value = _uiState.value.copy(showAccountPicker = true)
    }

    /**
     * Hide the account picker dialog.
     */
    fun hideAccountPicker() {
        _uiState.value = _uiState.value.copy(showAccountPicker = false)
    }

    /**
     * Create a Google account credential for account selection.
     * @param activity The activity to use for showing the account picker
     * @return The Google account credential
     */
    fun createGoogleAccountCredential(activity: Activity): GoogleAccountCredential {
        return GoogleAccountCredential.usingOAuth2(
            activity,
            Collections.singleton(DriveScopes.DRIVE_FILE)
        )
    }

    /**
     * Handle the result of the account picker.
     * @param resultCode The result code from the account picker
     * @param data The intent data from the account picker
     */
    fun handleAccountPickerResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                updateGoogleAccount(accountName)
            }
        }
        hideAccountPicker()
    }
}
