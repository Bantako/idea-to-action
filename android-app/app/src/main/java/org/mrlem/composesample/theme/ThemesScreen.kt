package org.mrlem.composesample.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mrlem.composesample.theme.Theme as AppTheme

@Composable
internal fun ThemesScreen(
    onThemeClick: (String) -> Unit,
    onFocusCoaching: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ThemesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        if (state.themes.size > 3) {
            OutlinedButton(
                onClick = onFocusCoaching,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small),
            ) {
                Text("今フォーカスするテーマを整理する")
            }
            HorizontalDivider()
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (state.themes.isEmpty()) {
                item {
                    Text(
                        text = "テーマがありません\n下のフォームから追加してください",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.size.medium),
                    )
                }
            } else {
                items(state.themes, key = { it.id }) { theme ->
                    ThemeRow(theme = theme, onClick = { onThemeClick(theme.id) })
                }
            }

            if (state.archivedThemes.isNotEmpty()) {
                item {
                    HorizontalDivider()
                    TextButton(
                        onClick = { viewModel.onAction(ThemesViewAction.ToggleShowArchived) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.size.medium),
                    ) {
                        Text(
                            if (state.showArchived)
                                "アーカイブ済みを隠す（${state.archivedThemes.size}件）"
                            else
                                "アーカイブ済みを表示（${state.archivedThemes.size}件）"
                        )
                    }
                }
                if (state.showArchived) {
                    items(state.archivedThemes, key = { "archived_${it.id}" }) { theme ->
                        ThemeRow(theme = theme, onClick = { onThemeClick(theme.id) }, dimmed = true)
                    }
                }
            }
        }

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.size.small),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = state.input,
                    onValueChange = { viewModel.onAction(ThemesViewAction.InputChange(it)) },
                    placeholder = { Text("テーマ名…") },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { viewModel.onAction(ThemesViewAction.AddTheme) },
                    modifier = Modifier.padding(start = AppTheme.size.small),
                ) {
                    Text("追加")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.size.small),
                modifier = Modifier.padding(top = AppTheme.size.small),
            ) {
                listOf("light" to "軽", "medium" to "中", "heavy" to "重").forEach { (weight, label) ->
                    if (state.inputWeight == weight) {
                        Button(onClick = {}) { Text(label) }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.onAction(ThemesViewAction.InputWeightChange(weight)) },
                        ) { Text(label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(
    theme: ThemeItem,
    onClick: () -> Unit,
    dimmed: Boolean = false,
) {
    val textColor = if (dimmed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = theme.title, color = textColor)
        }
        WeightBadge(weight = theme.weight)
        if (theme.pendingStepCount > 0) {
            Text(
                text = "${theme.pendingStepCount}ステップ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = AppTheme.size.small),
            )
        }
    }
    HorizontalDivider()
}

@Composable
internal fun WeightBadge(weight: String) {
    val (label, color) = when (weight) {
        "light" -> "軽" to MaterialTheme.colorScheme.tertiary
        "heavy" -> "重" to MaterialTheme.colorScheme.error
        else -> "中" to MaterialTheme.colorScheme.primary
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = AppTheme.size.small, vertical = AppTheme.size.smaller),
        )
    }
}
