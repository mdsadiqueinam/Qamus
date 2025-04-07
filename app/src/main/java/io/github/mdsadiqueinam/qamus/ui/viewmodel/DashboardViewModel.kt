package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.ErrorMessage
import io.github.mdsadiqueinam.qamus.extension.launchWithErrorHandling
import io.github.mdsadiqueinam.qamus.extension.update
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Data class representing the UI state for the dashboard screen.
 * 
 * @property isLoading Whether data is currently being loaded
 * @property error The current error message, if any
 */
data class DashboardUIState(
    val isLoading: Boolean = false,
    val error: ErrorMessage = ErrorMessage.None
)

/**
 * ViewModel for the dashboard screen.
 * Follows Single Responsibility Principle by focusing only on dashboard navigation and state management.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val navigator: QamusNavigator
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    // UI state for the dashboard
    private val _uiState = MutableStateFlow(DashboardUIState())
    val uiState: StateFlow<DashboardUIState> = _uiState.asStateFlow()

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = ErrorMessage.None) }
    }
}
