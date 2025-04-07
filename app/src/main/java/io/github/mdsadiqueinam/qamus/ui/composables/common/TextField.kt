package io.github.mdsadiqueinam.qamus.ui.composables.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
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

enum class TextDirectionMode {
    AUTO, RTL, LTR
}

@Composable
fun DirectionalInputFieldWrapper(
    text: String, content: @Composable (textDirection: TextDirection, trailingIcon: @Composable () -> Unit) -> Unit
) {
    var textDirectionMode by remember { mutableStateOf(TextDirectionMode.AUTO) }
    val currentTextDirection by remember(textDirectionMode, text) {
        derivedStateOf {
            when (textDirectionMode) {
                TextDirectionMode.AUTO -> text.textDirection
                TextDirectionMode.RTL -> TextDirection.Rtl
                TextDirectionMode.LTR -> TextDirection.Ltr
            }
        }
    }

    content(
        currentTextDirection
    ) {
        IconButton(onClick = {
            textDirectionMode =
                if (text.textDirection == TextDirection.Rtl || textDirectionMode == TextDirectionMode.RTL) {
                    TextDirectionMode.LTR
                } else {
                    TextDirectionMode.RTL
                }
        }) {
            Icon(
                painter = when (textDirectionMode) {
                    TextDirectionMode.AUTO -> {
                        if (currentTextDirection == TextDirection.Rtl) {
                            painterResource(R.drawable.format_textdirection_r_to_l_24px)
                        } else {
                            painterResource(R.drawable.format_textdirection_l_to_r_24px)
                        }
                    }

                    TextDirectionMode.RTL -> painterResource(R.drawable.format_textdirection_r_to_l_24px)
                    TextDirectionMode.LTR -> painterResource(R.drawable.format_textdirection_l_to_r_24px)
                }, contentDescription = stringResource(R.string.toggle_text_direction), modifier = Modifier.size(24.dp)
            )
        }
    }
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
    readOnly: Boolean = false,
) {

    DirectionalInputFieldWrapper(value) { textDirection, wrapperTrailingIcon ->
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
            },
            modifier = modifier,
            label = label,
            placeholder = placeholder,
            textStyle = textStyle.copy(textDirection = textDirection),
            leadingIcon = leadingIcon,
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (trailingIcon != null) {
                        trailingIcon()
                    }
                    wrapperTrailingIcon()
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
            readOnly = readOnly,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDirectionalInputField() {
    var text by remember { mutableStateOf("Hello صادق") }
    QamusTheme {
        DirectionalInputField(
            value = text, onValueChange = { text = it }, label = {
            Text(text = "Label")
        }, modifier = Modifier.padding(16.dp)
        )
    }
}
