package io.github.mdsadiqueinam.qamus.ui.composables.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState


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