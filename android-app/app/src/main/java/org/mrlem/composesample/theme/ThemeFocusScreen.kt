package org.mrlem.composesample.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import org.mrlem.composesample.coaching.ChatMessage
import org.mrlem.composesample.coaching.CoachingViewAction
import org.mrlem.composesample.theme.Theme as AppTheme

@Composable
internal fun ThemeFocusScreen(
    viewModel: ThemeFocusViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

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
                text = "テーマを整理する",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            items(state.messages) { msg ->
                FocusMessageBubble(msg)
            }
            if (state.loading) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(AppTheme.size.medium),
                    ) { CircularProgressIndicator() }
                }
            }
            if (state.error != null) {
                item {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(AppTheme.size.medium),
                    )
                }
            }
        }

        HorizontalDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.size.small),
            modifier = Modifier.fillMaxWidth().padding(AppTheme.size.small),
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
            ) { Text("送信") }
        }
    }
}

@Composable
private fun FocusMessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    val bgColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant
    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.size.small, vertical = AppTheme.size.small),
    ) {
        Surface(color = bgColor, shape = MaterialTheme.shapes.medium) {
            Text(text = msg.content, modifier = Modifier.padding(AppTheme.size.small))
        }
    }
}
