package io.github.mdsadiqueinam.qamus.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.Settings
import io.github.mdsadiqueinam.qamus.data.repository.BackupRestoreRepository
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import kotlinx.coroutines.Dispatchers
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
    val showPermissionDialog: Boolean = false,
    val isSignedIn: Boolean = false,
    val user: FirebaseUser? = null,
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
        Log.d("SettingsViewModel", "ViewModel initialized") // Add this log
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            launch {
                settingsRepository.settings.collectLatest { settings ->
                    _uiState.value = SettingsUIState(
                        settings = settings, isLoading = false
                    )
                }
            }

            launch {
                // Check if the user is signed in
                backupRestoreRepository.observeUserState().collectLatest { user ->
                    Log.d("SettingsViewModel", "user collected: $user")
                    _uiState.value = _uiState.value.copy(isSignedIn = user != null, user = user)
                }
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
     * Simulate a backup by updating the last backup information.
     */
    fun performBackup() {
        viewModelScope.launch {
            try {
                val currentTime = Clock.System.now()
                val currentVersion = uiState.value.settings.lastBackupVersion + 1
                settingsRepository.updateLastBackup(currentTime, currentVersion)
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

    fun signIn(activity: Context) {
        viewModelScope.launch {
            try {// Check if the user is signed in
                backupRestoreRepository.signIn(activity)
            } catch (e: Exception) {
                _errorMessage.value = "Error logging in: ${e.message}"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            backupRestoreRepository.signOut()
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
