package org.mrlem.composesample.review

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mrlem.composesample.theme.Theme as AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewScreen(
    onNavigateToReviewCoaching: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var memoInput by remember { mutableStateOf("") }

    LaunchedEffect(state.detailItem) {
        memoInput = state.detailItem?.memo ?: ""
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.items.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "今日のスケジュールはありません",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(AppTheme.size.medium),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
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
                items(state.items, key = { it.scheduledStepId }) { item ->
                    ReviewStepRow(
                        item = item,
                        onDetailClick = { viewModel.onAction(ReviewViewAction.ShowDetail(item)) },
                        onMarkResult = { result ->
                            viewModel.onAction(ReviewViewAction.MarkResult(item.scheduledStepId, item.stepId, result))
                        },
                    )
                }
            }
        }
        Button(
            onClick = onNavigateToReviewCoaching,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.size.medium),
        ) {
            Text("AIに相談する")
        }
    }

    state.detailItem?.let { detail ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(ReviewViewAction.HideDetail) },
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
                    value = memoInput,
                    onValueChange = { memoInput = it },
                    placeholder = { Text("メモ（任意）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.size.medium),
                )

                if (detail.result == "done") {
                    Button(
                        onClick = { viewModel.onAction(ReviewViewAction.ArchiveStep(detail.stepId)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.size.small),
                    ) {
                        Text("ステップを完了・アーカイブ")
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.onAction(
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
}

@Composable
internal fun ReviewStepRow(
    item: ReviewStepUi,
    onDetailClick: () -> Unit,
    onMarkResult: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.size.medium, vertical = AppTheme.size.small),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onDetailClick),
            ) {
                Text(
                    text = item.title,
                    color = if (item.result == "done") MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = item.themeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                if (item.memo != null) {
                    Text(
                        text = item.memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf(
                "done" to "できた",
                "started" to "着手した",
                "not_done" to "できなかった",
            ).forEach { (result, label) ->
                val selected = item.result == result
                if (selected) {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = label,
                            color = when (result) {
                                "done" -> MaterialTheme.colorScheme.primary
                                "started" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                } else {
                    TextButton(
                        onClick = { onMarkResult(result) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider()
}
