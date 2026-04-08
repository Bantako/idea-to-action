package org.mrlem.composesample.inbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mrlem.composesample.theme.Theme

@Composable
internal fun InboxDetailScreen(
    viewModel: InboxDetailViewModel,
    onNavigateBack: () -> Unit,
    onStartCoaching: (String) -> Unit,
    onNavigateToTheme: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InboxDetailViewEffect.NavigateBack -> onNavigateBack()
                is InboxDetailViewEffect.NavigateToCoaching -> onStartCoaching(effect.entryId)
                is InboxDetailViewEffect.NavigateToTheme -> onNavigateToTheme(effect.themeId)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onNavigateBack) {
            Text("← 戻る")
        }
        Text(
            text = state.text,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(Theme.size.medium),
        )
        Button(
            onClick = { viewModel.onAction(InboxDetailViewAction.StartCoaching) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.size.medium),
        ) {
            Text("AIと一緒に考える")
        }
        OutlinedButton(
            onClick = { viewModel.onAction(InboxDetailViewAction.CreateTheme) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.size.medium, vertical = Theme.size.small),
        ) {
            Text("手動でテーマを作る")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.size.medium),
        ) {
            OutlinedButton(
                onClick = { viewModel.onAction(InboxDetailViewAction.Delete) },
                modifier = Modifier.weight(1f),
            ) {
                Text("アーカイブ")
            }
        }
    }
}
