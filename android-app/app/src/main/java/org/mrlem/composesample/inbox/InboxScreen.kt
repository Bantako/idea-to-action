package org.mrlem.composesample.inbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mrlem.composesample.theme.Theme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun InboxScreen(
    onEntryClick: (String) -> Unit,
    onBatchCoaching: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InboxViewEffect.NavigateToEntry -> onEntryClick(effect.id)
                is InboxViewEffect.NavigateToBatchCoaching -> onBatchCoaching(effect.entryIds)
            }
        }
    }

    Inbox(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Inbox(
    state: InboxViewState,
    onAction: (InboxViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.isSelectMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Theme.size.small),
            ) {
                Text(
                    text = "${state.selectedIds.size}件選択中",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { onAction(InboxViewAction.ExitSelectMode) },
                    modifier = Modifier.padding(end = Theme.size.small),
                ) { Text("キャンセル") }
                Button(
                    onClick = { onAction(InboxViewAction.StartBatchCoaching) },
                    enabled = state.selectedIds.isNotEmpty(),
                ) { Text("まとめてAIと整理") }
            }
            HorizontalDivider()
        }

        if (state.entries.isEmpty()) {
            Text(
                text = "アイデアや「やりたいこと」を入力してください",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(Theme.size.medium),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onAction(InboxViewAction.EntryClick(entry.id)) },
                                onLongClick = { onAction(InboxViewAction.LongPress(entry.id)) },
                            )
                            .padding(horizontal = Theme.size.medium, vertical = Theme.size.small),
                    ) {
                        if (state.isSelectMode) {
                            Checkbox(
                                checked = entry.id in state.selectedIds,
                                onCheckedChange = { onAction(InboxViewAction.ToggleSelect(entry.id)) },
                            )
                        }
                        Text(
                            text = entry.text,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    HorizontalDivider()
                }
            }
        }

        if (!state.isSelectMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Theme.size.small),
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("アイデアを入力…") },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        onAction(InboxViewAction.AddEntry(input))
                        input = ""
                    },
                    modifier = Modifier.padding(start = Theme.size.small),
                ) {
                    Text("追加")
                }
            }
        }
    }
}
