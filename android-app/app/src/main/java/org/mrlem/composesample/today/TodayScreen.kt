package org.mrlem.composesample.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import org.mrlem.composesample.review.ReviewStepRow
import org.mrlem.composesample.review.ReviewViewAction
import org.mrlem.composesample.review.ReviewViewModel
import org.mrlem.composesample.theme.Theme as AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
    suggestViewModel: MorningSuggestViewModel = hiltViewModel(),
    reviewViewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val suggestState by suggestViewModel.state.collectAsStateWithLifecycle()
    val reviewState by reviewViewModel.state.collectAsStateWithLifecycle()
    var memoInput by remember { mutableStateOf("") }
    var reviewMemoInput by remember { mutableStateOf("") }
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
    LaunchedEffect(reviewState.detailItem) {
        reviewMemoInput = reviewState.detailItem?.memo ?: ""
    }
    LaunchedEffect(state.autoSuggestPending) {
        if (state.autoSuggestPending) {
            suggestViewModel.suggest()
            showSuggestSheet = true
            viewModel.onAction(TodayViewAction.AutoSuggestHandled)
        }
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

        if (state.items.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (suggestState.loading) {
                    CircularProgressIndicator()
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "今日やることがありません",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(AppTheme.size.medium),
                        )
                        Button(
                            onClick = { viewModel.onAction(TodayViewAction.ShowAddSheet) },
                            modifier = Modifier.padding(top = AppTheme.size.small),
                        ) {
                            Text("＋ やること追加")
                        }
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
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(state.items, key = { it.scheduledStepId }) { item ->
                    val index = state.items.indexOf(item)
                    ScheduledStepRow(
                        item = item,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.items.lastIndex,
                        onClick = { viewModel.onAction(TodayViewAction.ShowDetail(item)) },
                        onMoveUp = { viewModel.onAction(TodayViewAction.MoveStep(item.scheduledStepId, -1)) },
                        onMoveDown = { viewModel.onAction(TodayViewAction.MoveStep(item.scheduledStepId, +1)) },
                    )
                }
                item {
                    Text(
                        text = "＋ やること追加",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onAction(TodayViewAction.ShowAddSheet) }
                            .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small),
                    )
                    HorizontalDivider()
                }
                if (state.isReviewMode && state.selectedDate == today && reviewState.items.isNotEmpty()) {
                    item {
                        HorizontalDivider()
                        Text(
                            text = "今日の振り返り",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(
                                horizontal = AppTheme.size.medium,
                                vertical = AppTheme.size.small,
                            ),
                        )
                    }
                    items(reviewState.items, key = { "review_${it.scheduledStepId}" }) { item ->
                        ReviewStepRow(
                            item = item,
                            onDetailClick = { reviewViewModel.onAction(ReviewViewAction.ShowDetail(item)) },
                            onMarkResult = { result ->
                                reviewViewModel.onAction(ReviewViewAction.MarkResult(item.scheduledStepId, item.stepId, result))
                            },
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

    reviewState.detailItem?.let { detail ->
        ModalBottomSheet(
            onDismissRequest = { reviewViewModel.onAction(ReviewViewAction.HideDetail) },
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
                if (detail.starterAction != null) {
                    Text(
                        text = "入口: ${detail.starterAction}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = AppTheme.size.small),
                    )
                }
                TextField(
                    value = reviewMemoInput,
                    onValueChange = { reviewMemoInput = it },
                    placeholder = { Text("メモ（任意）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.size.medium),
                )
                if (detail.result == "done") {
                    Button(
                        onClick = { reviewViewModel.onAction(ReviewViewAction.ArchiveStep(detail.stepId)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.size.small),
                    ) {
                        Text("ステップを完了・アーカイブ")
                    }
                }
                OutlinedButton(
                    onClick = {
                        reviewViewModel.onAction(
                            ReviewViewAction.CarryOver(
                                scheduledStepId = detail.scheduledStepId,
                                stepId = detail.stepId,
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.size.smaller),
                ) {
                    Text("明日に持ち越す")
                }
            }
        }
    }

    if (state.showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(TodayViewAction.HideAddSheet) },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.size.medium)
                    .navigationBarsPadding(),
            ) {
                Text("ステップを選択", style = MaterialTheme.typography.titleMedium)
                if (state.availableSteps.isEmpty()) {
                    Text(
                        text = "追加できるステップがありません",
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = AppTheme.size.medium),
                    )
                } else {
                    state.availableSteps.forEach { step ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onAction(TodayViewAction.AddStepToToday(step.stepId)) }
                                .padding(vertical = AppTheme.size.small),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.stepTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = step.themeName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                        HorizontalDivider()
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
private fun ScheduledStepRow(
    item: ScheduledStepUi,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val titleColor by animateColorAsState(
        targetValue = if (item.started) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "titleColor",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = AppTheme.size.medium, end = AppTheme.size.smaller, top = AppTheme.size.small, bottom = AppTheme.size.small),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
            )
            Text(
                text = item.themeName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            if (item.starterAction != null) {
                Text(
                    text = "入口: ${item.starterAction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        AnimatedVisibility(
            visible = item.started,
            enter = scaleIn(animationSpec = tween(durationMillis = 250)) + fadeIn(animationSpec = tween(durationMillis = 250)),
        ) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = AppTheme.size.small),
            )
        }
        Column {
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "上へ",
                    tint = if (canMoveUp) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.outlineVariant,
                )
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "下へ",
                    tint = if (canMoveDown) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
    HorizontalDivider()
}
