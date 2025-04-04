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
    data class Resource(@StringRes val resId: Int, val formatArgs: Array<out Any> = emptyArray()) : ErrorMessage() {
        constructor(resId: Int, vararg args: Any) : this(resId, args)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Resource

            if (resId != other.resId) return false
            if (!formatArgs.contentEquals(other.formatArgs)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + formatArgs.contentHashCode()
            return result
        }
    }
}
