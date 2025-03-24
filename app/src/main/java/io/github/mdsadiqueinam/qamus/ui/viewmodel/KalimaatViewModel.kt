package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.Kalimaat
import io.github.mdsadiqueinam.qamus.data.model.WordType
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing the UI state for the dictionary screen.
 */
data class KalimaatUIState(
    val searchQuery: String = "",
    val selectedType: WordType? = null
)

/**
 * ViewModel for the dictionary screen.
 */
@HiltViewModel
class KalimaatViewModel @Inject constructor(
    private val repository: KalimaatRepository,
    private val navigator: QamusNavigator
) : ViewModel() {

    // Combined UI state for search and filter
    private val _uiState = MutableStateFlow(KalimaatUIState())
    val uiState: StateFlow<KalimaatUIState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: Flow<PagingData<Kalimaat>> = _uiState.flatMapLatest { state ->
        repository.searchEntries(state.searchQuery, state.selectedType)
    }.cachedIn(viewModelScope)

    // Flow of all entries as a list for dropdowns
    val allEntriesList: Flow<List<Kalimaat>> = repository.getAllEntriesAsList()

    // State for error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Search for dictionary entries with pagination.
     */
    fun searchEntries(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    /**
     * Filter entries by word type with pagination.
     */
    fun filterByType(type: WordType?) {
        _uiState.value = _uiState.value.copy(selectedType = type)
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Navigate to add entry screen
     */
    fun navigateToAddEntry() {
        viewModelScope.launch {
            navigator.navigateToAddEntry()
        }
    }

    /**
     * Navigate to kalima details screen
     * @param entryId The ID of the entry to view
     */
    fun navigateToKalimaDetails(entryId: Long) {
        viewModelScope.launch {
            navigator.navigateToKalimaDetails(entryId)
        }
    }

    /**
     * Navigate back to the previous screen
     */
    fun navigateBack() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }
}
