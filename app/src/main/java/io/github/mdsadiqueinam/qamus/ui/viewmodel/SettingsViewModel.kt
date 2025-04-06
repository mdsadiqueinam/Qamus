package io.github.mdsadiqueinam.qamus.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.ErrorMessage
import io.github.mdsadiqueinam.qamus.data.model.Settings
import io.github.mdsadiqueinam.qamus.data.repository.BackupRestoreRepository
import io.github.mdsadiqueinam.qamus.data.repository.DataTransferState
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import io.github.mdsadiqueinam.qamus.extension.launchWithErrorHandling
import io.github.mdsadiqueinam.qamus.extension.launchWithLoadingAndErrorHandling
import io.github.mdsadiqueinam.qamus.extension.update
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val error: ErrorMessage = ErrorMessage.None
)

/**
 * Sealed class representing the backup/restore state
 */
sealed class BackupRestoreState {
    data object Idle : BackupRestoreState()
    data class InProgress(
        val progress: Int = 0,
        val transferType: DataTransferState.TransferType,
        val bytesTransferred: Long = 0
    ) : BackupRestoreState()

    data class Error(val message: String) : BackupRestoreState()
    data object Success : BackupRestoreState()
}

/**
 * ViewModel for the settings screen.
 * Follows Single Responsibility Principle by focusing only on settings management.
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

    // Job to keep track of backup/restore operations for cancellation
    private var backupRestoreJob: Job? = null

    init {
        // Load settings when ViewModel is created
        loadSettings()
    }

    /**
     * Load settings from the repository.
     * Results are cached and distinct until changed.
     */
    private fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            launch {
                settingsRepository.settings
                    .distinctUntilChanged()
                    .catch { e -> 
                        _uiState.update { 
                            it.copy(
                                error = ErrorMessage.Message(e.message ?: "Error loading settings"),
                                isLoading = false
                            )
                        }
                    }
                    .collectLatest { settings ->
                        _uiState.update { it.copy(settings = settings, isLoading = false) }
                    }
            }

            launch {
                // Check if the user is signed in
                backupRestoreRepository.observeUserState()
                    .distinctUntilChanged()
                    .catch { e -> 
                        _uiState.update { 
                            it.copy(
                                error = ErrorMessage.Message(e.message ?: "Error checking user state"),
                                isLoading = false
                            )
                        }
                    }
                    .collectLatest { user ->
                        _uiState.update { it.copy(isSignedIn = user != null, user = user) }
                    }
            }
        }
    }

    /**
     * Helper method to handle operation with error handling
     */
    private fun performOperation(
        operation: suspend () -> Unit,
        errorResourceId: Int
    ) {
        launchWithErrorHandling(
            errorHandler = { e -> 
                _uiState.update { 
                    it.copy(
                        error = ErrorMessage.Resource(errorResourceId, e.message ?: ""),
                        isLoading = false
                    )
                }
            }
        ) {
            operation()
        }
    }

    /**
     * Update the reminder interval.
     */
    fun updateReminderInterval(interval: Int) {
        performOperation(
            operation = { settingsRepository.updateReminderInterval(interval) },
            errorResourceId = R.string.error_updating_reminder_interval
        )
    }

    /**
     * Update the reminder state.
     * disable or enable the reminder.
     */
    fun updateReminderState(isEnabled: Boolean) {
        performOperation(
            operation = { settingsRepository.setReminderEnabled(isEnabled) },
            errorResourceId = R.string.error_updating_reminder_state
        )
    }

    /**
     * Update the automatic backup frequency.
     */
    fun updateAutomaticBackupFrequency(frequency: Settings.AutomaticBackupFrequency) {
        performOperation(
            operation = { settingsRepository.updateAutomaticBackupFrequency(frequency) },
            errorResourceId = R.string.error_updating_backup_frequency
        )
    }

    /**
     * Update the mobile data usage setting for automatic backup.
     */
    fun updateUseMobileData(isEnabled: Boolean) {
        performOperation(
            operation = { settingsRepository.setUseMobileData(isEnabled) },
            errorResourceId = R.string.error_updating_mobile_data
        )
    }

    /**
     * Show the reset confirmation dialog.
     */
    fun showResetConfirmation() {
        _uiState.update { it.copy(showResetConfirmation = true) }
    }

    /**
     * Hide the reset confirmation dialog.
     */
    fun hideResetConfirmation() {
        _uiState.update { it.copy(showResetConfirmation = false) }
    }

    /**
     * Reset settings to default values.
     */
    fun resetSettings() {
        performOperation(
            operation = {
                settingsRepository.resetSettings()
                hideResetConfirmation()
            },
            errorResourceId = R.string.error_resetting_settings
        )
    }

    /**
     * Sign in the user
     */
    fun signIn(activity: Context) {
        performOperation(
            operation = { backupRestoreRepository.signIn(activity) },
            errorResourceId = R.string.error_logging_in
        )
    }

    /**
     * Sign out the user
     */
    fun signOut() {
        launchWithErrorHandling(
            errorHandler = { e -> 
                _uiState.update { 
                    it.copy(
                        error = ErrorMessage.Message(e.message ?: "Error signing out"),
                        isLoading = false
                    )
                }
            }
        ) {
            backupRestoreRepository.signOut()
        }
    }

    /**
     * Perform a backup operation
     */
    fun performBackup(activity: Context) {
        startDataTransferOperation(activity, isBackup = true)
    }

    /**
     * Perform a restore operation
     */
    fun performRestore(activity: Context) {
        startDataTransferOperation(activity, isBackup = false)
    }

    /**
     * Start a data transfer operation (backup or restore)
     */
    private fun startDataTransferOperation(activity: Context, isBackup: Boolean) {
        // Cancel any existing job
        cancelBackupRestore()

        // Start new backup/restore job
        backupRestoreJob = viewModelScope.launch {
            try {
                val flow = if (isBackup) backupRestoreRepository.backupDatabase()
                else backupRestoreRepository.restoreDatabase()

                flow.collectLatest { state ->
                    handleDataTransferState(state, activity, isBackup)
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = ErrorMessage.Message(e.message ?: "Error during data transfer"),
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Handle data transfer state updates
     */
    private fun handleDataTransferState(
        state: DataTransferState,
        activity: Context,
        isBackup: Boolean
    ) {
        when (state) {
            is DataTransferState.Success -> {
                _uiState.update { 
                    it.copy(backupRestoreState = BackupRestoreState.Success)
                }

                // Update last backup info if this was a backup operation
                if (isBackup) {
                    launchWithErrorHandling(
                        errorHandler = { e -> 
                            _uiState.update { 
                                it.copy(
                                    error = ErrorMessage.Message(e.message ?: "Error updating backup info"),
                                    isLoading = false
                                )
                            }
                        }
                    ) {
                        val currentTime = Clock.System.now()
                        val currentVersion = uiState.value.settings.lastBackupVersion + 1
                        settingsRepository.updateLastBackup(currentTime, currentVersion)
                    }
                }
            }

            is DataTransferState.Error -> {
                if (state.exception is UserRecoverableAuthIOException) {
                    (activity as Activity).startActivityForResult(
                        state.exception.intent,
                        REQUEST_AUTHORIZATION
                    )
                } else {
                    _uiState.update { 
                        it.copy(backupRestoreState = BackupRestoreState.Error(state.message))
                    }
                }
            }

            is DataTransferState.Uploading -> {
                _uiState.update { 
                    it.copy(
                        backupRestoreState = BackupRestoreState.InProgress(
                            progress = state.progress,
                            transferType = state.type,
                            bytesTransferred = state.bytes
                        )
                    )
                }
            }
        }
    }

    /**
     * Cancel the current backup/restore operation
     */
    fun cancelBackupRestore() {
        backupRestoreJob?.cancel()
        backupRestoreJob = null
        _uiState.update { it.copy(backupRestoreState = BackupRestoreState.Idle) }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = ErrorMessage.None) }
    }

    /**
     * Navigate back to the previous screen.
     */
    fun navigateBack() {
        launchWithErrorHandling(
            errorHandler = { e -> _uiState.update { it.copy(error = ErrorMessage.Message(e.message ?: "Navigation failed")) } }
        ) {
            navigator.navigateBack()
        }
    }
}
