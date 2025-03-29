package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

/**
 * Data class representing the UI state for the settings screen.
 */
data class SettingsUIState(
    val settings: Settings = Settings(),
    val isLoading: Boolean = true,
    val showResetConfirmation: Boolean = false,
    val showPermissionDialog: Boolean = false
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
     * Perform a backup of the database to Google Drive and update the last backup information.
     */
    fun performBackup() {
        viewModelScope.launch {
            try {
                // For simplicity, using a hardcoded account name
                // In a real implementation, we would show a dialog for the user to select an account
                val accountName = "user@gmail.com" // Replace with actual account selection logic

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
                // For simplicity, using a hardcoded account name
                // In a real implementation, we would show a dialog for the user to select an account
                val accountName = "user@gmail.com" // Replace with actual account selection logic

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
}
