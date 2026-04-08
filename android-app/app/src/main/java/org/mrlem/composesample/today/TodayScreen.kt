package org.mrlem.composesample.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.mrlem.composesample.theme.Theme as AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
    suggestViewModel: MorningSuggestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val suggestState by suggestViewModel.state.collectAsStateWithLifecycle()
    var memoInput by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val today = remember { dateFormat.format(Date()) }
    val tomorrow = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        dateFormat.format(cal.time)
    }
    var showSuggestSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.detailItem) {
        if (state.detailItem == null) memoInput = ""
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("今日" to today, "明日" to tomorrow).forEach { (label, date) ->
                if (state.selectedDate == date) {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f).padding(AppTheme.size.smaller),
                    ) { Text(label) }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.onAction(TodayViewAction.SwitchDate(date)) },
                        modifier = Modifier.weight(1f).padding(AppTheme.size.smaller),
                    ) { Text(label) }
                }
            }
        }
        HorizontalDivider()

        if (state.timedItems.isEmpty() && state.untimedItems.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "今日やることがありません\nテーマのステップから追加してください",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(AppTheme.size.medium),
                    )
                    if (state.selectedDate == today) {
                        OutlinedButton(
                            onClick = {
                                showSuggestSheet = true
                                suggestViewModel.suggest()
                            },
                            modifier = Modifier.padding(top = AppTheme.size.small),
                        ) {
                            Text("AIに今日の候補を提案してもらう")
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.timedItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "タイムライン",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(
                                horizontal = AppTheme.size.medium,
                                vertical = AppTheme.size.small,
                            ),
                        )
                    }
                    items(state.timedItems, key = { it.scheduledStepId }) { item ->
                        ScheduledStepRow(
                            item = item,
                            onClick = { viewModel.onAction(TodayViewAction.ShowDetail(item)) },
                        )
                    }
                }
                if (state.untimedItems.isNotEmpty()) {
                    item {
                        if (state.timedItems.isNotEmpty()) HorizontalDivider()
                        Text(
                            text = "時刻未定",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(
                                horizontal = AppTheme.size.medium,
                                vertical = AppTheme.size.small,
                            ),
                        )
                    }
                    items(state.untimedItems, key = { it.scheduledStepId }) { item ->
                        ScheduledStepRow(
                            item = item,
                            onClick = { viewModel.onAction(TodayViewAction.ShowDetail(item)) },
                        )
                    }
                }
            }
        }
    }

    state.detailItem?.let { detail ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(TodayViewAction.HideDetail) },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.size.medium)
                    .navigationBarsPadding(),
            ) {
                Text(detail.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = detail.themeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = AppTheme.size.smaller),
                )
                if (detail.themeGoal != null) {
                    Text(
                        text = detail.themeGoal,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = AppTheme.size.smaller),
                    )
                }
                if (detail.starterAction != null) {
                    Text(
                        text = "入口: ${detail.starterAction}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = AppTheme.size.small),
                    )
                }
                if (detail.started) {
                    Text(
                        text = "着手済み",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = AppTheme.size.small),
                    )
                } else {
                    TextField(
                        value = memoInput,
                        onValueChange = { memoInput = it },
                        placeholder = { Text("メモ（任意）") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.size.medium),
                    )
                    Button(
                        onClick = {
                            viewModel.onAction(
                                TodayViewAction.MarkStarted(
                                    scheduledStepId = detail.scheduledStepId,
                                    memo = memoInput.ifBlank { null },
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.size.small),
                    ) {
                        Text("着手した")
                    }
                }
            }
        }
    }

    if (showSuggestSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSuggestSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.size.medium)
                    .navigationBarsPadding(),
            ) {
                Text("今日の候補ステップ", style = MaterialTheme.typography.titleMedium)
                when {
                    suggestState.loading -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(AppTheme.size.medium),
                        ) { CircularProgressIndicator() }
                    }
                    suggestState.error != null -> {
                        Text(
                            text = suggestState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = AppTheme.size.small),
                        )
                    }
                    suggestState.suggestions.isEmpty() -> {
                        Text(
                            text = "候補が見つかりませんでした",
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = AppTheme.size.small),
                        )
                    }
                    else -> {
                        suggestState.suggestions.forEach { suggestion ->
                            val accepted = suggestion.stepId in suggestState.acceptedIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppTheme.size.smaller),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = suggestion.stepTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (accepted) MaterialTheme.colorScheme.outline
                                                else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = suggestion.themeName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    if (suggestion.reason.isNotBlank()) {
                                        Text(
                                            text = suggestion.reason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                }
                                if (accepted) {
                                    Text(
                                        text = "✓",
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = AppTheme.size.small),
                                    )
                                } else {
                                    Button(
                                        onClick = { suggestViewModel.accept(suggestion.stepId) },
                                        modifier = Modifier.padding(start = AppTheme.size.small),
                                    ) { Text("追加") }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledStepRow(item: ScheduledStepUi, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.started) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.themeName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        if (item.startTime != null) {
            val h = item.startTime / 60
            val m = (item.startTime % 60).toString().padStart(2, '0')
            Text(
                text = "$h:$m",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = AppTheme.size.small),
            )
        }
        if (item.started) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = AppTheme.size.small),
            )
        }
    }
    HorizontalDivider()
}
