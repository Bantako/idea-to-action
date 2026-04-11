package org.mrlem.composesample.feature.today

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.data.ai.AiService
import org.mrlem.composesample.data.db.NodeEntity
import org.mrlem.composesample.data.db.NodeStatus
import org.mrlem.composesample.domain.NodeRepository
import javax.inject.Inject

data class TodayState(
    val activeNodes: List<NodeEntity> = emptyList(),
    val readyNodes: List<NodeEntity> = emptyList(),
    val aiRanked: Boolean = false,
)

sealed class TodayAction {
    data class MarkActive(val node: NodeEntity) : TodayAction()
    data class MarkDone(val node: NodeEntity) : TodayAction()
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: NodeRepository,
    private val aiService: AiService,
) : UnidirectionalViewModel<TodayState, TodayAction, Unit>() {

    private val _state = MutableStateFlow(TodayState())
    override val state: StateFlow<TodayState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeActionable().collect { (active, ready) ->
                _state.update { it.copy(activeNodes = active, readyNodes = ready, aiRanked = false) }
                if (aiService.isAvailable && ready.size > 1) {
                    val ranked = aiService.rankReadyNodes(ready)
                    _state.update { it.copy(readyNodes = ranked, aiRanked = true) }
                }
            }
        }
        viewModelScope.launch {
            actions.collect { action ->
                when (action) {
                    is TodayAction.MarkActive ->
                        repository.updateStatus(action.node, NodeStatus.ACTIVE)
                    is TodayAction.MarkDone ->
                        repository.updateStatus(action.node, NodeStatus.DONE)
                }
            }
        }
    }
}
