package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.Kalimaat
import io.github.mdsadiqueinam.qamus.data.model.WordType
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
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
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // UI state for the form
    private val _uiState = MutableStateFlow(AddEntryUIState())
    val uiState: StateFlow<AddEntryUIState> = _uiState.asStateFlow()

    // Flow of all entries as a list for dropdowns
    val allEntriesList: Flow<List<Kalimaat>> = repository.getAllEntriesAsList()

    // State for error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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

        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
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
     * Update UI state when form fields change.
     */
    fun updateHuroof(huroof: String) {
        _uiState.value = _uiState.value.copy(huroof = huroof)
    }

    fun updateMeaning(meaning: String) {
        _uiState.value = _uiState.value.copy(meaning = meaning)
    }

    fun updateDesc(desc: String) {
        _uiState.value = _uiState.value.copy(desc = desc)
    }

    fun updateType(type: WordType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun updateRootId(rootId: Long?) {
        _uiState.value = _uiState.value.copy(rootId = rootId)
    }

    /**
     * Save the entry (add new or update existing).
     */
    fun saveEntry() {
        val state = _uiState.value

        if (state.huroof.isBlank() || state.meaning.isBlank()) {
            _errorMessage.value = "Word and meaning cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                if (state.isEditMode) {
                    // Update existing entry
                    val entry = Kalimaat(
                        id = state.id,
                        huroof = state.huroof,
                        meaning = state.meaning,
                        desc = state.desc,
                        type = state.type,
                        rootId = state.rootId
                    )
                    repository.updateEntry(entry)
                } else {
                    // Add new entry
                    val entry = Kalimaat(
                        huroof = state.huroof,
                        meaning = state.meaning,
                        desc = state.desc,
                        type = state.type,
                        rootId = state.rootId
                    )
                    repository.insertEntry(entry)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving entry: ${e.message}"
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
