package io.github.mdsadiqueinam.qamus.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.model.WordType
import io.github.mdsadiqueinam.qamus.extension.ShowSnackbar
import io.github.mdsadiqueinam.qamus.ui.composables.common.DirectionalInputField
import io.github.mdsadiqueinam.qamus.ui.viewmodel.AddEntryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    viewModel: AddEntryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val entries by viewModel.allEntriesList.collectAsState(initial = emptyList())

    // Find the selected root entry from the entries list
    val selectedRootEntry = remember(entries, uiState.rootId) {
        uiState.rootId?.let { rootId ->
            entries.find { it.id == rootId }
        }
    }

    // Show error message in snackbar
    snackbarHostState.ShowSnackbar(uiState.error)

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(
                text = if (uiState.isEditMode) stringResource(R.string.edit_entry) else stringResource(R.string.add_new_entry),
                modifier = Modifier.fillMaxWidth()
            )
        }, navigationIcon = {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        // Set layout direction to RTL for Arabic text
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Show loading indicator if loading
            if (uiState.isLoading) {
                Text(
                    text = stringResource(R.string.loading),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                // Arabic Word
                DirectionalInputField(
                    value = uiState.huroof,
                    onValueChange = { viewModel.updateHuroof(it) },
                    label = { Text(stringResource(R.string.arabic_word)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Meaning
                DirectionalInputField(
                    value = uiState.meaning,
                    onValueChange = { viewModel.updateMeaning(it) },
                    label = { Text(stringResource(R.string.meaning)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                DirectionalInputField(
                    value = uiState.desc,
                    onValueChange = { viewModel.updateDesc(it) },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Word Type
                WordTypeDropdown(
                    selectedType = uiState.type,
                    onTypeSelected = { viewModel.updateType(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Root ID Dropdown
                RootIdDropdown(
                    entries = entries,
                    selectedEntry = selectedRootEntry,
                    onEntrySelected = { entry -> viewModel.updateRootId(entry?.id) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = { viewModel.saveAndNavigateBack() }, modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (uiState.isEditMode) stringResource(R.string.update_entry) else stringResource(R.string.save_entry),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootIdDropdown(
    entries: List<Kalima>, selectedEntry: Kalima?, onEntrySelected: (Kalima?) -> Unit, modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier
    ) {
        DirectionalInputField(
            value = selectedEntry?.huroof ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.root_word)) },
            placeholder = { Text(stringResource(R.string.select_root_word)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false }) {
            // Option to clear selection
            DropdownMenuItem(text = { Text(stringResource(R.string.none)) }, onClick = {
                onEntrySelected(null)
                expanded = false
            })

            // List all entries
            entries.forEach { entry ->
                DropdownMenuItem(text = {
                    Text(stringResource(R.string.entry_with_meaning, entry.huroof, entry.meaning))
                }, onClick = {
                    onEntrySelected(entry)
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordTypeDropdown(
    selectedType: WordType, onTypeSelected: (WordType) -> Unit, modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier
    ) {
        DirectionalInputField(
            value = stringResource(WordType.getStringResourceId(selectedType)),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.word_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false }) {
            WordType.entries.forEach { type ->
                DropdownMenuItem(text = { Text(stringResource(WordType.getStringResourceId(type))) }, onClick = {
                    onTypeSelected(type)
                    expanded = false
                })
            }
        }
    }
}
