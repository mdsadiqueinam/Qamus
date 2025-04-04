package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.ErrorMessage
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.model.WordType
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import io.github.mdsadiqueinam.qamus.extension.launchWithErrorHandling
import io.github.mdsadiqueinam.qamus.extension.launchWithLoadingAndErrorHandling
import io.github.mdsadiqueinam.qamus.extension.update
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing the UI state for the add/edit entry screen.
 */
data class AddEntryUIState(
    val id: Long = 0,
    val huroof: String = "",
    val meaning: String = "",
    val desc: String = "",
    val type: WordType = WordType.ISM,
    val rootId: Long? = null,
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false
)

/**
 * ViewModel for the add/edit entry screen.
 */
@HiltViewModel
class AddEntryViewModel @Inject constructor(
    private val repository: KalimaatRepository,
    private val savedStateHandle: SavedStateHandle,
    private val navigator: QamusNavigator,
) : ViewModel() {

    // UI state for the form
    private val _uiState = MutableStateFlow(AddEntryUIState())
    val uiState: StateFlow<AddEntryUIState> = _uiState.asStateFlow()

    // Flow of all entries as a list for dropdowns
    val allEntriesList: Flow<List<Kalima>> = repository.getAllEntriesAsList()

    // State for error messages
    private val _errorMessage = MutableStateFlow<ErrorMessage>(ErrorMessage.None)
    val errorMessage: StateFlow<ErrorMessage> = _errorMessage.asStateFlow()

    init {
        // Get entryId from SavedStateHandle
        val entryId = savedStateHandle.get<Long>("entryId") ?: -1L

        // Load entry if ID is provided
        if (entryId > 0) {
            loadEntry(entryId)
        }
    }

    /**
     * Load an existing entry by ID.
     */
    private fun loadEntry(id: Long) {
        if (id <= 0) return

        launchWithLoadingAndErrorHandling(
            loadingState = _uiState,
            updateLoading = { state, isLoading -> state.copy(isLoading = isLoading) },
            errorHandler = { e -> _errorMessage.value = ErrorMessage.Resource(R.string.error_loading_entry, e.message ?: "") }
        ) {
            val entry = repository.getEntryById(id)
            if (entry != null) {
                _uiState.value = AddEntryUIState(
                    id = entry.id,
                    huroof = entry.huroof,
                    meaning = entry.meaning,
                    desc = entry.desc,
                    type = entry.type,
                    rootId = entry.rootId,
                    isLoading = false,
                    isEditMode = true
                )
            } else {
                _errorMessage.value = ErrorMessage.Resource(R.string.entry_not_found)
            }
        }
    }

    /**
     * Update UI state when form fields change.
     */
    fun updateHuroof(huroof: String) {
        _uiState.update { it.copy(huroof = huroof) }
    }

    fun updateMeaning(meaning: String) {
        _uiState.update { it.copy(meaning = meaning) }
    }

    fun updateDesc(desc: String) {
        _uiState.update { it.copy(desc = desc) }
    }

    fun updateType(type: WordType) {
        _uiState.update { it.copy(type = type) }
    }

    fun updateRootId(rootId: Long?) {
        _uiState.update { it.copy(rootId = rootId) }
    }

    /**
     * Creates a Kalima object from the current UI state.
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
     *
     * @return true if validation passed and save operation was attempted, false otherwise
     */
    fun saveEntry(): Boolean {
        val state = _uiState.value

        if (state.huroof.isBlank() || state.meaning.isBlank()) {
            _errorMessage.value = ErrorMessage.Resource(R.string.word_meaning_required)
            return false
        }

        launchWithErrorHandling(
            errorHandler = { e -> _errorMessage.value = ErrorMessage.Resource(R.string.error_saving_entry, e.message ?: "") }
        ) {
            val entry = createKalimaFromState(state)
            if (state.isEditMode) {
                repository.updateEntry(entry)
            } else {
                repository.insertEntry(entry)
            }
        }

        return true
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = ErrorMessage.None
    }

    /**
     * Navigate back to the previous screen
     */
    fun navigateBack() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    /**
     * Save the entry and navigate back if successful.
     * This ensures navigation only happens after the save operation completes.
     */
    fun saveEntryAndNavigateBack() {
        val validationPassed = saveEntry()
        if (!validationPassed) return

        // Use a separate coroutine to wait for the save operation to complete
        // before navigating back
        viewModelScope.launch {
            // Wait a short time to allow the save operation to complete
            kotlinx.coroutines.delay(100)
            if (_errorMessage.value is ErrorMessage.None) {
                navigator.navigateBack()
            }
        }
    }
}
