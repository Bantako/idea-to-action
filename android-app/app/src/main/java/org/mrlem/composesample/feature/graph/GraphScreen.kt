package org.mrlem.composesample.feature.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.mrlem.composesample.data.db.EdgeEntity
import org.mrlem.composesample.data.db.NodeEntity
import org.mrlem.composesample.data.db.NodeStatus
import org.mrlem.composesample.data.db.ThemeEntity

@Composable
fun GraphScreen(
    contentPadding: PaddingValues,
    viewModel: GraphViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // 未整理ゾーン
        item(key = "unorganized_header") {
            SectionHeader(
                title = "未整理",
                action = {
                    TextButton(onClick = { viewModel.onAction(GraphAction.ShowCreateTheme) }) {
                        Text("+ テーマ作成", style = MaterialTheme.typography.labelMedium)
                    }
                },
            )
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
                    themes = state.themes,
                    onAddPrereq = { viewModel.onAction(GraphAction.ShowAddEdge(item.node)) },
                    onRemovePrereq = { edge -> viewModel.onAction(GraphAction.RemoveEdge(edge)) },
                    onAssignTheme = { viewModel.onAction(GraphAction.ShowAssignTheme(item.node)) },
                    onEdit = { viewModel.onAction(GraphAction.ShowEdit(item.node)) },
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
                            themes = state.themes,
                            onAddPrereq = { viewModel.onAction(GraphAction.ShowAddEdge(item.node)) },
                            onRemovePrereq = { edge -> viewModel.onAction(GraphAction.RemoveEdge(edge)) },
                            onAssignTheme = { viewModel.onAction(GraphAction.ShowAssignTheme(item.node)) },
                            onEdit = { viewModel.onAction(GraphAction.ShowEdit(item.node)) },
                            indented = true,
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 32.dp))
                    }
                }
            }
        }
    }

    // 前提追加ダイアログ
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
            title = { Text("ノードを編集") },
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
                        minLines = 3,
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
                TextButton(onClick = { viewModel.onAction(GraphAction.DismissEdit) }) {
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
private fun SectionHeader(
    title: String,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
    }
}

@Composable
private fun ThemeSectionHeader(
    theme: ThemeEntity,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
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

@Composable
private fun NodeItemRow(
    item: NodeItem,
    themes: List<ThemeEntity>,
    onAddPrereq: () -> Unit,
    onRemovePrereq: (EdgeEntity) -> Unit,
    onAssignTheme: () -> Unit,
    onEdit: () -> Unit,
    indented: Boolean = false,
) {
    val isReady = item.node.status == NodeStatus.READY
    val startPadding = if (indented) 32.dp else 16.dp
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isReady) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .drawBehind {
                if (isReady) {
                    drawLine(
                        color = primaryColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 4.dp.toPx(),
                    )
                }
            }
            .padding(start = startPadding, end = 16.dp, top = 10.dp, bottom = 6.dp),
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

        if (item.node.body.isNotBlank()) {
            Text(
                text = item.node.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            TextButton(
                onClick = onAddPrereq,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            ) {
                Text(
                    text = "+ 前提を追加",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            TextButton(
                onClick = onAssignTheme,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(
                    text = "テーマ変更",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            TextButton(
                onClick = onEdit,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(
                    text = "編集",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
