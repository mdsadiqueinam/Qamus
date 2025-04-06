package io.github.mdsadiqueinam.qamus.ui.composables.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import io.github.mdsadiqueinam.qamus.ui.theme.QamusTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Internal composable for standard dialog action buttons (Dismiss, Ok).
 */
@Composable
internal fun DialogActionButtons(
    modifier: Modifier = Modifier,
    showOkButton: Boolean,
    onDismiss: () -> Unit,
    onOk: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onDismiss) {
            Text("Dismiss")
        }
        if (showOkButton) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onOk) {
                Text("Ok")
            }
        }
    }
}


/**
 * Internal composable for a standard checkbox with associated spacing.
 */
@Composable
internal fun PickerItem(
    selected: Boolean, onClick: () -> Unit, itemContent: @Composable () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            onClick()
        }
        .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            itemContent()
        }
    }
}

/**
 * Reusable dialog content structure for both static and paging items.
 */
@Composable
internal fun PickerDialogContent(
    showOkButton: Boolean,
    onDismiss: () -> Unit,
    onOk: () -> Unit = {},
    searchSection: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        // Optional search section
        searchSection?.invoke()

        // Main content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            content()
        }

        // Action buttons
        DialogActionButtons(
            showOkButton = showOkButton, onDismiss = onDismiss, onOk = onOk
        )
    }
}

// --- Base Picker Composable ---

/**
 * Base composable for creating picker dialogs.
 */
@Composable
fun BaseItemPicker(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
    selectedItemContent: @Composable () -> Unit,
    content: @Composable (dismissDialog: () -> Unit) -> Unit,
) {
    var showDialog by remember { mutableStateOf(true) }

    Column(modifier = modifier
        .fillMaxWidth()
        .clickable(enabled = enabled) { showDialog = true }
        .padding(vertical = 8.dp, horizontal = 12.dp)) {
        Text(
            text = label, style = MaterialTheme.typography.titleMedium, maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            selectedItemContent()
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                content { showDialog = false }
            }
        }
    }
}

// --- Public Picker Components ---

/**
 * A simple item picker where the dialog content is a LazyColumn defined by the caller.
 */
@Composable
fun <T> ItemPicker(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
    items: List<T>,
    selectedItem: T,
    itemKey: (T) -> Any = { it.hashCode() },
    onItemSelected: (T) -> Unit,
    selectedItemContent: @Composable () -> Unit,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
) {
    BaseItemPicker(
        modifier = modifier,
        label = label,
        enabled = enabled,
        selectedItemContent = selectedItemContent,
        content = { dismissDialog ->
            PickerDialogContent(
                showOkButton = true,
                onDismiss = dismissDialog,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items.size, key = { itemKey(items[it]) }) { index ->
                        val item = items[index]
                        val isSelected = selectedItem == item

                        PickerItem(
                            selected = isSelected, onClick = {
                                onItemSelected(item)
                                dismissDialog()
                            }) {
                            itemContent(item, isSelected)
                        }
                    }
                }
            }
        })
}

/**
 * An item picker for selecting multiple items from a static list.
 */
@Composable
fun <T : Any> MultipleItemPicker(
    modifier: Modifier = Modifier,
    label: String,
    items: List<T>,
    selectedItems: Set<T>,
    enabled: Boolean = true,
    onItemsSelected: (Set<T>) -> Unit,
    itemKey: (T) -> Any = { it.hashCode() },
    selectedItemsContent: @Composable () -> Unit,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
) {
    var tempSelectedItems by remember(selectedItems) { mutableStateOf(selectedItems) }

    BaseItemPicker(
        modifier = modifier,
        label = label,
        enabled = enabled,
        selectedItemContent = selectedItemsContent,
        content = { dismissDialog ->
            PickerDialogContent(
                showOkButton = true,
                onDismiss = dismissDialog,
                onOk = {
                    onItemsSelected(tempSelectedItems)
                    dismissDialog()
                },
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items.size, key = { itemKey(items[it]) }) { index ->
                        val item = items[index]
                        val isSelected = tempSelectedItems.contains(item)

                        PickerItem(
                            selected = isSelected, onClick = {
                                tempSelectedItems = if (isSelected) {
                                    tempSelectedItems - item
                                } else {
                                    tempSelectedItems + item
                                }
                            }) {
                            itemContent(item, isSelected)
                        }
                    }
                }
            }
        })
}

/**
 * An item picker for selecting a single item from a paginated list.
 */
@Composable
fun <T : Any> ItemPicker(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
    lazyPagingItems: LazyPagingItems<T>,
    onItemSelected: (T) -> Unit,
    selectedItem: T,
    itemKey: (T) -> Any = { it.hashCode() },
    search: (String) -> Unit,
    searchDebounceMs: Long = 350,
    searchPlaceholder: @Composable () -> Unit,
    selectedItemContent: @Composable () -> Unit,
    itemContent: @Composable (T) -> Unit,
) {
    BaseItemPicker(
        modifier = modifier,
        label = label,
        enabled = enabled,
        selectedItemContent = selectedItemContent,
        content = { dismissDialog ->
            PickerDialogContent(
                showOkButton = false,
                onDismiss = dismissDialog,
                searchSection = {
                    SearchInputField(
                        onSearchChanged = search,
                        debounceMs = searchDebounceMs,
                        placeholder = searchPlaceholder
                    )
                },
            ) {
                // Handle fullscreen loading/error states
                PagingStateHandlingFullscreen(
                    loadState = lazyPagingItems.loadState,
                    itemCount = lazyPagingItems.itemCount,
                    onRetry = { lazyPagingItems.retry() })

                // Determine if the list should be shown
                val showList =
                    (lazyPagingItems.loadState.refresh !is LoadState.Loading && lazyPagingItems.loadState.refresh !is LoadState.Error) || lazyPagingItems.itemCount > 0

                if (showList) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = lazyPagingItems.itemKey(itemKey)
                        ) { index ->
                            val item = lazyPagingItems[index]
                            val isSelected = selectedItem == item

                            if (item != null) {
                                PickerItem(
                                    selected = isSelected, onClick = {
                                        onItemSelected(item)
                                        dismissDialog()
                                    }) {
                                    itemContent(item)
                                }
                            }
                        }

                        pagingStateHandlingFooter(
                            loadState = lazyPagingItems.loadState,
                            onRetry = { lazyPagingItems.retry() })
                    }
                }
            }
        })
}

/**
 * An item picker for selecting multiple items from a paginated list.
 */
@Composable
fun <T : Any> MultipleItemPicker(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
    lazyPagingItems: LazyPagingItems<T>,
    selectedItems: Set<T>,
    onItemsSelected: (Set<T>) -> Unit,
    itemKey: (T) -> Any = { it.hashCode() },
    search: (String) -> Unit,
    searchDebounceMs: Long = 350,
    searchPlaceholder: @Composable () -> Unit,
    selectedItemsContent: @Composable () -> Unit,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
) {
    var tempSelectedItems by remember(selectedItems) { mutableStateOf(selectedItems) }

    BaseItemPicker(
        modifier = modifier,
        label = label,
        enabled = enabled,
        selectedItemContent = selectedItemsContent,
        content = { dismissDialog ->
            PickerDialogContent(
                showOkButton = true,
                onDismiss = dismissDialog,
                onOk = {
                    onItemsSelected(tempSelectedItems)
                    dismissDialog()
                },
                searchSection = {
                    SearchInputField(
                        onSearchChanged = search,
                        debounceMs = searchDebounceMs,
                        placeholder = searchPlaceholder
                    )
                },
            ) {
                // Handle fullscreen loading/error states
                PagingStateHandlingFullscreen(
                    loadState = lazyPagingItems.loadState,
                    itemCount = lazyPagingItems.itemCount,
                    onRetry = { lazyPagingItems.retry() })

                // Determine if the list should be shown
                val showList =
                    (lazyPagingItems.loadState.refresh !is LoadState.Loading && lazyPagingItems.loadState.refresh !is LoadState.Error) || lazyPagingItems.itemCount > 0

                if (showList) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = lazyPagingItems.itemKey(itemKey)
                        ) { index ->
                            val item = lazyPagingItems[index]
                            if (item != null) {
                                val isSelected = tempSelectedItems.contains(item)

                                PickerItem(
                                    selected = isSelected, onClick = {
                                        tempSelectedItems = if (isSelected) {
                                            tempSelectedItems - item
                                        } else {
                                            tempSelectedItems + item
                                        }
                                    }) {
                                    itemContent(item, isSelected)
                                }
                            }
                        }

                        pagingStateHandlingFooter(
                            loadState = lazyPagingItems.loadState,
                            onRetry = { lazyPagingItems.retry() })
                    }
                }
            }
        })
}

// Preview list item picker
@Preview(showBackground = true)
@Composable
fun ListItemPickerPreview() {
    QamusTheme {
        Column(
            modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center
        ) {
            ItemPicker(
                label = "Select Item",
                items = (1..5).toList().map { "Item $it" },
                selectedItem = "Item 1",
                onItemSelected = {},
                selectedItemContent = { Text("Selected Item") },
            ) { item, isSelected ->
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Preview Multiple Selected list item
@Preview(showBackground = true)
@Composable
fun MultipleItemPickerPreview() {
    QamusTheme {
        Column(
            modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center
        ) {
            MultipleItemPicker(
                label = "Select Items",
                items = (1..5).toList().map { "Item $it" },
                selectedItems = setOf("Item 1", "Item 3"),
                onItemsSelected = {},
                selectedItemsContent = { Text("Selected Items") },
            ) { item, isSelected ->
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Preview Paging Item Picker
@Preview(showBackground = true)
@Composable
fun PagingItemPickerPreview() {
    QamusTheme {
        Column(
            modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center
        ) {
            val previewItems = List(500) { "Preview Item ${it + 1}" }
            val previewData = PagingData.from(
                previewItems, LoadStates(
                    refresh = LoadState.NotLoading(false),
                    append = LoadState.NotLoading(false),
                    prepend = LoadState.NotLoading(false),
                )
            )
            val lazyPagingItems: LazyPagingItems<String> =
                MutableStateFlow(previewData).collectAsLazyPagingItems()

            ItemPicker(
                label = "Select Item",
                lazyPagingItems = lazyPagingItems,
                selectedItemContent = { Text("Selected Item") },
                selectedItem = "Preview Item 2",
                onItemSelected = {},
                itemContent = { item ->
                    Text(
                        text = item, style = MaterialTheme.typography.bodyMedium
                    )
                },
                search = {},
                searchPlaceholder = { Text("Search...") })
        }
    }
}

// Preview Paging Multiple Item Picker
@Preview(showBackground = true)
@Composable
fun PagingMultipleItemPickerPreview() {
    QamusTheme {
        Column(
            modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center
        ) {
            val previewItems = List(500) { "Preview Item ${it + 1}" }
            val previewData = PagingData.from(
                previewItems, LoadStates(
                    refresh = LoadState.NotLoading(false),
                    append = LoadState.NotLoading(false),
                    prepend = LoadState.NotLoading(false),
                )
            )
            val lazyPagingItems: LazyPagingItems<String> =
                MutableStateFlow(previewData).collectAsLazyPagingItems()

            MultipleItemPicker(
                label = "Select Items",
                lazyPagingItems = lazyPagingItems,
                selectedItems = setOf("Preview Item 2", "Preview Item 4"),
                onItemsSelected = {},
                selectedItemsContent = { Text("Selected Items") },
                itemContent = { item, isSelected ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                search = {},
                searchPlaceholder = { Text("Search...") })
        }
    }
}