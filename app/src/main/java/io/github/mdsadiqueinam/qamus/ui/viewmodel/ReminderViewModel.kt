package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadRandomKalima()
    }

    /**
     * Load a random Kalima entry from the repository.
     */
    fun loadRandomKalima() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val randomKalima = kalimaatRepository.getRandomEntry()
                if (randomKalima != null) {
                    _kalima.value = randomKalima
                } else {
                    _error.value = "No Kalima entries found"
                }
            } catch (e: Exception) {
                _error.value = "Error loading Kalima: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}