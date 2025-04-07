package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.ErrorMessage
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.model.WordType
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import io.github.mdsadiqueinam.qamus.extension.launchWithErrorHandling
import io.github.mdsadiqueinam.qamus.extension.update
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusDestinations
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * Data class representing the UI state for the dictionary screen.
 * 
 * @property searchQuery The current search query
 * @property selectedType The currently selected word type filter
 * @property error The current error message, if any
 */
data class KalimaatUIState(
    val searchQuery: String = "",
    val selectedType: WordType? = null,
    val error: ErrorMessage = ErrorMessage.None
)

/**
 * ViewModel for the dictionary screen.
 * Follows Single Responsibility Principle by focusing only on dictionary data management.
 */
@HiltViewModel
class KalimaatViewModel @Inject constructor(
    private val repository: KalimaatRepository,
    private val navigator: QamusNavigator
) : ViewModel() {

    companion object {
        private const val TAG = "KalimaatViewModel"
    }

    // Combined UI state for search and filter
    private val _uiState = MutableStateFlow(KalimaatUIState())
    val uiState: StateFlow<KalimaatUIState> = _uiState.asStateFlow()

    /**
     * Flow of dictionary entries with pagination.
     * Automatically updates when search query or filter changes.
     * Results are cached and distinct until changed.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: Flow<PagingData<Kalima>> = _uiState
        .flatMapLatest { state ->
            repository.searchEntries(state.searchQuery, state.selectedType)
        }
        .catch { e -> 
            _uiState.update { it.copy(error = ErrorMessage.Message(e.message ?: "Error loading entries")) }
        }
        .cachedIn(viewModelScope)

    /**
     * Search for dictionary entries with pagination.
     * 
     * @param query The search query to filter entries
     */
    fun searchEntries(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Filter entries by word type with pagination.
     * 
     * @param type The word type to filter entries, or null to show all types
     */
    fun filterByType(type: WordType?) {
        _uiState.update { it.copy(selectedType = type) }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = ErrorMessage.None) }
    }

    /**
     * Navigate to kalima details screen.
     * 
     * @param entryId The ID of the entry to view
     */
    fun navigateToKalimaDetails(entryId: Long) {
        launchWithErrorHandling(
            errorHandler = { e -> _uiState.update { it.copy(error = ErrorMessage.Message(e.message ?: "Navigation failed")) } }
        ) {
            navigator.navigate(QamusDestinations.AddEntry(entryId))
        }
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
