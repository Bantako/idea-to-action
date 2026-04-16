package org.mrlem.composesample.feature.projects

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.mrlem.composesample.data.db.MemoEntity
import org.mrlem.composesample.data.db.StepEntity
import org.mrlem.composesample.data.db.StepStatus

@Composable
fun ProjectDetailScreen(
    contentPadding: PaddingValues,
    projectId: Long,
    onBack: () -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(projectId) {
        viewModel.initProject(projectId)
    }

    val state by viewModel.state.collectAsState()
    val project = state.project

    if (project == null) {
        // プロジェクトが見つからない（アーカイブ後など）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
        ) {
            TextButton(onClick = onBack) { Text("← 戻る") }
            Text(
                text = "プロジェクトが見つかりません",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        return
    }

    val isFocused = project.focusedAt != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.padding(bottom = 4.dp)) {
                Text("← 戻る")
            }

            Text(
                text = project.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 背景メモ表示・編集
            if (state.editingBackground) {
                OutlinedTextField(
                    value = state.backgroundInput,
                    onValueChange = { viewModel.onBackgroundInputChanged(it) },
                    placeholder = { Text("なぜこれに取り組むか…（任意）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Button(onClick = { viewModel.saveBackground() }) { Text("保存") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { viewModel.cancelEditingBackground() }) { Text("キャンセル") }
                }
            } else if (project.background != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "背景メモ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = project.background,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    IconButton(onClick = { viewModel.startEditingBackground() }) {
                        Icon(Icons.Default.Edit, contentDescription = "背景メモを編集", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                TextButton(
                    onClick = { viewModel.startEditingBackground() },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "+ 背景メモを追加",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // フォーカスボタン
            if (isFocused) {
                OutlinedButton(
                    onClick = { viewModel.clearFocus() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("フォーカスを外す")
                }
            } else {
                Button(
                    onClick = { viewModel.setFocus() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("フォーカスする")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ステップ",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        items(state.steps, key = { it.id }) { step ->
            StepItem(
                step = step,
                onMarkDone = { viewModel.markStepDone(step) },
                onDelete = { viewModel.deleteStep(step) },
            )
        }

        item {
            // ステップ追加
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.stepInput,
                    onValueChange = { viewModel.onStepInputChanged(it) },
                    placeholder = { Text("ステップを追加…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.addStep() },
                    enabled = state.stepInput.isNotBlank(),
                ) {
                    Text("追加")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // 紐付いたメモ
            if (state.memos.isNotEmpty()) {
                Text(
                    text = "関連メモ",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        items(state.memos, key = { "memo-${it.id}" }) { memo ->
            MemoReadItem(memo = memo)
            HorizontalDivider()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // 休止・アーカイブ
            var showArchiveConfirm by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.pause() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("休止する")
                }
                OutlinedButton(
                    onClick = { showArchiveConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("アーカイブする")
                }
            }

            if (showArchiveConfirm) {
                AlertDialog(
                    onDismissRequest = { showArchiveConfirm = false },
                    title = { Text("アーカイブしますか？") },
                    text = { Text("「${project.title}」をアーカイブします。一覧からは消えます。") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showArchiveConfirm = false
                                viewModel.archive()
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text("アーカイブ")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showArchiveConfirm = false }) {
                            Text("キャンセル")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun StepItem(
    step: StepEntity,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit,
) {
    val isDone = step.status == StepStatus.DONE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (!isDone) {
            IconButton(onClick = onMarkDone) {
                Icon(Icons.Default.Check, contentDescription = "完了", tint = MaterialTheme.colorScheme.primary)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun MemoReadItem(memo: MemoEntity) {
    Text(
        text = memo.text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}
