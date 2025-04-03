package io.github.mdsadiqueinam.qamus.data.model

import androidx.annotation.StringRes

sealed class ErrorMessage {
    data class Message(val message: String) : ErrorMessage()
    class Resource(@StringRes val resId: Int, vararg val formatArgs: Any) : ErrorMessage()
    data object None : ErrorMessage()
}