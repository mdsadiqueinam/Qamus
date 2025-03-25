package io.github.mdsadiqueinam.qamus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.util.checkAnswer

/**
 * Composable function for the Kalima reminder content.
 * Shows a full screen reminder with the Kalima content.
 */
@Composable
fun KalimaReminderContent(
    kalima: Kalima?,
    onClose: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text(
                            text = stringResource(R.string.loading),
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    error != null -> {
                        Text(
                            text = error,
                            fontSize = 20.sp,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = onClose) {
                            Text(stringResource(R.string.close), fontSize = 16.sp)
                        }
                    }

                    kalima == null -> {
                        Text(
                            text = stringResource(R.string.no_kalima_available),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.check_back_later),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = onClose) {
                            Text(stringResource(R.string.close), fontSize = 16.sp)
                        }
                    }

                    else -> {
                        KalimaContent(kalima, onClose)
                    }
                }
            }
        }
    }
}

@Composable
private fun KalimaContent(kalima: Kalima, onClose: () -> Unit) {
    var answer by remember { mutableStateOf("") }
    var isAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }

    // Display the Arabic word (huroof)
    Text(
        text = kalima.huroof,
        fontSize = 38.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (isAnswerCorrect != null) {
        Text(
            text = kalima.meaning,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    if (isAnswerCorrect !== null) {
        Text(
            text = if (isAnswerCorrect == true) stringResource(R.string.correct_answer) else stringResource(
                R.string.wrong_answer
            ),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isAnswerCorrect == true) Color.Green else Color.Red,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text(stringResource(R.string.meaning)) },
                placeholder = { Text(stringResource(R.string.meaning)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAnswerCorrect != null) Arrangement.Center else Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(stringResource(R.string.close), fontSize = 16.sp)
        }

        if (isAnswerCorrect == null) {
            Button(
                onClick = {
                    isAnswerCorrect = checkAnswer(answer, kalima.meaning)
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(stringResource(R.string.submit), fontSize = 16.sp)
            }
        }
    }
}
