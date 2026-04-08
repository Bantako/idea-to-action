package org.mrlem.composesample.review

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.step.StepDao
import org.mrlem.composesample.theme.ThemeDao
import org.mrlem.composesample.today.ScheduledStepDao
import org.mrlem.composesample.today.ScheduledStepEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val scheduledStepDao: ScheduledStepDao,
    private val stepDao: StepDao,
    private val themeDao: ThemeDao,
) : UnidirectionalViewModel<ReviewViewState, ReviewViewAction, Unit>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val today = dateFormat.format(Date())
    private val _state = MutableStateFlow(ReviewViewState())
    override val state: StateFlow<ReviewViewState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            scheduledStepDao.observeByDate(today).collect { scheduledSteps ->
                val items = scheduledSteps.mapNotNull { ss ->
                    val step = stepDao.getById(ss.stepId) ?: return@mapNotNull null
                    val theme = themeDao.getById(step.themeId) ?: return@mapNotNull null
                    ReviewStepUi(
                        scheduledStepId = ss.id,
                        stepId = ss.stepId,
                        title = step.title,
                        starterAction = step.starterAction,
                        themeName = theme.title,
                        started = ss.started,
                        done = ss.done,
                        memo = ss.memo,
                        result = ss.result,
                    )
                }
                _state.update { current ->
                    current.copy(
                        items = items,
                        detailItem = current.detailItem?.let { detail ->
                            items.find { it.scheduledStepId == detail.scheduledStepId }
                        },
                    )
                }
            }
        }

        actions.onEach { action ->
            when (action) {
                is ReviewViewAction.ShowDetail -> _state.update { it.copy(detailItem = action.item) }
                is ReviewViewAction.HideDetail -> _state.update { it.copy(detailItem = null) }
                is ReviewViewAction.MarkResult -> {
                    scheduledStepDao.markResult(action.scheduledStepId, action.result)
                    if (action.result == "done") {
                        scheduledStepDao.markDone(
                            id = action.scheduledStepId,
                            memo = null,
                            actualEndedAt = System.currentTimeMillis(),
                        )
                    } else if (action.result == "started") {
                        scheduledStepDao.markStarted(
                            id = action.scheduledStepId,
                            memo = null,
                            actualStartedAt = System.currentTimeMillis(),
                        )
                    }
                }
                is ReviewViewAction.ArchiveStep -> {
                    stepDao.archive(id = action.stepId, archivedAt = System.currentTimeMillis())
                    _state.update { it.copy(detailItem = null) }
                }
                is ReviewViewAction.CarryOver -> {
                    val tomorrow = dateFormat.format(
                        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
                    )
                    scheduledStepDao.insert(
                        ScheduledStepEntity(
                            id = UUID.randomUUID().toString(),
                            stepId = action.stepId,
                            date = tomorrow,
                        )
                    )
                    _state.update { it.copy(detailItem = null) }
                }
            }
        }.launchIn(viewModelScope)
    }
}
