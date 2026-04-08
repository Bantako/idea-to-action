package org.mrlem.composesample.coaching

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mrlem.composesample.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewCoachingScreen(
    viewModel: ReviewCoachingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var addedSteps by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(state.showAddStep) {
        if (state.showAddStep) {
            addedSteps = emptySet()
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onNavigateBack) { Text("← 戻る") }
            Text(
                text = "AIに相談する",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            items(state.messages) { msg ->
                ReviewMessageBubble(msg)
            }
            if (state.loading) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Theme.size.medium),
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (state.error != null) {
                item {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(Theme.size.medium),
                    )
                }
            }
        }

        HorizontalDivider()
        if (state.availableThemes.isNotEmpty()) {
            OutlinedButton(
                onClick = { viewModel.onAction(CoachingViewAction.ShowAddStep) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Theme.size.small, vertical = Theme.size.smaller),
            ) {
                Text("提案されたステップを追加する")
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.size.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.size.small),
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("返答…") },
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = {
                    viewModel.onAction(CoachingViewAction.Send(input))
                    input = ""
                },
                enabled = !state.loading && input.isNotBlank(),
            ) {
                Text("送信")
            }
        }
    }

    if (state.showAddStep) {
        ModalBottomSheet(onDismissRequest = { viewModel.onAction(CoachingViewAction.HideAddStep) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Theme.size.medium)
                    .navigationBarsPadding(),
            ) {
                Text("ステップを追加する", style = MaterialTheme.typography.titleMedium)

                HorizontalDivider(modifier = Modifier.padding(top = Theme.size.medium))

                if (state.suggestedSteps.isEmpty()) {
                    Text(
                        text = "ステップの提案が見つかりませんでした",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = Theme.size.medium),
                    )
                } else {
                    state.suggestedSteps.forEach { step ->
                        val added = step.title in addedSteps
                        val themeName = state.availableThemes.firstOrNull { it.id == step.themeId }?.title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Theme.size.smaller),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (added) MaterialTheme.colorScheme.outline
                                            else MaterialTheme.colorScheme.onSurface,
                                )
                                if (themeName != null) {
                                    Text(
                                        text = themeName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                            if (added) {
                                Text(
                                    text = "✓",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = Theme.size.small),
                                )
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.onAction(CoachingViewAction.AddStep(step.title, step.themeId))
                                        addedSteps = addedSteps + step.title
                                    },
                                    modifier = Modifier.padding(start = Theme.size.small),
                                ) {
                                    Text("＋")
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }

                TextButton(
                    onClick = { viewModel.onAction(CoachingViewAction.HideAddStep) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Theme.size.small),
                ) {
                    Text("閉じる")
                }
            }
        }
    }
}

@Composable
private fun ReviewMessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    val bgColor = if (isUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.size.small, vertical = Theme.size.small),
    ) {
        Surface(
            color = bgColor,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = msg.content,
                modifier = Modifier.padding(Theme.size.small),
            )
        }
    }
}
