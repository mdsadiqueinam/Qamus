package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.github.mdsadiqueinam.qamus.data.model.Kalimaat
import io.github.mdsadiqueinam.qamus.data.model.WordType
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
class KalimaatViewModel(private val repository: KalimaatRepository) : ViewModel() {

    // State for entries
    private var _entries: Flow<PagingData<Kalimaat>>? = null

    // Combined UI state for search and filter
    private val _uiState = MutableStateFlow(KalimaatUIState())
    val uiState: StateFlow<KalimaatUIState> = _uiState.asStateFlow()

    // State for error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        getEntries()
    }

    /**
     * Get all dictionary entries with pagination.
     */
    fun getEntries(): Flow<PagingData<Kalimaat>> {
        val lastResult = _entries
        if (lastResult != null) {
            return lastResult
        }

        val newResult = repository.getEntries()
            .cachedIn(viewModelScope)
        _entries = newResult
        return newResult
    }

    /**
     * Search for dictionary entries with pagination.
     */
    fun searchEntries(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            return
        }

        val newResult = repository.searchEntries(query)
            .cachedIn(viewModelScope)
        _entries = newResult
    }

    /**
     * Filter entries by word type with pagination.
     */
    fun filterByType(type: WordType?): Flow<PagingData<Kalimaat>> {
        _uiState.value = _uiState.value.copy(selectedType = type)
        if (type == null) {
            return getEntries()
        }

        val newResult = repository.getEntriesByType(type)
            .cachedIn(viewModelScope)
        _entries = newResult
        return newResult
    }

    /**
     * Add a new dictionary entry.
     */
    fun addEntry(huroof: String, meaning: String, desc: String, type: WordType, rootId: Long? = null) {
        if (huroof.isBlank() || meaning.isBlank()) {
            _errorMessage.value = "Word and meaning cannot be empty"
            return
        }

        val entry = Kalimaat(
            huroof = huroof,
            meaning = meaning,
            desc = desc,
            type = type,
            rootId = rootId
        )

        viewModelScope.launch {
            try {
                repository.insertEntry(entry)
                // No need to call loadEntries() as the paging library will handle updates
            } catch (e: Exception) {
                _errorMessage.value = "Error adding entry: ${e.message}"
            }
        }
    }

    /**
     * Delete a dictionary entry.
     */
    fun deleteEntry(entry: Kalimaat) {
        viewModelScope.launch {
            try {
                repository.deleteEntry(entry)
                // No need to call loadEntries() as the paging library will handle updates
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting entry: ${e.message}"
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
     * Factory for creating DictionaryViewModel instances.
     */
    class Factory(private val repository: KalimaatRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(KalimaatViewModel::class.java)) {
                return KalimaatViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
