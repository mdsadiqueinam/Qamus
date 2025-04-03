package io.github.mdsadiqueinam.qamus.extension

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import io.github.mdsadiqueinam.qamus.data.model.ErrorMessage

@Composable
fun SnackbarHostState.ShowSnackbar(errorMessage: ErrorMessage, clearError: () -> Unit = {}) {
    val message = when (errorMessage) {
        is ErrorMessage.Message -> errorMessage.message
        is ErrorMessage.Resource -> stringResource(errorMessage.resId, *errorMessage.formatArgs)
        is ErrorMessage.None -> null
    }

    LaunchedEffect(message) {
        message?.let {
            showSnackbar(it)
            clearError()
        }
    }
}