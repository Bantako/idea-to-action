package org.mrlem.composesample.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun CaptureScreen(
    contentPadding: PaddingValues,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = { viewModel.onAction(CaptureAction.InputChanged(it)) },
                placeholder = { Text("アイデアを入力…") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.onAction(CaptureAction.Submit) },
                enabled = state.input.isNotBlank(),
            ) {
                Text("追加")
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (state.isLoadingAi) {
                item {
                    Text(
                        text = "AI が関連ノードを確認中…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            if (state.suggestions.isNotEmpty()) {
                items(state.suggestions, key = { "${it.newNodeId}-${it.relatedNode.id}" }) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onAccept = { viewModel.onAction(CaptureAction.AcceptSuggestion(suggestion)) },
                        onDismiss = { viewModel.onAction(CaptureAction.DismissSuggestion(suggestion)) },
                    )
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
            }

            items(state.nodes, key = { it.id }) { node ->
                Text(
                    text = node.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: NodeSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "「${suggestion.relatedNode.title}」との関連が考えられます",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("スキップ")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onAccept) {
                    Text("エッジを追加")
                }
            }
        }
    }
}
