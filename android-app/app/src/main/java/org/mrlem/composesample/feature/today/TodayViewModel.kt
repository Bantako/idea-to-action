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
import org.mrlem.composesample.data.ai.AiService
import org.mrlem.composesample.data.db.NodeEntity
import org.mrlem.composesample.data.db.NodeStatus
import org.mrlem.composesample.data.db.ThemeEntity
import org.mrlem.composesample.domain.NodeRepository
import org.mrlem.composesample.domain.ThemeRepository
import javax.inject.Inject

data class TodayState(
    val activeNodes: List<NodeEntity> = emptyList(),
    // null キーは未整理（theme_id = null）グループ
    val readyByTheme: List<Pair<ThemeEntity?, List<NodeEntity>>> = emptyList(),
    val aiRanked: Boolean = false,
)

sealed class TodayAction {
    data class MarkActive(val node: NodeEntity) : TodayAction()
    data class MarkDone(val node: NodeEntity) : TodayAction()
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: NodeRepository,
    private val themeRepository: ThemeRepository,
    private val aiService: AiService,
) : UnidirectionalViewModel<TodayState, TodayAction, Unit>() {

    private val _state = MutableStateFlow(TodayState())
    override val state: StateFlow<TodayState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeActionable(),
                themeRepository.observeAll(),
            ) { (active, ready), themes ->
                active to Pair(ready, themes)
            }.collect { (active, readyAndThemes) ->
                val (ready, themes) = readyAndThemes
                val grouped = groupByTheme(ready, themes)
                _state.update { it.copy(activeNodes = active, readyByTheme = grouped, aiRanked = false) }

                if (aiService.isAvailable && ready.size > 1) {
                    val ranked = aiService.rankReadyNodes(ready)
                    val rankedGrouped = groupByTheme(ranked, themes)
                    _state.update { it.copy(readyByTheme = rankedGrouped, aiRanked = true) }
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

    private fun groupByTheme(
        nodes: List<NodeEntity>,
        themes: List<ThemeEntity>,
    ): List<Pair<ThemeEntity?, List<NodeEntity>>> {
        val grouped = nodes.groupBy { it.themeId }
        return buildList {
            // テーマ定義順にグループを追加
            themes.forEach { theme ->
                val themedNodes = grouped[theme.id]
                if (!themedNodes.isNullOrEmpty()) add(theme to themedNodes)
            }
            // 未整理ノードを末尾に追加
            val unthemed = grouped[null]
            if (!unthemed.isNullOrEmpty()) add(null to unthemed)
        }
    }
}
