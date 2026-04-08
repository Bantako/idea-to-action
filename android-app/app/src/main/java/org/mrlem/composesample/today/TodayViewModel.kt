package org.mrlem.composesample.today

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.step.StepDao
import org.mrlem.composesample.theme.ThemeDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val scheduledStepDao: ScheduledStepDao,
    private val stepDao: StepDao,
    private val themeDao: ThemeDao,
) : UnidirectionalViewModel<TodayViewState, TodayViewAction, Unit>() {

    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private val _selectedDate = MutableStateFlow(today)
    private val _state = MutableStateFlow(TodayViewState(selectedDate = today))
    override val state: StateFlow<TodayViewState> = _state.asStateFlow()

    init {
        _selectedDate
            .flatMapLatest { date -> scheduledStepDao.observeByDate(date) }
            .onEach { scheduledSteps ->
                val items = scheduledSteps.mapNotNull { ss ->
                    val step = stepDao.getById(ss.stepId) ?: return@mapNotNull null
                    val theme = themeDao.getById(step.themeId) ?: return@mapNotNull null
                    ScheduledStepUi(
                        scheduledStepId = ss.id,
                        stepId = ss.stepId,
                        title = step.title,
                        starterAction = step.starterAction,
                        themeName = theme.title,
                        themeGoal = theme.goal,
                        startTime = ss.startTime,
                        durationMinutes = ss.durationMinutes,
                        started = ss.started,
                        done = ss.done,
                        memo = ss.memo,
                    )
                }
                _state.update { current ->
                    current.copy(
                        timedItems = items.filter { it.startTime != null }.sortedBy { it.startTime },
                        untimedItems = items.filter { it.startTime == null },
                        detailItem = current.detailItem?.let { detail ->
                            items.find { it.scheduledStepId == detail.scheduledStepId }
                        },
                    )
                }
            }
            .launchIn(viewModelScope)

        actions.onEach { action ->
            when (action) {
                is TodayViewAction.ShowDetail -> _state.update { it.copy(detailItem = action.item) }
                is TodayViewAction.HideDetail -> _state.update { it.copy(detailItem = null) }
                is TodayViewAction.MarkStarted -> {
                    scheduledStepDao.markStarted(
                        id = action.scheduledStepId,
                        memo = action.memo,
                        actualStartedAt = System.currentTimeMillis(),
                    )
                    _state.update { it.copy(detailItem = null) }
                }
                is TodayViewAction.SwitchDate -> {
                    _selectedDate.value = action.date
                    _state.update { it.copy(selectedDate = action.date) }
                }
            }
        }.launchIn(viewModelScope)
    }
}
