package io.github.mdsadiqueinam.qamus.ui.composables.kalima

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.model.WordType
import io.github.mdsadiqueinam.qamus.extension.textDirection

@Composable
fun KalimaCard(modifier: Modifier, kalima: Kalima, onClick: () -> Unit) {
    Card(
        modifier = modifier, onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = kalima.huroof,
                    style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Rtl),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = kalima.meaning,
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = kalima.meaning.textDirection),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(WordType.getStringResourceId(kalima.type)),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    ),
                )
            }
        }
    }
}

// Preview kalima card
@Preview
@Composable
fun KalimaCardPreview() {
    KalimaCard(
        modifier = Modifier.padding(8.dp), kalima = Kalima(
            huroof = "كَلِمَةٌ",
            meaning = "لش",
            type = WordType.ISM,
            desc = "A single distinct meaningful element of speech or writing."
        ), onClick = {})
}