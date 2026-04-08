package org.mrlem.composesample.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mrlem.composesample.theme.Theme as AppTheme

@Composable
internal fun StepDetailScreen(
    viewModel: StepDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var titleInput by remember { mutableStateOf("") }
    var starterInput by remember { mutableStateOf("") }

    LaunchedEffect(state.title) {
        if (state.title.isNotEmpty() && titleInput.isEmpty()) {
            titleInput = state.title
            starterInput = state.starterAction
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onNavigateBack) { Text("← 戻る") }
            Text(
                text = "ステップ編集",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()

        TextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            placeholder = { Text("ステップのタイトル") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small)
                .onFocusChanged { if (!it.isFocused) viewModel.onAction(StepDetailViewAction.SaveTitle(titleInput)) },
        )

        TextField(
            value = starterInput,
            onValueChange = { starterInput = it },
            placeholder = { Text("入口アクション（任意）: 最初の5分でやること") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small)
                .onFocusChanged {
                    if (!it.isFocused) viewModel.onAction(StepDetailViewAction.SaveStarterAction(starterInput))
                },
        )

        Text(
            text = "重さ",
            modifier = Modifier.padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.size.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.size.medium),
        ) {
            listOf("light" to "軽（〜15分）", "medium" to "中（〜1時間）", "heavy" to "重（腰を据えて）").forEach { (weight, label) ->
                if (state.weight == weight) {
                    Button(onClick = {}, modifier = Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelSmall) }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.onAction(StepDetailViewAction.SetWeight(weight)) },
                        modifier = Modifier.weight(1f),
                    ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}
