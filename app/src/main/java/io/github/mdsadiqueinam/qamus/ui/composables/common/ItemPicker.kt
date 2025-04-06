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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey

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
 * Internal composable to display appropriate UI based on Paging's loading states.
 */
@Composable
internal fun PagingStateHandlingFullscreen(
    modifier: Modifier = Modifier,
    loadState: CombinedLoadStates,
    itemCount: Int,
    onRetry: () -> Unit
) {
    val refreshState = loadState.refresh

    if (refreshState is LoadState.Loading && itemCount == 0) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (refreshState is LoadState.Error && itemCount == 0) {
        val error = refreshState.error
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error loading data: ${error.localizedMessage ?: "Unknown error"}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Internal extension for LazyListScope to add footer items for pagination.
 */
internal fun LazyListScope.pagingStateHandlingFooter(
    loadState: CombinedLoadStates, onRetry: () -> Unit
) {
    val appendState = loadState.append

    if (appendState is LoadState.Loading) {
        item("paging_append_loading") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.height(24.dp))
            }
        }
    }

    if (appendState is LoadState.Error) {
        val error = appendState.error
        item("paging_append_error") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Error loading more: ${error.localizedMessage ?: "Unknown error"}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

/**
 * Internal composable for a standard checkbox with associated spacing.
 */
@Composable
internal fun PickerCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    Spacer(modifier = Modifier.width(16.dp))
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
    Column(Modifier.fillMaxSize()) {
        // Optional search section
        searchSection?.invoke()

        // Main content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier
        .fillMaxWidth()
        .clickable(enabled = enabled) { showDialog = true }
        .padding(vertical = 8.dp, horizontal = 4.dp)) {
        Text(
            text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.padding(start = 4.dp)) {
            selectedItemContent()
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
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

                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onItemSelected(item)
                                dismissDialog()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            PickerCheckbox(
                                checked = isSelected, onCheckedChange = {
                                    onItemSelected(item)
                                    dismissDialog()
                                })
                            Box(modifier = Modifier.weight(1f)) {
                                itemContent(item, isSelected)
                            }
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

                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempSelectedItems = if (isSelected) {
                                    tempSelectedItems - item
                                } else {
                                    tempSelectedItems + item
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            PickerCheckbox(
                                checked = isSelected, onCheckedChange = { checked ->
                                    tempSelectedItems = if (checked) {
                                        tempSelectedItems + item
                                    } else {
                                        tempSelectedItems - item
                                    }
                                })
                            Box(modifier = Modifier.weight(1f)) {
                                itemContent(item, isSelected)
                            }
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
    selectedItemContent: @Composable () -> Unit,
    onItemSelected: (T) -> Unit,
    itemContent: @Composable (T) -> Unit,
    itemKey: (T) -> Any = { it.hashCode() },
    search: (String) -> Unit,
    searchDebounceMs: Long = 350,
    searchPlaceholder: @Composable () -> Unit
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = lazyPagingItems.itemKey(itemKey)
                        ) { index ->
                            val item = lazyPagingItems[index]
                            if (item != null) {
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onItemSelected(item)
                                        dismissDialog()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
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
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
    itemKey: (T) -> Any = { it.hashCode() },
    search: (String) -> Unit,
    searchDebounceMs: Long = 350,
    searchPlaceholder: @Composable () -> Unit,
    selectedItemsContent: @Composable () -> Unit,
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

                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempSelectedItems = if (isSelected) {
                                            tempSelectedItems - item
                                        } else {
                                            tempSelectedItems + item
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    PickerCheckbox(
                                        checked = isSelected, onCheckedChange = { checked ->
                                            tempSelectedItems = if (checked) {
                                                tempSelectedItems + item
                                            } else {
                                                tempSelectedItems - item
                                            }
                                        })
                                    Box(modifier = Modifier.weight(1f)) {
                                        itemContent(item, isSelected)
                                    }
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