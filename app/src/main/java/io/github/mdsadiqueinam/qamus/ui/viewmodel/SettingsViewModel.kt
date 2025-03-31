package io.github.mdsadiqueinam.qamus.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.Settings
import io.github.mdsadiqueinam.qamus.data.repository.BackupRestoreRepository
import io.github.mdsadiqueinam.qamus.data.repository.DataTransferState
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
    val backupRestoreState: BackupRestoreState = BackupRestoreState.Idle,
)

sealed class BackupRestoreState {
    object Idle : BackupRestoreState()
    data class InProgress(
        val progress: Int = 0, val transferType: DataTransferState.TransferType, val bytesTransferred: Long = 0
    ) : BackupRestoreState()

    data class Error(val message: String) : BackupRestoreState()
    object Success : BackupRestoreState()
}

/**
 * ViewModel for the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRestoreRepository: BackupRestoreRepository,
    private val navigator: QamusNavigator
) : ViewModel() {
    companion object {
        private const val REQUEST_AUTHORIZATION = 1001
    }

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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            launch {
                settingsRepository.settings.collectLatest { settings ->
                    _uiState.value = _uiState.value.copy(settings = settings, isLoading = false)
                }
            }

            launch {
                // Check if the user is signed in
                backupRestoreRepository.observeUserState().collectLatest { user ->
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

    // Job to keep track of backup/restore operations for cancellation
    private var backupRestoreJob: kotlinx.coroutines.Job? = null

    fun performBackup(activity: Context) {
        // Cancel any existing job
        backupRestoreJob?.cancel()

        // Reset state to idle
        _uiState.value = _uiState.value.copy(
            backupRestoreState = BackupRestoreState.Idle
        )

        // Start new backup job
        backupRestoreJob = viewModelScope.launch {
            backupRestoreRepository.backupDatabase().collectLatest {
                when (it) {
                    is DataTransferState.Success -> {
                        _uiState.value = _uiState.value.copy(
                            backupRestoreState = BackupRestoreState.Success
                        )
                        // Update last backup time and version
                        val currentTime = Clock.System.now()
                        val currentVersion = uiState.value.settings.lastBackupVersion + 1
                        settingsRepository.updateLastBackup(currentTime, currentVersion)
                    }

                    is DataTransferState.Error -> {
                        if (it.exception is UserRecoverableAuthIOException) {
                            (activity as Activity).startActivityForResult(it.exception.intent, REQUEST_AUTHORIZATION)
                        } else {
                            _errorMessage.value = "Error: ${it.message}"
                            _uiState.value = _uiState.value.copy(
                                backupRestoreState = BackupRestoreState.Idle
                            )
                        }
                    }

                    is DataTransferState.Uploading -> {
                        _uiState.value = _uiState.value.copy(
                            backupRestoreState = BackupRestoreState.InProgress(
                                progress = it.progress,
                                transferType = it.type,
                                bytesTransferred = 0 // We don't have bytes info in the current implementation
                            )
                        )
                    }
                }
            }
        }
    }

    fun performRestore(activity: Context) {
        // Cancel any existing job
        backupRestoreJob?.cancel()

        // Reset state to idle
        _uiState.value = _uiState.value.copy(
            backupRestoreState = BackupRestoreState.Idle
        )

        // Start new restore job
        backupRestoreJob = viewModelScope.launch {
            backupRestoreRepository.restoreDatabase().collectLatest {
                when (it) {
                    is DataTransferState.Success -> {
                        _uiState.value = _uiState.value.copy(
                            backupRestoreState = BackupRestoreState.Success
                        )
                    }

                    is DataTransferState.Error -> {
                        if (it.exception is UserRecoverableAuthIOException) {
                            (activity as Activity).startActivityForResult(it.exception.intent, REQUEST_AUTHORIZATION)
                        } else {
                            _errorMessage.value = "Error: ${it.message}"
                            _uiState.value = _uiState.value.copy(
                                backupRestoreState = BackupRestoreState.Idle
                            )
                        }
                    }

                    is DataTransferState.Uploading -> {
                        _uiState.value = _uiState.value.copy(
                            backupRestoreState = BackupRestoreState.InProgress(
                                progress = it.progress,
                                transferType = it.type,
                                bytesTransferred = 0 // We don't have bytes info in the current implementation
                            )
                        )
                    }
                }
            }
        }
    }

    fun cancelBackupRestore() {
        backupRestoreJob?.cancel()
        backupRestoreJob = null
        _uiState.value = _uiState.value.copy(
            backupRestoreState = BackupRestoreState.Idle
        )
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
