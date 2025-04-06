package io.github.mdsadiqueinam.qamus.ui.composables.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.extension.textDirection
import io.github.mdsadiqueinam.qamus.ui.theme.QamusTheme
import kotlinx.coroutines.delay

enum class TextDirectionMode {
    AUTO, RTL, LTR
}

@Composable
fun DirectionalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    textStyle: TextStyle = LocalTextStyle.current,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    var textDirectionMode by remember { mutableStateOf(TextDirectionMode.AUTO) }
    val currentTextDirection by remember(textDirectionMode) {
        derivedStateOf {
            when (textDirectionMode) {
                TextDirectionMode.AUTO -> value.textDirection
                TextDirectionMode.RTL -> TextDirection.Rtl
                TextDirectionMode.LTR -> TextDirection.Ltr
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it)
            if (textDirectionMode == TextDirectionMode.AUTO) {
                textDirectionMode = if (it.textDirection == TextDirection.Rtl) {
                    TextDirectionMode.RTL
                } else {
                    TextDirectionMode.LTR
                }
            }
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        textStyle = textStyle.copy(textDirection = currentTextDirection),
        leadingIcon = leadingIcon,
        trailingIcon = {
            Row() {
                if (trailingIcon != null) {
                    trailingIcon()
                }
                IconButton(onClick = {
                    textDirectionMode =
                        if (value.textDirection == TextDirection.Rtl || textDirectionMode == TextDirectionMode.RTL) {
                            TextDirectionMode.LTR
                        } else {
                            TextDirectionMode.RTL
                        }
                }) {
                    Icon(
                        painter = when (textDirectionMode) {
                            TextDirectionMode.AUTO -> {
                                if (value.textDirection == TextDirection.Rtl) {
                                    painterResource(R.drawable.format_textdirection_r_to_l_24px)
                                } else {
                                    painterResource(R.drawable.format_textdirection_l_to_r_24px)
                                }
                            }

                            TextDirectionMode.RTL -> painterResource(R.drawable.format_textdirection_r_to_l_24px)
                            TextDirectionMode.LTR -> painterResource(R.drawable.format_textdirection_l_to_r_24px)
                        },
                        contentDescription = stringResource(R.string.toggle_text_direction),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = colors,
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDirectionalInputField() {
    var text by remember { mutableStateOf("Hello") }
    QamusTheme {
        DirectionalInputField(
            value = text,
            onValueChange = { text = it },
            label = {
                Text(text = "Label")
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}


/**
 * Internal composable for a debounced search input field.
 * Manages its own text state and calls back with the debounced value.
 */
@Composable
fun SearchInputField(
    modifier: Modifier = Modifier,
    initialValue: String = "",
    placeholder: @Composable (() -> Unit)? = null,
    debounceMs: Long = 350,
    onSearchChanged: (String) -> Unit
) {
    var searchText by remember { mutableStateOf(initialValue) }
    val debouncedSearchText by remember(searchText) { derivedStateOf { searchText } }

    LaunchedEffect(debouncedSearchText) {
        delay(debounceMs)
        onSearchChanged(debouncedSearchText)
    }

    DirectionalInputField(
        value = searchText,
        onValueChange = { searchText = it },
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        placeholder = placeholder ?: { Text(text = stringResource(R.string.search)) },
        singleLine = true,
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { searchText = "" }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                }
            }
        }
    )
}