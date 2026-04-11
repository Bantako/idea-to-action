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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
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
    private val _state = MutableStateFlow(
        TodayViewState(
            selectedDate = today,
            isReviewMode = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 19,
        )
    )
    override val state: StateFlow<TodayViewState> = _state.asStateFlow()

    private var autoSuggestChecked = false

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
                        started = ss.started,
                        done = ss.done,
                        memo = ss.memo,
                    )
                }
                val shouldAutoSuggest = !autoSuggestChecked &&
                    _selectedDate.value == today &&
                    items.isEmpty()
                if (!autoSuggestChecked) autoSuggestChecked = true
                _state.update { current ->
                    current.copy(
                        items = items,
                        detailItem = current.detailItem?.let { detail ->
                            items.find { it.scheduledStepId == detail.scheduledStepId }
                        },
                        autoSuggestPending = if (shouldAutoSuggest) true else current.autoSuggestPending,
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
                is TodayViewAction.AutoSuggestHandled -> _state.update { it.copy(autoSuggestPending = false) }
                is TodayViewAction.ShowAddSheet -> {
                    val scheduledToday = scheduledStepDao.getByDate(_selectedDate.value)
                    val scheduledStepIds = scheduledToday.map { it.stepId }.toSet()
                    val themes = themeDao.getActive()
                    val steps = mutableListOf<StepPickerUi>()
                    for (theme in themes) {
                        val themeSteps = stepDao.getActiveByTheme(theme.id)
                        themeSteps.filter { it.id !in scheduledStepIds }.forEach { step ->
                            steps.add(StepPickerUi(
                                stepId = step.id,
                                stepTitle = step.title,
                                themeName = theme.title,
                            ))
                        }
                    }
                    _state.update { it.copy(showAddSheet = true, availableSteps = steps) }
                }
                is TodayViewAction.HideAddSheet -> _state.update { it.copy(showAddSheet = false, availableSteps = emptyList()) }
                is TodayViewAction.AddStepToToday -> {
                    val maxOrder = scheduledStepDao.getMaxSortOrder(_selectedDate.value) ?: -1
                    scheduledStepDao.insert(
                        ScheduledStepEntity(
                            id = UUID.randomUUID().toString(),
                            stepId = action.stepId,
                            date = _selectedDate.value,
                            sortOrder = maxOrder + 1,
                        )
                    )
                    _state.update { current ->
                        current.copy(availableSteps = current.availableSteps.filter { it.stepId != action.stepId })
                    }
                }
                is TodayViewAction.MoveStep -> {
                    val items = _state.value.items
                    val index = items.indexOfFirst { it.scheduledStepId == action.scheduledStepId }
                    val swapIndex = index + action.direction
                    if (index < 0 || swapIndex < 0 || swapIndex >= items.size) return@onEach
                    val allEntities = scheduledStepDao.getByDate(_selectedDate.value)
                    val entityA = allEntities.find { it.id == items[index].scheduledStepId } ?: return@onEach
                    val entityB = allEntities.find { it.id == items[swapIndex].scheduledStepId } ?: return@onEach
                    scheduledStepDao.updateSortOrder(entityA.id, entityB.sortOrder)
                    scheduledStepDao.updateSortOrder(entityB.id, entityA.sortOrder)
                }
            }
        }.launchIn(viewModelScope)
    }
}
