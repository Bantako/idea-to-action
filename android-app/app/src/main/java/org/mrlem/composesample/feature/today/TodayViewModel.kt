package org.mrlem.composesample.feature.today

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.data.db.NodeEntity
import org.mrlem.composesample.data.db.NodeStatus
import org.mrlem.composesample.data.db.ThemeEntity
import org.mrlem.composesample.domain.NodeRepository
import org.mrlem.composesample.domain.ThemeRepository
import javax.inject.Inject

data class TodayState(
    val activeNodes: List<NodeEntity> = emptyList(),
    val readyByTheme: List<Pair<ThemeEntity?, List<NodeEntity>>> = emptyList(),
)

sealed class TodayAction {
    data class MarkActive(val node: NodeEntity) : TodayAction()
    data class MarkDone(val node: NodeEntity) : TodayAction()
    data class MarkAbandoned(val node: NodeEntity) : TodayAction()
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: NodeRepository,
    private val themeRepository: ThemeRepository,
) : UnidirectionalViewModel<TodayState, TodayAction, Unit>() {

    private val _state = MutableStateFlow(TodayState())
    override val state: StateFlow<TodayState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeAll(),
                themeRepository.observeAll(),
            ) { nodes, themes ->
                val active = nodes.filter { it.status == NodeStatus.ACTIVE }
                val ready = nodes.filter { it.status == NodeStatus.READY }
                active to groupByTheme(ready, themes)
            }.collect { (active, grouped) ->
                _state.update { it.copy(activeNodes = active, readyByTheme = grouped) }
            }
        }
        viewModelScope.launch {
            actions.collect { action ->
                when (action) {
                    is TodayAction.MarkActive ->
                        repository.updateStatus(action.node, NodeStatus.ACTIVE)
                    is TodayAction.MarkDone ->
                        repository.updateStatus(action.node, NodeStatus.DONE)
                    is TodayAction.MarkAbandoned ->
                        repository.updateStatus(action.node, NodeStatus.ABANDONED)
                }
            }
        }
    }

    private fun groupByTheme(
        nodes: List<NodeEntity>,
        themes: List<ThemeEntity>,
    ): List<Pair<ThemeEntity?, List<NodeEntity>>> {
        val grouped = nodes.groupBy { it.themeId }
        return buildList {
            themes.forEach { theme ->
                val themedNodes = grouped[theme.id]
                if (!themedNodes.isNullOrEmpty()) add(theme to themedNodes)
            }
            val unthemed = grouped[null]
            if (!unthemed.isNullOrEmpty()) add(null to unthemed)
        }
    }
}
