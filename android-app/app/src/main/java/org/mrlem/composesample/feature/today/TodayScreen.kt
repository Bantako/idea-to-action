package org.mrlem.composesample.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.mrlem.composesample.data.db.NodeEntity

@Composable
fun TodayScreen(
    contentPadding: PaddingValues,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isEmpty = state.activeNodes.isEmpty() && state.readyNodes.isEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        if (isEmpty) {
            item {
                Text(
                    text = "着手できるノードがありません",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            return@LazyColumn
        }

        if (state.activeNodes.isNotEmpty()) {
            item {
                SectionHeader("進行中")
            }
            items(state.activeNodes, key = { it.id }) { node ->
                ActiveNodeRow(
                    node = node,
                    onDone = { viewModel.onAction(TodayAction.MarkDone(node)) },
                )
                HorizontalDivider()
            }
        }

        if (state.readyNodes.isNotEmpty()) {
            item {
                SectionHeader("着手できます")
            }
            items(state.readyNodes, key = { it.id }) { node ->
                ReadyNodeRow(
                    node = node,
                    onStart = { viewModel.onAction(TodayAction.MarkActive(node)) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ActiveNodeRow(node: NodeEntity, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = node.title, style = MaterialTheme.typography.bodyLarge)
        }
        Button(onClick = onDone) {
            Text("完了")
        }
    }
}

@Composable
private fun ReadyNodeRow(node: NodeEntity, onStart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = node.title, style = MaterialTheme.typography.bodyLarge)
        }
        OutlinedButton(onClick = onStart) {
            Text("着手")
        }
    }
}
