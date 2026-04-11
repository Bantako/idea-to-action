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
import org.mrlem.composesample.today.StepScheduleSheet
import org.mrlem.composesample.theme.Theme as AppTheme

@Composable
internal fun ThemeDetailScreen(
    viewModel: ThemeDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStep: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var titleInput by remember { mutableStateOf("") }
    var goalInput by remember { mutableStateOf("") }
    var stepInput by remember { mutableStateOf("") }
    var scheduleSheetStepId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ThemeDetailViewEffect.NavigateBack -> onNavigateBack()
                is ThemeDetailViewEffect.NavigateToStep -> onNavigateToStep(effect.stepId)
            }
        }
    }

    LaunchedEffect(state.title) {
        if (state.title.isNotEmpty() && titleInput.isEmpty()) {
            titleInput = state.title
            goalInput = state.goal
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onNavigateBack) { Text("← 戻る") }
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { viewModel.onAction(ThemeDetailViewAction.Archive) }) {
                Text("アーカイブ", color = MaterialTheme.colorScheme.error)
            }
        }
        HorizontalDivider()

        TextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            placeholder = { Text("テーマ名") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small)
                .onFocusChanged { if (!it.isFocused) viewModel.onAction(ThemeDetailViewAction.SaveTitle(titleInput)) },
        )

        TextField(
            value = goalInput,
            onValueChange = { goalInput = it },
            placeholder = { Text("ゴール・目的（任意）") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small)
                .onFocusChanged { if (!it.isFocused) viewModel.onAction(ThemeDetailViewAction.SaveGoal(goalInput)) },
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.size.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small),
        ) {
            listOf("light" to "軽", "medium" to "中", "heavy" to "重").forEach { (weight, label) ->
                if (state.weight == weight) {
                    Button(onClick = {}, modifier = Modifier.weight(1f)) { Text(label) }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.onAction(ThemeDetailViewAction.SetWeight(weight)) },
                        modifier = Modifier.weight(1f),
                    ) { Text(label) }
                }
            }
        }

        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.size.medium),
        ) {
            Text(
                text = "ステップ",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (state.totalStepCount > 0) {
                Text(
                    text = "完了 ${state.completedStepCount} / ${state.totalStepCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.steps.isEmpty()) {
                item {
                    Text(
                        text = "ステップがありません",
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = AppTheme.size.medium),
                    )
                }
            } else {
                items(state.steps, key = { it.id }) { step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onAction(ThemeDetailViewAction.NavigateToStep(step.id)) }
                            .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = step.title)
                            if (step.starterAction != null) {
                                Text(
                                    text = "入口: ${step.starterAction}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                        WeightBadge(weight = step.weight)
                        TextButton(
                            onClick = { scheduleSheetStepId = step.id },
                        ) {
                            Text("今日やる", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = { viewModel.onAction(ThemeDetailViewAction.ArchiveStep(step.id)) },
                        ) {
                            Text("完了", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.size.small),
        ) {
            TextField(
                value = stepInput,
                onValueChange = { stepInput = it },
                placeholder = { Text("ステップを追加…") },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    viewModel.onAction(ThemeDetailViewAction.AddStep(stepInput))
                    stepInput = ""
                },
                modifier = Modifier.padding(start = AppTheme.size.small),
            ) {
                Text("追加")
            }
        }

        if (state.activityLog.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = "実績ログ",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(AppTheme.size.medium),
            )
            state.activityLog.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.smaller),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.stepTitle,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (entry.memo != null) {
                            Text(
                                text = entry.memo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = AppTheme.size.small),
                    )
                    Text(
                        text = when (entry.result) {
                            "done" -> "できた"
                            "started" -> "着手"
                            else -> "未達"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (entry.result) {
                            "done" -> MaterialTheme.colorScheme.primary
                            "started" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        },
                    )
                }
                HorizontalDivider()
            }
        }
    }

    scheduleSheetStepId?.let { stepId ->
        StepScheduleSheet(
            onDismiss = { scheduleSheetStepId = null },
            onConfirm = { date, startTime, durationMinutes, notificationEnabled ->
                viewModel.onAction(ThemeDetailViewAction.ScheduleStep(stepId, date, startTime, durationMinutes, notificationEnabled))
                scheduleSheetStepId = null
            },
        )
    }
}
