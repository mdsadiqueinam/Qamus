package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.ErrorMessage
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.model.WordType
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import io.github.mdsadiqueinam.qamus.extension.launchWithErrorHandling
import io.github.mdsadiqueinam.qamus.extension.launchWithLoadingAndErrorHandling
import io.github.mdsadiqueinam.qamus.extension.update
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusDestinations
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Data class representing the UI state for the add/edit entry screen.
 * 
 * @property id The ID of the entry (0 for new entries)
 * @property huroof The Arabic text of the entry
 * @property meaning The meaning of the entry
 * @property desc Additional description of the entry
 * @property type The word type of the entry
 * @property rootId The ID of the root entry, if this is a derived form
 * @property isLoading Whether data is currently being loaded
 * @property isEditMode Whether the screen is in edit mode
 * @property error The current error message, if any
 */
data class AddEntryUIState(
    val id: Long = 0,
    val huroof: String = "",
    val meaning: String = "",
    val desc: String = "",
    val type: WordType = WordType.ISM,
    val rootId: Long? = null,
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val error: ErrorMessage = ErrorMessage.None
)

/**
 * ViewModel for the add/edit entry screen.
 * Follows Single Responsibility Principle by focusing only on entry creation and editing.
 */
@HiltViewModel
class AddEntryViewModel @Inject constructor(
    private val repository: KalimaatRepository,
    private val savedStateHandle: SavedStateHandle,
    private val navigator: QamusNavigator,
) : ViewModel() {

    companion object {
        private const val TAG = "AddEntryViewModel"
    }

    // UI state for the form
    private val _uiState = MutableStateFlow(AddEntryUIState())
    val uiState: StateFlow<AddEntryUIState> = _uiState.asStateFlow()

    // Flow of all entries as a list for dropdowns
    val allEntriesList: Flow<List<Kalima>> = repository.getAllEntriesAsList()
        .catch { e -> 
            _uiState.update { it.copy(error = ErrorMessage.Message(e.message ?: "Error loading entries list")) }
        }
        .distinctUntilChanged()

    private val id = savedStateHandle.toRoute<QamusDestinations.AddEntry>().id

    /**
     * Load an existing entry by ID.
     * 
     * @param id The ID of the entry to load
     */
    private fun loadEntry() {
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
                _uiState.update { 
                    it.copy(
                        id = entry.id,
                        huroof = entry.huroof,
                        meaning = entry.meaning,
                        desc = entry.desc,
                        type = entry.type,
                        rootId = entry.rootId,
                        isEditMode = true,
                        error = ErrorMessage.None
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        error = ErrorMessage.Resource(R.string.entry_not_found)
                    )
                }
            }
        }
    }

    /**
     * Update UI state when form fields change.
     * 
     * @param huroof The new huroof value
     */
    fun updateHuroof(huroof: String) {
        _uiState.update { it.copy(huroof = huroof) }
    }

    /**
     * Update UI state when form fields change.
     * 
     * @param meaning The new meaning value
     */
    fun updateMeaning(meaning: String) {
        _uiState.update { it.copy(meaning = meaning) }
    }

    /**
     * Update UI state when form fields change.
     * 
     * @param desc The new description value
     */
    fun updateDesc(desc: String) {
        _uiState.update { it.copy(desc = desc) }
    }

    /**
     * Update UI state when form fields change.
     * 
     * @param type The new word type value
     */
    fun updateType(type: WordType) {
        _uiState.update { it.copy(type = type) }
    }

    /**
     * Update UI state when form fields change.
     * 
     * @param rootId The new root ID value
     */
    fun updateRootId(rootId: Long?) {
        _uiState.update { it.copy(rootId = rootId) }
    }

    /**
     * Creates a Kalima object from the current UI state.
     * 
     * @param state The current UI state
     * @return A Kalima object with the values from the UI state
     */
    private fun createKalimaFromState(state: AddEntryUIState): Kalima {
        return Kalima(
            id = state.id,
            huroof = state.huroof,
            meaning = state.meaning,
            desc = state.desc,
            type = state.type,
            rootId = state.rootId
        )
    }

    /**
     * Save the entry (add new or update existing).
     * Validates the entry before saving.
     *
     * @return true if validation passed and save operation was attempted, false otherwise
     */
    fun saveEntry(): Boolean {
        val state = _uiState.value

        if (state.huroof.isBlank() || state.meaning.isBlank()) {
            _uiState.update { it.copy(error = ErrorMessage.Resource(R.string.word_meaning_required)) }
            return false
        }

        launchWithLoadingAndErrorHandling(
            loadingState = _uiState,
            updateLoading = { state, isLoading -> state.copy(isLoading = isLoading) },
            errorHandler = { e -> 
                _uiState.update { 
                    it.copy(
                        error = ErrorMessage.Resource(R.string.error_saving_entry, e.message ?: ""),
                        isLoading = false
                    )
                }
            }
        ) {
            val entry = createKalimaFromState(state)
            if (state.isEditMode) {
                repository.updateEntry(entry)
            } else {
                repository.insertEntry(entry)
            }
            _uiState.update { it.copy(error = ErrorMessage.None) }
        }

        return true
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

    /**
     * Save the entry and navigate back if successful.
     * This ensures navigation only happens after the save operation completes.
     */
    fun saveAndNavigateBack() {
        val validationPassed = saveEntry()
        if (!validationPassed) return

        launchWithErrorHandling(
            errorHandler = { e -> _uiState.update { it.copy(error = ErrorMessage.Message(e.message ?: "Navigation failed")) } }
        ) {
            // Wait a short time to allow the save operation to complete
            delay(100)
            if (_uiState.value.error is ErrorMessage.None) {
                navigator.navigateBack()
            }
        }
    }
}
