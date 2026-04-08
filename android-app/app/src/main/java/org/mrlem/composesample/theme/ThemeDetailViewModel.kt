package org.mrlem.composesample.theme

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.step.StepDao
import org.mrlem.composesample.step.StepEntity
import org.mrlem.composesample.today.NotificationScheduler
import org.mrlem.composesample.today.ScheduledStepDao
import org.mrlem.composesample.today.ScheduledStepEntity
import java.util.UUID
import javax.inject.Inject

@HiltViewModel(assistedFactory = ThemeDetailViewModel.Factory::class)
class ThemeDetailViewModel @AssistedInject constructor(
    private val themeDao: ThemeDao,
    private val stepDao: StepDao,
    private val scheduledStepDao: ScheduledStepDao,
    private val notificationScheduler: NotificationScheduler,
    @Assisted private val key: ThemeKey,
) : UnidirectionalViewModel<ThemeDetailViewState, ThemeDetailViewAction, ThemeDetailViewEffect>() {

    @AssistedFactory
    interface Factory {
        fun create(key: ThemeKey): ThemeDetailViewModel
    }

    override val state = combine(
        themeDao.observeById(key.themeId).filterNotNull(),
        stepDao.observeByTheme(key.themeId),
        scheduledStepDao.observeByTheme(key.themeId),
    ) { theme, steps, scheduledSteps ->
        val stepTitleMap = steps.associate { it.id to it.title }
        val logEntries = scheduledSteps.mapNotNull { ss ->
            val result = ss.result ?: return@mapNotNull null
            ActivityLogEntry(
                date = ss.date,
                stepTitle = stepTitleMap[ss.stepId] ?: return@mapNotNull null,
                result = result,
                memo = ss.memo,
            )
        }
        ThemeDetailViewState(
            title = theme.title,
            goal = theme.goal ?: "",
            weight = theme.weight,
            steps = steps,
            activityLog = logEntries,
        )
    }.stateIn(viewModelScope, WhileSubscribed(), ThemeDetailViewState())

    init {
        actions
            .onEach { action ->
                when (action) {
                    is ThemeDetailViewAction.SaveGoal -> themeDao.updateGoal(key.themeId, action.text)
                    is ThemeDetailViewAction.SaveTitle -> {
                        if (action.text.isNotBlank()) themeDao.updateTitle(key.themeId, action.text)
                    }
                    is ThemeDetailViewAction.SetWeight -> themeDao.updateWeight(key.themeId, action.weight)
                    is ThemeDetailViewAction.Archive -> {
                        themeDao.archive(key.themeId, System.currentTimeMillis())
                        trigger(ThemeDetailViewEffect.NavigateBack)
                    }
                    is ThemeDetailViewAction.AddStep -> {
                        val text = action.title.trim()
                        if (text.isNotEmpty()) {
                            stepDao.insert(
                                StepEntity(
                                    id = UUID.randomUUID().toString(),
                                    themeId = key.themeId,
                                    title = text,
                                    createdAt = System.currentTimeMillis(),
                                )
                            )
                        }
                    }
                    is ThemeDetailViewAction.ArchiveStep -> {
                        stepDao.archive(action.stepId, System.currentTimeMillis())
                    }
                    is ThemeDetailViewAction.NavigateToStep -> {
                        trigger(ThemeDetailViewEffect.NavigateToStep(action.stepId))
                    }
                    is ThemeDetailViewAction.ScheduleStep -> {
                        val step = stepDao.getById(action.stepId)
                        val entity = ScheduledStepEntity(
                            id = UUID.randomUUID().toString(),
                            stepId = action.stepId,
                            date = action.date,
                            startTime = action.startTime,
                            durationMinutes = action.durationMinutes,
                            notificationEnabled = action.notificationEnabled,
                        )
                        scheduledStepDao.insert(entity)
                        stepDao.markToday(action.stepId)
                        if (action.notificationEnabled && step != null) {
                            notificationScheduler.schedule(entity, step.title)
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
