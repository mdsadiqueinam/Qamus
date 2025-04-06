package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.ErrorMessage
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import io.github.mdsadiqueinam.qamus.extension.launchWithLoadingAndErrorHandling
import io.github.mdsadiqueinam.qamus.extension.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Data class representing the UI state for the reminder screen.
 * 
 * @property kalima The current Kalima being displayed
 * @property isLoading Whether data is currently being loaded
 * @property error The current error message, if any
 */
data class ReminderUIState(
    val kalima: Kalima? = null,
    val isLoading: Boolean = true,
    val error: ErrorMessage = ErrorMessage.None
)

/**
 * ViewModel for the Kalima reminder screen.
 * Follows Single Responsibility Principle by focusing only on reminder functionality.
 */
@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val kalimaatRepository: KalimaatRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ReminderViewModel"
    }

    // UI state for the reminder screen
    private val _uiState = MutableStateFlow(ReminderUIState())
    val uiState: StateFlow<ReminderUIState> = _uiState.asStateFlow()

    init {
        loadRandomKalima()
    }

    /**
     * Load a random Kalima entry from the repository.
     * Results are cached and distinct until changed.
     */
    fun loadRandomKalima() {
        launchWithLoadingAndErrorHandling(
            loadingState = _uiState,
            updateLoading = { state, isLoading -> state.copy(isLoading = isLoading) },
            errorHandler = { e -> 
                _uiState.update { 
                    it.copy(
                        error = ErrorMessage.Message("Error loading Kalima: ${e.message ?: ""}"),
                        isLoading = false
                    )
                }
            }
        ) {
            val randomKalima = kalimaatRepository.getRandomEntry()
            if (randomKalima != null) {
                _uiState.update { 
                    it.copy(
                        kalima = randomKalima,
                        error = ErrorMessage.None
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        error = ErrorMessage.Message("No Kalima entries found")
                    )
                }
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = ErrorMessage.None) }
    }
}
