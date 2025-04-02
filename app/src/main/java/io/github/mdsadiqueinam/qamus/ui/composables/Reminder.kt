package io.github.mdsadiqueinam.qamus.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.util.formatTime
import kotlin.math.roundToInt

@Composable
fun ReminderSetting(
    currentInterval: Int,
    onIntervalChanged: (Int) -> Unit,
    isEnabledReminder: Boolean,
    onReminderStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val valueRange = 10f..180f
    val step = 10
    val steps = ((valueRange.endInclusive - valueRange.start) / step).toInt()

    var sliderPosition by remember(currentInterval) {
        mutableFloatStateOf(currentInterval.toFloat())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.remind_every, formatTime(sliderPosition)),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        ReminderSlider(
            sliderPosition = sliderPosition,
            onSliderValueChange = {
                val snappedValue = ((it / step).roundToInt() * step).toFloat()
                sliderPosition = snappedValue.coerceIn(valueRange)
            },
            onValueChangeFinished = { onIntervalChanged(sliderPosition.toInt()) },
            valueRange = valueRange,
            steps = steps - 1
        )

        Spacer(modifier = Modifier.height(16.dp))

        ReminderToggle(
            isEnabled = isEnabledReminder,
            onToggle = onReminderStateChanged
        )
    }
}

@Composable
fun ReminderSlider(
    sliderPosition: Float,
    onSliderValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition,
            onValueChange = onSliderValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ten_minutes),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.three_hours),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ReminderToggle(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.enable_reminder),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}