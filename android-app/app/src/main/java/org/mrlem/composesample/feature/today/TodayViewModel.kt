package org.mrlem.composesample.feature.today

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.data.ai.AiService
import org.mrlem.composesample.data.db.DailyLogEntity
import org.mrlem.composesample.data.db.ProjectEntity
import org.mrlem.composesample.data.db.StepEntity
import org.mrlem.composesample.data.db.StepStatus
import org.mrlem.composesample.domain.DailyLogRepository
import org.mrlem.composesample.domain.ProjectRepository
import org.mrlem.composesample.domain.StepRepository
import javax.inject.Inject

data class TodayState(
    val focusedProject: ProjectEntity? = null,
    val pendingSteps: List<StepEntity> = emptyList(),
    val adHocSteps: List<StepEntity> = emptyList(),
    val todayLogs: List<DailyLogEntity> = emptyList(),
    val manualInput: String = "",
)

sealed class TodayAction {
    data class MarkStepDone(val step: StepEntity) : TodayAction()
    data class AddManualLog(val what: String) : TodayAction()
    data class DeleteLog(val log: DailyLogEntity) : TodayAction()
    data class UpdateManualInput(val text: String) : TodayAction()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val stepRepository: StepRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val aiService: AiService,
) : UnidirectionalViewModel<TodayState, TodayAction, Unit>() {

    private val _state = MutableStateFlow(TodayState())
    override val state: StateFlow<TodayState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            projectRepository.observeFocused()
                .flatMapLatest { focused ->
                    val project = focused.firstOrNull()
                    if (project == null) {
                        combine(
                            flowOf(emptyList<StepEntity>()),
                            dailyLogRepository.observeToday(),
                        ) { steps, logs -> Triple(null as ProjectEntity?, steps, logs) }
                    } else {
                        combine(
                            stepRepository.observeByProject(project.id),
                            dailyLogRepository.observeToday(),
                        ) { steps, logs ->
                            Triple(project, steps.filter { it.status == StepStatus.PENDING }, logs)
                        }
                    }
                }
                .collect { (project, steps, logs) ->
                    _state.update { it.copy(
                        focusedProject = project,
                        pendingSteps = steps,
                        todayLogs = logs,
                    ) }
                    // AI によるステップ並べ替えは非同期で上書き
                    if (aiService.isAvailable && steps.size > 1) {
                        viewModelScope.launch {
                            val ranked = aiService.rankPendingSteps(steps)
                            _state.update { it.copy(pendingSteps = ranked) }
                        }
                    }
                }
        }
        viewModelScope.launch {
            stepRepository.observePendingNoProject().collect { adHocSteps ->
                _state.update { it.copy(adHocSteps = adHocSteps) }
            }
        }
        viewModelScope.launch {
            actions.collect { action ->
                when (action) {
                    is TodayAction.MarkStepDone -> {
                        stepRepository.markDone(action.step)
                        dailyLogRepository.createFromStep(action.step)
                    }
                    is TodayAction.AddManualLog -> {
                        if (action.what.isNotBlank()) {
                            dailyLogRepository.createManual(action.what)
                            _state.update { it.copy(manualInput = "") }
                        }
                    }
                    is TodayAction.DeleteLog -> {
                        dailyLogRepository.delete(action.log)
                    }
                    is TodayAction.UpdateManualInput -> {
                        _state.update { it.copy(manualInput = action.text) }
                    }

                }
            }
        }
    }
}
