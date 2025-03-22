package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.github.mdsadiqueinam.qamus.data.model.DictionaryEntry
import io.github.mdsadiqueinam.qamus.data.model.WordType
import io.github.mdsadiqueinam.qamus.data.repository.DictionaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for the dictionary screen.
 */
class DictionaryViewModel(private val repository: DictionaryRepository) : ViewModel() {

    // State for all dictionary entries
    private val _entries = MutableStateFlow<List<DictionaryEntry>>(emptyList())
    val entries: StateFlow<List<DictionaryEntry>> = _entries.asStateFlow()

    // State for paginated entries
    private var _entriesPaged: Flow<PagingData<DictionaryEntry>>? = null

    // State for search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // State for selected word type filter
    private val _selectedType = MutableStateFlow<WordType?>(null)
    val selectedType: StateFlow<WordType?> = _selectedType.asStateFlow()

    // State for error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // State for loading indicator
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State for using pagination
    private val _usePagination = MutableStateFlow(true)
    val usePagination: StateFlow<Boolean> = _usePagination.asStateFlow()

    /**
     * Toggle pagination on/off
     */
    fun togglePagination() {
        _usePagination.value = !_usePagination.value
    }

    init {
        if (_usePagination.value) {
            getEntriesPaged()
        } else {
            loadEntries()
        }
    }

    /**
     * Load all dictionary entries.
     */
    fun loadEntries() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllEntries()
                .catch { e ->
                    _errorMessage.value = "Error loading entries: ${e.message}"
                    _isLoading.value = false
                }
                .collect { entries ->
                    _entries.value = entries
                    _isLoading.value = false
                }
        }
    }

    /**
     * Get all dictionary entries with pagination.
     */
    fun getEntriesPaged(): Flow<PagingData<DictionaryEntry>> {
        val lastResult = _entriesPaged
        if (lastResult != null) {
            return lastResult
        }

        val newResult = repository.getEntriesPaged()
            .cachedIn(viewModelScope)
        _entriesPaged = newResult
        return newResult
    }

    /**
     * Search for dictionary entries.
     */
    fun searchEntries(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadEntries()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            repository.searchEntries(query)
                .catch { e ->
                    _errorMessage.value = "Error searching entries: ${e.message}"
                    _isLoading.value = false
                }
                .collect { entries ->
                    _entries.value = entries
                    _isLoading.value = false
                }
        }
    }

    /**
     * Search for dictionary entries with pagination.
     */
    fun searchEntriesPaged(query: String): Flow<PagingData<DictionaryEntry>> {
        _searchQuery.value = query
        if (query.isBlank()) {
            return getEntriesPaged()
        }

        val newResult = repository.searchEntriesPaged(query)
            .cachedIn(viewModelScope)
        _entriesPaged = newResult
        return newResult
    }

    /**
     * Filter entries by word type.
     */
    fun filterByType(type: WordType?) {
        _selectedType.value = type
        if (type == null) {
            loadEntries()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            repository.getEntriesByType(type)
                .catch { e ->
                    _errorMessage.value = "Error filtering entries: ${e.message}"
                    _isLoading.value = false
                }
                .collect { entries ->
                    _entries.value = entries
                    _isLoading.value = false
                }
        }
    }

    /**
     * Filter entries by word type with pagination.
     */
    fun filterByTypePaged(type: WordType?): Flow<PagingData<DictionaryEntry>> {
        _selectedType.value = type
        if (type == null) {
            return getEntriesPaged()
        }

        val newResult = repository.getEntriesByTypePaged(type)
            .cachedIn(viewModelScope)
        _entriesPaged = newResult
        return newResult
    }

    /**
     * Add a new dictionary entry.
     */
    fun addEntry(kalima: String, meaning: String, desc: String, type: WordType, rootId: Long? = null) {
        if (kalima.isBlank() || meaning.isBlank()) {
            _errorMessage.value = "Word and meaning cannot be empty"
            return
        }

        val entry = DictionaryEntry(
            kalima = kalima,
            meaning = meaning,
            desc = desc,
            type = type,
            rootId = rootId
        )

        viewModelScope.launch {
            try {
                repository.insertEntry(entry)
                loadEntries()
            } catch (e: Exception) {
                _errorMessage.value = "Error adding entry: ${e.message}"
            }
        }
    }

    /**
     * Delete a dictionary entry.
     */
    fun deleteEntry(entry: DictionaryEntry) {
        viewModelScope.launch {
            try {
                repository.deleteEntry(entry)
                loadEntries()
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
    class Factory(private val repository: DictionaryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DictionaryViewModel::class.java)) {
                return DictionaryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
