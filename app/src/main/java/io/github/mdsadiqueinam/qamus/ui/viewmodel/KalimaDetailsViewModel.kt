package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.ErrorMessage
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import io.github.mdsadiqueinam.qamus.extension.launchWithErrorHandling
import io.github.mdsadiqueinam.qamus.extension.launchWithLoadingAndErrorHandling
import io.github.mdsadiqueinam.qamus.extension.update
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Data class representing the UI state for the kalima details screen.
 * 
 * @property entry The current dictionary entry being displayed
 * @property isLoading Whether data is currently being loaded
 * @property rootEntry The root entry if the current entry is derived from a root
 * @property relatedEntries List of entries related to the current entry
 * @property error The current error message, if any
 */
data class KalimaDetailsUIState(
    val entry: Kalima? = null,
    val isLoading: Boolean = false,
    val rootEntry: Kalima? = null,
    val relatedEntries: List<Kalima> = emptyList(),
    val error: ErrorMessage = ErrorMessage.None
)

/**
 * ViewModel for the kalima details screen.
 * Follows Single Responsibility Principle by focusing only on entry details management.
 */
@HiltViewModel
class KalimaDetailsViewModel @Inject constructor(
    private val repository: KalimaatRepository,
    private val savedStateHandle: SavedStateHandle,
    private val navigator: QamusNavigator
) : ViewModel() {

    companion object {
        private const val TAG = "KalimaDetailsViewModel"
    }

    // UI state for the details screen
    private val _uiState = MutableStateFlow(KalimaDetailsUIState(isLoading = true))
    val uiState: StateFlow<KalimaDetailsUIState> = _uiState.asStateFlow()

    init {
        // Get entryId from SavedStateHandle
        val entryId = savedStateHandle.get<Long>("entryId") ?: -1L

        // Load entry if ID is provided
        if (entryId > 0) {
            loadEntry(entryId)
        } else {
            _uiState.update { 
                it.copy(
                    error = ErrorMessage.Message("Invalid entry ID"),
                    isLoading = false
                )
            }
        }
    }

    /**
     * Load an entry by ID.
     * Optimized to use parallel database calls with coroutines.
     * 
     * @param id The ID of the entry to load
     */
    private fun loadEntry(id: Long) {
        if (id <= 0) return

        launchWithLoadingAndErrorHandling(
            loadingState = _uiState,
            updateLoading = { state, isLoading -> state.copy(isLoading = isLoading) },
            errorHandler = { e -> 
                _uiState.update { 
                    it.copy(
                        error = ErrorMessage.Resource(R.string.error_loading_entry, e.message ?: ""),
                        isLoading = false
                    )
                }
            }
        ) {
            val entry = repository.getEntryById(id)
            if (entry != null) {
                // Use coroutines to parallelize database calls
                val rootEntryDeferred = viewModelScope.async {
                    entry.rootId?.let { rootId ->
                        repository.getEntryById(rootId)
                    }
                }

                val relatedEntriesDeferred = viewModelScope.async {
                    if (entry.rootId != null) {
                        // Get entries with the same rootId as the current entry
                        repository.getEntriesByRootId(entry.rootId)
                            .filter { it.id != entry.id } // Exclude the current entry
                    } else if (entry.id > 0) {
                        // If this is a root entry, get entries that have this entry as their root
                        repository.getEntriesByRootId(entry.id)
                    } else {
                        emptyList()
                    }
                }

                // Await results from parallel calls
                val rootEntry = rootEntryDeferred.await()
                val relatedEntries = relatedEntriesDeferred.await()

                // Update UI state once with all data
                _uiState.update {
                    KalimaDetailsUIState(
                        entry = entry,
                        isLoading = false,
                        rootEntry = rootEntry,
                        relatedEntries = relatedEntries,
                        error = ErrorMessage.None
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        error = ErrorMessage.Resource(R.string.entry_not_found),
                        isLoading = false
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

    /**
     * Delete the current entry and navigate back.
     */
    fun deleteEntry() {
        val entry = _uiState.value.entry ?: return

        launchWithErrorHandling(
            errorHandler = { e -> 
                _uiState.update { 
                    it.copy(
                        error = ErrorMessage.Resource(R.string.error_generic, "Error deleting entry: ${e.message ?: ""}")
                    )
                }
            }
        ) {
            repository.deleteEntry(entry)
            navigator.navigateBack()
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

    /**
     * Navigate to edit entry screen.
     * 
     * @param entryId The ID of the entry to edit
     */
    fun navigateToEditEntry(entryId: Long) {
        launchWithErrorHandling(
            errorHandler = { e -> _uiState.update { it.copy(error = ErrorMessage.Message(e.message ?: "Navigation failed")) } }
        ) {
            navigator.navigateToAddEntry(entryId)
        }
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
            navigator.navigateToKalimaDetails(entryId)
        }
    }
}
