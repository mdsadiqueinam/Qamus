package io.github.mdsadiqueinam.qamus.ui.composables.common

import android.icu.text.Bidi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.ui.theme.QamusTheme

enum class TextDirectionMode {
    AUTO,
    RTL,
    LTR
}

fun getBidiDirection(text: String): TextDirection {
    val bidi = Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT)
    return if (bidi.baseIsLeftToRight()) TextDirection.Ltr else TextDirection.Rtl
}

@Composable
fun DirectionalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    var textDirectionMode by remember { mutableStateOf(TextDirectionMode.AUTO) }
    val currentTextDirection by remember(textDirectionMode) {
        derivedStateOf {
            when (textDirectionMode) {
                TextDirectionMode.AUTO -> getBidiDirection(value)
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
                // Only update automatically if in AUTO mode
                textDirectionMode = if (getBidiDirection(it) == TextDirection.Rtl) {
                    TextDirectionMode.RTL
                } else {
                    TextDirectionMode.LTR
                }
            }
        },
        modifier = modifier,
        label = {
            Text(text = label ?: "")
        },
        textStyle = LocalTextStyle.current.copy(textDirection = currentTextDirection),
        trailingIcon = {
            IconButton(onClick = {
                textDirectionMode = if (getBidiDirection(value) == TextDirection.Rtl) {
                    TextDirectionMode.LTR
                } else {
                    TextDirectionMode.RTL
                }
            }) {
                Icon(
                    painter = when (textDirectionMode) {
                        TextDirectionMode.AUTO -> {
                            if (getBidiDirection(value) == TextDirection.Rtl) {
                                painterResource(R.drawable.format_textdirection_r_to_l_24px)
                            } else {
                                painterResource(R.drawable.format_textdirection_l_to_r_24px)
                            }
                        }

                        TextDirectionMode.RTL -> painterResource(R.drawable.format_textdirection_r_to_l_24px)
                        TextDirectionMode.LTR -> painterResource(R.drawable.format_textdirection_l_to_r_24px)
                    },
                    contentDescription = "Toggle Text Direction",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
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
            label = "Enter Text",
            modifier = Modifier.padding(16.dp)
        )
    }
}
