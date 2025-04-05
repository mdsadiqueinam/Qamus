package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.ErrorMessage
import io.github.mdsadiqueinam.qamus.extension.update
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing the UI state for the dashboard screen.
 * 
 * @property isLoading Whether data is currently being loaded
 */
data class DashboardUIState(
    val isLoading: Boolean = false
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

    // State for error messages
    private val _errorMessage = MutableStateFlow<ErrorMessage>(ErrorMessage.None)
    val errorMessage: StateFlow<ErrorMessage> = _errorMessage.asStateFlow()

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = ErrorMessage.None
    }

    /**
     * Navigate to dictionary screen.
     * Handles navigation to the dictionary feature.
     */
    fun navigateToDictionary() {
        viewModelScope.launch {
            navigator.navigateToDictionary()
        }
    }

    /**
     * Navigate to add entry screen.
     * Handles navigation to the add entry feature.
     */
    fun navigateToAddEntry() {
        viewModelScope.launch {
            navigator.navigateToAddEntry()
        }
    }

    /**
     * Navigate to settings screen.
     * Handles navigation to the settings feature.
     */
    fun navigateToSettings() {
        viewModelScope.launch {
            navigator.navigateToSettings()
        }
    }
}
