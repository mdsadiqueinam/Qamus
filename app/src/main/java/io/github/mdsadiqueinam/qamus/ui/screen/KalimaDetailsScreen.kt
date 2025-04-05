package io.github.mdsadiqueinam.qamus.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.model.WordType
import io.github.mdsadiqueinam.qamus.extension.ShowSnackbar
import io.github.mdsadiqueinam.qamus.ui.viewmodel.KalimaDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalimaDetailsScreen(
    viewModel: KalimaDetailsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error message in snackbar
    snackbarHostState.ShowSnackbar(errorMessage) {
        viewModel.clearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kalima Details",
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.entry?.let { entry ->
                        IconButton(
                            onClick = { viewModel.deleteEntry() }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Entry")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            uiState.entry?.let { entry ->
                FloatingActionButton(onClick = { viewModel.navigateToEditEntry(entry.id) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Entry")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.entry == null -> {
                    Text(
                        text = "Entry not found",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                else -> {
                    EntryDetails(
                        entry = uiState.entry!!,
                        rootEntry = uiState.rootEntry,
                        relatedEntries = uiState.relatedEntries,
                        onViewDetails = { entryId -> viewModel.navigateToKalimaDetails(entryId) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EntryDetails(
    entry: Kalima,
    rootEntry: Kalima?,
    relatedEntries: List<Kalima>,
    onViewDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Set layout direction to RTL for Arabic text
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
        ) {
            // Main entry details
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Arabic Word
                    Text(
                        text = entry.huroof,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Word Type
                    DetailItem(
                        label = stringResource(R.string.word_type_label),
                        value = stringResource(WordType.getStringResourceId(entry.type))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Meaning
                    DetailItem(
                        label = stringResource(R.string.meaning_label),
                        value = entry.meaning
                    )

                    // Description (if available)
                    if (entry.desc.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailItem(
                            label = stringResource(R.string.description_label),
                            value = entry.desc
                        )
                    }
                }
            }

            // Root entry details (if available)
            if (rootEntry != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { rootEntry?.id?.let { onViewDetails(it) } }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.root_word_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Root Word
                        Text(
                            text = rootEntry.huroof,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Root Word Type
                        DetailItem(
                            label = stringResource(R.string.word_type_label),
                            value = stringResource(WordType.getStringResourceId(rootEntry.type))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Root Word Meaning
                        DetailItem(
                            label = stringResource(R.string.meaning_label),
                            value = rootEntry.meaning
                        )

                        // Root Word Description (if available)
                        if (rootEntry.desc.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailItem(
                                label = stringResource(R.string.description_label),
                                value = rootEntry.desc
                            )
                        }
                    }
                }
            }

            // Related entries (if available)
            if (relatedEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "الكلمات المتعلقة (Related Words)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                relatedEntries.forEach { relatedEntry ->
                    Spacer(modifier = Modifier.height(8.dp))
                    RelatedEntryItem(
                        entry = relatedEntry,
                        onClick = { onViewDetails(relatedEntry.id) }
                    )
                }
            }

            // Entry ID (for debugging/reference)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ID: ${entry.id}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun RelatedEntryItem(
    entry: Kalima,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = entry.huroof,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = entry.meaning,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "نوع: ${WordType.toArabic(entry.type)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
