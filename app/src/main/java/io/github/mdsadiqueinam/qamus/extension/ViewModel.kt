package io.github.mdsadiqueinam.qamus.extension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Updates a MutableStateFlow with a new value using the copy function of a data class.
 * This reduces boilerplate when updating individual properties of a state object.
 *
 * @param update A lambda that receives the current state and returns the updated state.
 */
inline fun <T> MutableStateFlow<T>.update(crossinline update: (T) -> T) {
    value = update(value)
}

/**
 * Executes a suspending operation within a ViewModel's viewModelScope with error handling.
 * 
 * @param errorHandler A lambda that handles any exceptions thrown during execution.
 * @param block The suspending operation to execute.
 */
inline fun ViewModel.launchWithErrorHandling(
    crossinline errorHandler: (Exception) -> Unit,
    crossinline block: suspend () -> Unit
) {
    viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            errorHandler(e)
        }
    }
}

/**
 * Executes a suspending operation within a ViewModel's viewModelScope with loading state management
 * and error handling.
 *
 * @param loadingState A MutableStateFlow that will be updated to reflect loading state.
 * @param updateLoading A lambda that updates the loading state in the state object.
 * @param errorHandler A lambda that handles any exceptions thrown during execution.
 * @param block The suspending operation to execute.
 */
inline fun <T> ViewModel.launchWithLoadingAndErrorHandling(
    loadingState: MutableStateFlow<T>,
    crossinline updateLoading: (T, Boolean) -> T,
    crossinline errorHandler: (Exception) -> Unit,
    crossinline block: suspend () -> Unit
) {
    loadingState.update { updateLoading(it, true) }
    viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            errorHandler(e)
        } finally {
            loadingState.update { updateLoading(it, false) }
        }
    }
}
