package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.ErrorMessage
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import io.github.mdsadiqueinam.qamus.extension.launchWithLoadingAndErrorHandling
import io.github.mdsadiqueinam.qamus.extension.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Kalima reminder screen.
 * Responsible for retrieving and managing Kalima data for display.
 */
@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val kalimaatRepository: KalimaatRepository
) : ViewModel() {

    // UI state for the reminder screen
    private val _kalima = MutableStateFlow<Kalima?>(null)
    val kalima: StateFlow<Kalima?> = _kalima.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<ErrorMessage>(ErrorMessage.None)
    val error: StateFlow<ErrorMessage> = _error.asStateFlow()

    init {
        loadRandomKalima()
    }

    /**
     * Load a random Kalima entry from the repository.
     */
    fun loadRandomKalima() {
        launchWithLoadingAndErrorHandling(
            loadingState = _isLoading,
            updateLoading = { state, isLoading -> isLoading },
            errorHandler = { e -> _error.value = ErrorMessage.Message("Error loading Kalima: ${e.message ?: ""}") }
        ) {
            _error.value = ErrorMessage.None

            val randomKalima = kalimaatRepository.getRandomEntry()
            if (randomKalima != null) {
                _kalima.update { randomKalima }
            } else {
                _error.value = ErrorMessage.Message("No Kalima entries found")
            }
        }
    }
}
