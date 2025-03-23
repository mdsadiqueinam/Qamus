package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.Kalimaat
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing the UI state for the kalima details screen.
 */
data class KalimaDetailsUIState(
    val entry: Kalimaat? = null,
    val isLoading: Boolean = false,
    val rootEntry: Kalimaat? = null
)

/**
 * ViewModel for the kalima details screen.
 */
@HiltViewModel
class KalimaDetailsViewModel @Inject constructor(
    private val repository: KalimaatRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // UI state for the details screen
    private val _uiState = MutableStateFlow(KalimaDetailsUIState(isLoading = true))
    val uiState: StateFlow<KalimaDetailsUIState> = _uiState.asStateFlow()

    // State for error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Get entryId from SavedStateHandle
        val entryId = savedStateHandle.get<Long>("entryId") ?: -1L

        // Load entry if ID is provided
        if (entryId > 0) {
            loadEntry(entryId)
        } else {
            _errorMessage.value = "Invalid entry ID"
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    /**
     * Load an entry by ID.
     */
    private fun loadEntry(id: Long) {
        if (id <= 0) return

        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val entry = repository.getEntryById(id)
                if (entry != null) {
                    // Load root entry if available
                    val rootEntry = entry.rootId?.let { rootId ->
                        repository.getEntryById(rootId)
                    }
                    
                    _uiState.value = KalimaDetailsUIState(
                        entry = entry,
                        isLoading = false,
                        rootEntry = rootEntry
                    )
                } else {
                    _errorMessage.value = "Entry not found"
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading entry: ${e.message}"
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}