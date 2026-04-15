package org.mrlem.composesample.feature.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.mrlem.composesample.data.db.EdgeType
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
        // 未整理ゾーン
        item(key = "unorganized_header") {
            SectionHeader(title = "未整理")
        }

        val unorganized = state.unorganizedItems
        if (unorganized.isEmpty()) {
            item(key = "unorganized_empty") {
                Text(
                    text = "未整理のノードはありません",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } else {
            items(unorganized, key = { "u_${it.node.id}" }) { item ->
                NodeItemRow(
                    item = item,
                    isSelected = item.node.id in state.selectedNodeIds,
                    isSelectMode = state.isSelectMode,
                    onEdit = { viewModel.onAction(GraphAction.ShowEdit(item.node)) },
                    onLongPress = { viewModel.onAction(GraphAction.ToggleSelect(item.node.id)) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }

        // テーマ別ゾーン
        items(state.themes, key = { "theme_${it.id}" }) { theme ->
            val expanded = state.isThemeExpanded(theme.id)
            val themeItems = state.itemsForTheme(theme.id)

            ThemeSectionHeader(
                theme = theme,
                count = themeItems.size,
                expanded = expanded,
                onToggle = { viewModel.onAction(GraphAction.ToggleTheme(theme.id)) },
                onLongPress = { viewModel.onAction(GraphAction.ShowDeleteTheme(theme)) },
            )

            if (expanded) {
                if (themeItems.isEmpty()) {
                    Text(
                        text = "ノードなし",
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    themeItems.forEach { item ->
                        NodeItemRow(
                            item = item,
                            isSelected = item.node.id in state.selectedNodeIds,
                            isSelectMode = state.isSelectMode,
                            onEdit = { viewModel.onAction(GraphAction.ShowEdit(item.node)) },
                            onLongPress = { viewModel.onAction(GraphAction.ToggleSelect(item.node.id)) },
                            indented = true,
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 32.dp))
                    }
                }
            }
        }
    }

    // 選択モードのボトムバー
    AnimatedVisibility(
        visible = state.isSelectMode,
        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
    } // Box

    // 接続追加ダイアログ
    if (state.addEdgeTarget != null) {
        val target = state.addEdgeTarget!!
        val candidates = state.allNodes.filter { it.id != target.id }
        AlertDialog(
            onDismissRequest = { viewModel.onAction(GraphAction.DismissAddEdge) },
            title = { Text("「${target.title}」に接続を追加") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            EdgeType.PREREQUISITE to "前提",
                            EdgeType.RELATED to "関連",
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = state.addEdgeType == type,
                                onClick = { viewModel.onAction(GraphAction.SelectEdgeType(type)) },
                                label = { Text(label) },
                            )
                        }
                    }
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

    // 接続管理ダイアログ（ノードタップで開く）
    if (state.editTarget != null) {
        val target = state.editTarget!!
        val targetItem = state.allItems.find { it.node.id == target.id }
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
                    val statusLabel = if (target.status == NodeStatus.READY) "READY ★" else target.status.name
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (target.status == NodeStatus.READY)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // --- 接続セクション（主役）---
                    if (targetItem != null && targetItem.prereqs.isNotEmpty()) {
                        Text(
                            text = "前提",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            targetItem.prereqs.forEach { prereq ->
                                SuggestionChip(
                                    onClick = { viewModel.onAction(GraphAction.RemoveEdge(prereq.edge)) },
                                    label = { Text("× ${prereq.fromNode.title}") },
                                )
                            }
                        }
                    }

                    if (targetItem != null && targetItem.relatedNodes.isNotEmpty()) {
                        Text(
                            text = "関連",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = targetItem.relatedNodes.joinToString("、") { it.title },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    TextButton(
                        onClick = {
                            viewModel.onAction(GraphAction.DismissEdit)
                            viewModel.onAction(GraphAction.ShowAddEdge(target))
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    ) {
                        Text("+ 接続を追加", style = MaterialTheme.typography.labelMedium)
                    }

                    HorizontalDivider()

                    // --- 編集セクション（副）---
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
                    TextButton(onClick = { viewModel.onAction(GraphAction.AbandonNode) }) {
                        Text("やめる", color = MaterialTheme.colorScheme.error)
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
                OutlinedTextField(
                    value = themeName,
                    onValueChange = { themeName = it },
                    label = { Text("テーマ名") },
                    singleLine = true,
                )
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

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeSectionHeader(
    theme: ThemeEntity,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (expanded) "▼" else "▶",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = theme.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NodeItemRow(
    item: NodeItem,
    isSelected: Boolean = false,
    isSelectMode: Boolean = false,
    onEdit: () -> Unit,
    onLongPress: () -> Unit = {},
    indented: Boolean = false,
) {
    val isReady = item.node.status == NodeStatus.READY
    val startPadding = if (indented) 32.dp else 16.dp
    val primaryColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectMode) onLongPress() else onEdit() },
                onLongClick = onLongPress,
            )
            .background(
                when {
                    isSelected -> selectedColor.copy(alpha = 0.15f)
                    isReady -> primaryColor.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    color = selectedColor.copy(alpha = 0.5f),
                ) else Modifier
            )
            .drawBehind {
                if (isReady && !isSelected) {
                    drawLine(
                        color = primaryColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 4.dp.toPx(),
                    )
                }
            }
            .padding(start = startPadding, end = 16.dp, top = 10.dp, bottom = 10.dp),
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
                fontWeight = if (isReady) FontWeight.Bold else FontWeight.Normal,
                color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isReady) "READY ★" else item.node.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                fontWeight = if (isReady) FontWeight.Bold else FontWeight.Normal,
            )
        }

        if (item.prereqs.isNotEmpty()) {
            Text(
                text = "前提: " + item.prereqs.joinToString("、") { it.fromNode.title },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (item.relatedNodes.isNotEmpty()) {
            Text(
                text = "関連: " + item.relatedNodes.joinToString("、") { it.title },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
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
