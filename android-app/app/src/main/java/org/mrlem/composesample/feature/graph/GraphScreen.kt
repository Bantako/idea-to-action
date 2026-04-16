package org.mrlem.composesample.feature.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.mrlem.composesample.data.db.NodeEntity
import org.mrlem.composesample.data.db.NodeStatus
import org.mrlem.composesample.data.db.ThemeEntity

@Composable
fun GraphScreen(
    contentPadding: PaddingValues,
    viewModel: GraphViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            val items = state.allItems
            if (items.isNotEmpty()) {
                items(items, key = { "n_${it.node.id}" }) { item ->
                    NodeItemRow(
                        item = item,
                        theme = state.themeFor(item.node.themeId),
                        isSelected = item.node.id in state.selectedNodeIds,
                        isSelectMode = state.isSelectMode,
                        onEdit = { viewModel.onAction(GraphAction.ShowEdit(item.node)) },
                        onLongPress = { viewModel.onAction(GraphAction.ToggleSelect(item.node.id)) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            } else {
                item(key = "empty") {
                    Text(
                        text = "ノードがありません",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            // 対応しないゾーン（折りたたみ）
            val deferred = state.deferredItems
            if (deferred.isNotEmpty()) {
                item(key = "deferred_header") {
                    DeferredSectionHeader(
                        count = deferred.size,
                        expanded = state.showDeferred,
                        onToggle = { viewModel.onAction(GraphAction.ToggleDeferred) },
                    )
                }
                if (state.showDeferred) {
                    items(deferred, key = { "d_${it.node.id}" }) { item ->
                        DeferredNodeRow(
                            item = item,
                            onRestore = { viewModel.onAction(GraphAction.RestoreNode(item.node)) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }

        // 選択モードのボトムバー
        AnimatedVisibility(
            visible = state.isSelectMode,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${state.selectedNodeIds.size}件選択中",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Row {
                    if (state.themes.isNotEmpty()) {
                        TextButton(onClick = { viewModel.onAction(GraphAction.ShowBulkAssign) }) {
                            Text("既存に追加")
                        }
                    }
                    TextButton(onClick = { viewModel.onAction(GraphAction.ShowCreateTheme) }) {
                        Text("テーマ命名")
                    }
                    TextButton(onClick = { viewModel.onAction(GraphAction.ClearSelection) }) {
                        Text("キャンセル")
                    }
                }
            }
        }
    }

    // テーマ割り当てダイアログ
    if (state.assignThemeTarget != null) {
        val target = state.assignThemeTarget!!
        AlertDialog(
            onDismissRequest = { viewModel.onAction(GraphAction.DismissAssignTheme) },
            title = { Text("「${target.title}」のテーマを選択") },
            text = {
                LazyColumn {
                    item {
                        TextButton(
                            onClick = { viewModel.onAction(GraphAction.AssignTheme(null)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("未整理に戻す")
                        }
                    }
                    items(state.themes, key = { it.id }) { theme ->
                        TextButton(
                            onClick = { viewModel.onAction(GraphAction.AssignTheme(theme.id)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(theme.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(GraphAction.DismissAssignTheme) }) {
                    Text("キャンセル")
                }
            },
        )
    }

    // ノード編集ダイアログ
    if (state.editTarget != null) {
        val target = state.editTarget!!
        var editTitle by remember(target.id) { mutableStateOf(target.title) }
        var editBody by remember(target.id) { mutableStateOf(target.body) }
        AlertDialog(
            onDismissRequest = { viewModel.onAction(GraphAction.DismissEdit) },
            title = {
                Column {
                    Text(
                        text = target.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = target.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("タイトル") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editBody,
                        onValueChange = { editBody = it },
                        label = { Text("メモ（任意）") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onAction(GraphAction.SaveEdit(editTitle, editBody)) },
                    enabled = editTitle.isNotBlank(),
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.onAction(GraphAction.DeferNode) }) {
                        Text("今は対応しない", color = MaterialTheme.colorScheme.secondary)
                    }
                    TextButton(onClick = { viewModel.onAction(GraphAction.DeleteNode) }) {
                        Text("削除", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { viewModel.onAction(GraphAction.DismissEdit) }) {
                        Text("キャンセル")
                    }
                }
            },
        )
    }

    // テーマ削除確認ダイアログ
    if (state.deleteThemeTarget != null) {
        val target = state.deleteThemeTarget!!
        AlertDialog(
            onDismissRequest = { viewModel.onAction(GraphAction.DismissDeleteTheme) },
            title = { Text("「${target.name}」を削除") },
            text = { Text("テーマを削除します。所属ノードは未整理に戻ります。") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(GraphAction.ConfirmDeleteTheme) }) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(GraphAction.DismissDeleteTheme) }) {
                    Text("キャンセル")
                }
            },
        )
    }

    // 既存テーマへ一括追加ダイアログ
    if (state.showBulkAssign) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(GraphAction.DismissBulkAssign) },
            title = { Text("テーマに追加（${state.selectedNodeIds.size}件）") },
            text = {
                LazyColumn {
                    items(state.themes, key = { it.id }) { theme ->
                        TextButton(
                            onClick = { viewModel.onAction(GraphAction.BulkAssign(theme.id)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(theme.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(GraphAction.DismissBulkAssign) }) {
                    Text("キャンセル")
                }
            },
        )
    }

    // テーマ作成ダイアログ
    if (state.showCreateTheme) {
        var themeName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.onAction(GraphAction.DismissCreateTheme) },
            title = { Text("テーマを作成") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.themeSuggestions.isNotEmpty()) {
                        Text(
                            text = "AI の提案",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.themeSuggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { themeName = suggestion },
                                    label = { Text(suggestion) },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = themeName,
                        onValueChange = { themeName = it },
                        label = { Text("テーマ名") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onAction(GraphAction.CreateTheme(themeName)) },
                    enabled = themeName.isNotBlank(),
                ) {
                    Text("作成")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(GraphAction.DismissCreateTheme) }) {
                    Text("キャンセル")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeferredSectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${if (expanded) "▼" else "▶"} 今は対応しない（$count）",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun DeferredNodeRow(item: NodeItem, onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.node.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        TextButton(onClick = onRestore) {
            Text("復活", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NodeItemRow(
    item: NodeItem,
    theme: ThemeEntity? = null,
    isSelected: Boolean = false,
    isSelectMode: Boolean = false,
    onEdit: () -> Unit,
    onLongPress: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectMode) onLongPress() else onEdit() },
                onLongClick = onLongPress,
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                ) else Modifier
            )
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = item.node.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.node.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        if (theme != null) {
            Text(
                text = "# ${theme.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (item.node.body.isNotBlank()) {
            Text(
                text = item.node.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
