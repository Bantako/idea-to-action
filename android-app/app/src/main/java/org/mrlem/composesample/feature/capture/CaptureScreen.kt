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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.mrlem.composesample.data.db.MemoEntity
import org.mrlem.composesample.data.db.ProjectEntity

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

        if (state.memos.isEmpty()) {
            Text(
                text = "未整理のメモはありません",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(state.memos, key = { it.id }) { memo ->
                    MemoItem(
                        memo = memo,
                        onClick = { viewModel.onAction(CaptureAction.ShowLinkSheet(memo)) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val linkTarget = state.linkTarget
    if (linkTarget != null) {
        LinkToProjectDialog(
            memo = linkTarget,
            projects = state.projects,
            aiSuggestedProjectIds = state.aiSuggestedProjectIds,
            onDismiss = { viewModel.onAction(CaptureAction.DismissLinkSheet) },
            onLinkToProject = { projectId -> viewModel.onAction(CaptureAction.LinkToProject(projectId)) },
            onDelete = { viewModel.onAction(CaptureAction.DeleteMemo(linkTarget)) },
        )
    }
}

@Composable
private fun MemoItem(
    memo: MemoEntity,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = memo.text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun LinkToProjectDialog(
    memo: MemoEntity,
    projects: List<ProjectEntity>,
    aiSuggestedProjectIds: List<Long>,
    onDismiss: () -> Unit,
    onLinkToProject: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("プロジェクトに紐付ける") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "「${memo.text}」",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                if (projects.isEmpty()) {
                    Text(
                        text = "プロジェクトがまだありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    projects.forEach { project ->
                        val isSuggested = project.id in aiSuggestedProjectIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLinkToProject(project.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSuggested) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSuggested) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSuggested) {
                                Text(
                                    text = "AI提案",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("削除", color = MaterialTheme.colorScheme.error)
            }
        },
    )
}
