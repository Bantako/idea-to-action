package org.mrlem.composesample.feature.capture

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.mrlem.composesample.data.db.NodeStatus

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
            items(state.nodes, key = { it.id }) { node ->
                val isReady = node.status == NodeStatus.READY
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onAction(CaptureAction.ShowEdit(node)) }
                        .padding(vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = node.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isReady) FontWeight.Bold else FontWeight.Normal,
                            color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        if (isReady) {
                            Text(
                                text = "READY ★",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    if (node.body.isNotBlank()) {
                        Text(
                            text = node.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                HorizontalDivider()
            }

            if (state.aiLog.isNotEmpty()) {
                item {
                    Text(
                        text = "AI がまとめた関連",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(state.aiLog, key = { "${it.newNodeTitle}-${it.relatedNodeTitle}" }) { entry ->
                    Text(
                        text = "「${entry.newNodeTitle}」→「${entry.relatedNodeTitle}」",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }

    val editTarget = state.editTarget
    if (editTarget != null) {
        NodeEditDialog(
            node = editTarget,
            onDismiss = { viewModel.onAction(CaptureAction.DismissEdit) },
            onSave = { title, body -> viewModel.onAction(CaptureAction.SaveEdit(title, body)) },
            onDelete = { viewModel.onAction(CaptureAction.DeleteNode) },
        )
    }
}

@Composable
private fun NodeEditDialog(
    node: org.mrlem.composesample.data.db.NodeEntity,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(node.title) }
    var body by remember { mutableStateOf(node.body) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ノードを編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("タイトル") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("メモ（任意）") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, body) },
                enabled = title.isNotBlank(),
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("キャンセル") }
            }
        },
    )
}
