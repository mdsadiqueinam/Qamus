package io.github.mdsadiqueinam.qamus.data.model

import androidx.annotation.StringRes

/**
 * Sealed class representing different types of error messages.
 */
sealed class ErrorMessage {
    /**
     * Represents no error.
     */
    data object None : ErrorMessage()

    /**
     * Represents an error with a direct message string.
     */
    data class Message(val message: String) : ErrorMessage()

    /**
     * Represents an error with a resource ID and format arguments.
     */
    class Resource(@StringRes val resId: Int, vararg val formatArgs: Any) : ErrorMessage()
}
