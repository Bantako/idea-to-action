package org.mrlem.composesample.feature.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import org.mrlem.composesample.data.db.EdgeEntity
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.mrlem.composesample.data.db.NodeStatus

private val STATUS_FILTERS: List<NodeStatus?> = listOf(null) + NodeStatus.entries

private fun NodeStatus?.label(): String = when (this) {
    null -> "全て"
    NodeStatus.IDEA -> "IDEA"
    NodeStatus.READY -> "READY"
    NodeStatus.ACTIVE -> "ACTIVE"
    NodeStatus.DONE -> "DONE"
    NodeStatus.ABANDONED -> "ABANDONED"
}

@Composable
fun GraphScreen(
    contentPadding: PaddingValues,
    viewModel: GraphViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(STATUS_FILTERS) { filter ->
                FilterChip(
                    selected = state.statusFilter == filter,
                    onClick = { viewModel.onAction(GraphAction.FilterChanged(filter)) },
                    label = { Text(filter.label()) },
                )
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(state.displayItems, key = { it.node.id }) { item ->
                NodeItemRow(
                    item = item,
                    onAddPrereq = { viewModel.onAction(GraphAction.ShowAddEdge(item.node)) },
                    onRemovePrereq = { edge -> viewModel.onAction(GraphAction.RemoveEdge(edge)) },
                )
                HorizontalDivider()
            }
        }
    }

    if (state.addEdgeTarget != null) {
        val target = state.addEdgeTarget!!
        val candidates = state.allNodes.filter { it.id != target.id }
        AlertDialog(
            onDismissRequest = { viewModel.onAction(GraphAction.DismissAddEdge) },
            title = { Text("「${target.title}」の前提ノードを選択") },
            text = {
                LazyColumn {
                    items(candidates, key = { it.id }) { node ->
                        TextButton(
                            onClick = { viewModel.onAction(GraphAction.AddEdge(node.id)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(node.title)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(GraphAction.DismissAddEdge) }) {
                    Text("キャンセル")
                }
            },
        )
    }
}

@Composable
private fun NodeItemRow(
    item: NodeItem,
    onAddPrereq: () -> Unit,
    onRemovePrereq: (EdgeEntity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.node.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = item.node.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        if (item.prereqs.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                item.prereqs.forEach { prereq ->
                    SuggestionChip(
                        onClick = { onRemovePrereq(prereq.edge) },
                        label = { Text("× ${prereq.fromNode.title}") },
                    )
                }
            }
        }

        TextButton(
            onClick = onAddPrereq,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = "+ 前提を追加",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
