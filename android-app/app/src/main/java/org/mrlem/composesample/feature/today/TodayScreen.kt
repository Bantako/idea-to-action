package org.mrlem.composesample.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.mrlem.composesample.data.db.DailyLogEntity
import org.mrlem.composesample.data.db.StepEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(
    contentPadding: PaddingValues,
    onNavigateToProjects: () -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        if (state.focusedProject == null) {
            item {
                NoFocusedProject(onNavigateToProjects = onNavigateToProjects)
            }
        } else {
            item {
                Text(
                    text = state.focusedProject!!.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (state.pendingSteps.isEmpty()) {
                item {
                    Text(
                        text = "未完了のステップがありません",
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(state.pendingSteps, key = { "step_${it.id}" }) { step ->
                    PendingStepRow(
                        step = step,
                        onDone = { viewModel.onAction(TodayAction.MarkStepDone(step)) },
                    )
                    HorizontalDivider()
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        item {
            Text(
                text = "今日だけのタスク",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        items(state.adHocSteps, key = { "adhoc_${it.id}" }) { step ->
            PendingStepRow(
                step = step,
                onDone = { viewModel.onAction(TodayAction.MarkStepDone(step)) },
            )
            HorizontalDivider()
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "今日の記録",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (state.todayLogs.isEmpty()) {
            item {
                Text(
                    text = "まだ記録がありません",
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        } else {
            items(state.todayLogs, key = { "log_${it.id}" }) { log ->
                DailyLogRow(
                    log = log,
                    onDelete = { viewModel.onAction(TodayAction.DeleteLog(log)) },
                )
                HorizontalDivider()
            }
        }

        item {
            ManualLogInput(
                value = state.manualInput,
                onValueChange = { viewModel.onAction(TodayAction.UpdateManualInput(it)) },
                onSubmit = { viewModel.onAction(TodayAction.AddManualLog(state.manualInput)) },
            )
        }
    }
}

@Composable
private fun NoFocusedProject(onNavigateToProjects: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "集中するプロジェクトを選んでください",
            color = MaterialTheme.colorScheme.outline,
        )
        TextButton(onClick = onNavigateToProjects) {
            Text("Projects を開く")
        }
    }
}

@Composable
private fun PendingStepRow(step: StepEntity, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = onDone) {
            Text("完了")
        }
    }
}

@Composable
private fun DailyLogRow(log: DailyLogEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(text = log.what, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = formatTime(log.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        TextButton(onClick = onDelete) {
            Text("削除", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ManualLogInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("一言記録を追加…") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        Button(
            onClick = onSubmit,
            enabled = value.isNotBlank(),
        ) {
            Text("追加")
        }
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
