package org.mrlem.composesample.feature.graph

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.data.db.EdgeEntity
import org.mrlem.composesample.data.db.EdgeType
import org.mrlem.composesample.data.db.NodeEntity
import org.mrlem.composesample.data.db.NodeStatus
import org.mrlem.composesample.domain.NodeRepository
import javax.inject.Inject

data class Prereq(
    val edge: EdgeEntity,
    val fromNode: NodeEntity,
)

data class NodeItem(
    val node: NodeEntity,
    val prereqs: List<Prereq>,
)

data class GraphState(
    val allItems: List<NodeItem> = emptyList(),
    val allNodes: List<NodeEntity> = emptyList(),
    val statusFilter: NodeStatus? = null,
    val addEdgeTarget: NodeEntity? = null,  // ノード: 前提条件を追加する対象
) {
    val displayItems: List<NodeItem>
        get() = if (statusFilter == null) allItems
                else allItems.filter { it.node.status == statusFilter }
}

sealed class GraphAction {
    data class FilterChanged(val status: NodeStatus?) : GraphAction()
    data class ShowAddEdge(val node: NodeEntity) : GraphAction()
    object DismissAddEdge : GraphAction()
    data class AddEdge(val fromId: Long) : GraphAction()
    data class RemoveEdge(val edge: EdgeEntity) : GraphAction()
}

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val repository: NodeRepository,
) : UnidirectionalViewModel<GraphState, GraphAction, Unit>() {

    private val _state = MutableStateFlow(GraphState())
    override val state: StateFlow<GraphState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeAll(),
                repository.observeEdges(),
            ) { nodes, edges ->
                val nodeMap = nodes.associateBy { it.id }
                val incomingPrereqsByNode = edges
                    .filter { it.type == EdgeType.PREREQUISITE }
                    .groupBy { it.toId }

                val items = nodes.map { node ->
                    NodeItem(
                        node = node,
                        prereqs = incomingPrereqsByNode[node.id]
                            ?.mapNotNull { edge -> nodeMap[edge.fromId]?.let { Prereq(edge, it) } }
                            ?: emptyList(),
                    )
                }
                nodes to items
            }.collect { (allNodes, allItems) ->
                _state.update { it.copy(allNodes = allNodes, allItems = allItems) }
            }
        }
        viewModelScope.launch {
            actions.collect { action ->
                when (action) {
                    is GraphAction.FilterChanged -> {
                        _state.update { it.copy(statusFilter = action.status) }
                    }
                    is GraphAction.ShowAddEdge -> {
                        _state.update { it.copy(addEdgeTarget = action.node) }
                    }
                    is GraphAction.DismissAddEdge -> {
                        _state.update { it.copy(addEdgeTarget = null) }
                    }
                    is GraphAction.AddEdge -> {
                        val target = _state.value.addEdgeTarget ?: return@collect
                        repository.addEdge(fromId = action.fromId, toId = target.id)
                        _state.update { it.copy(addEdgeTarget = null) }
                    }
                    is GraphAction.RemoveEdge -> {
                        repository.removeEdge(action.edge)
                    }
                }
            }
        }
    }
}
