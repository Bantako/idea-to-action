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
import org.mrlem.composesample.data.db.ThemeEntity
import org.mrlem.composesample.domain.NodeRepository
import org.mrlem.composesample.domain.ThemeRepository
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
    val themes: List<ThemeEntity> = emptyList(),
    val allItems: List<NodeItem> = emptyList(),
    val allNodes: List<NodeEntity> = emptyList(),
    val expandedThemeIds: Set<Long> = emptySet(),
    val addEdgeTarget: NodeEntity? = null,
    val assignThemeTarget: NodeEntity? = null,
    val editTarget: NodeEntity? = null,
    val deleteThemeTarget: ThemeEntity? = null,
    val showCreateTheme: Boolean = false,
) {
    val unorganizedItems: List<NodeItem>
        get() = allItems.filter { it.node.themeId == null }

    fun itemsForTheme(themeId: Long): List<NodeItem> =
        allItems.filter { it.node.themeId == themeId }

    fun isThemeExpanded(themeId: Long): Boolean = themeId in expandedThemeIds
}

sealed class GraphAction {
    data class ShowAddEdge(val node: NodeEntity) : GraphAction()
    object DismissAddEdge : GraphAction()
    data class AddEdge(val fromId: Long) : GraphAction()
    data class RemoveEdge(val edge: EdgeEntity) : GraphAction()
    data class ToggleTheme(val themeId: Long) : GraphAction()
    data class ShowAssignTheme(val node: NodeEntity) : GraphAction()
    object DismissAssignTheme : GraphAction()
    data class AssignTheme(val themeId: Long?) : GraphAction()
    object ShowCreateTheme : GraphAction()
    object DismissCreateTheme : GraphAction()
    data class CreateTheme(val name: String) : GraphAction()
    data class ShowDeleteTheme(val theme: ThemeEntity) : GraphAction()
    object DismissDeleteTheme : GraphAction()
    object ConfirmDeleteTheme : GraphAction()
    data class ShowEdit(val node: NodeEntity) : GraphAction()
    object DismissEdit : GraphAction()
    data class SaveEdit(val title: String, val body: String) : GraphAction()
    object DeleteNode : GraphAction()
    object AbandonNode : GraphAction()
}

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val themeRepository: ThemeRepository,
) : UnidirectionalViewModel<GraphState, GraphAction, Unit>() {

    private val _state = MutableStateFlow(GraphState())
    override val state: StateFlow<GraphState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                nodeRepository.observeAll(),
                nodeRepository.observeEdges(),
                themeRepository.observeAll(),
            ) { nodes, edges, themes ->
                val nodeMap = nodes.associateBy { it.id }
                val incomingPrereqsByNode = edges
                    .filter { it.type == EdgeType.PREREQUISITE }
                    .groupBy { it.toId }

                val items = nodes.filter { it.status != NodeStatus.ABANDONED }.map { node ->
                    NodeItem(
                        node = node,
                        prereqs = incomingPrereqsByNode[node.id]
                            ?.mapNotNull { edge -> nodeMap[edge.fromId]?.let { Prereq(edge, it) } }
                            ?: emptyList(),
                    )
                }
                Triple(nodes, items, themes)
            }.collect { (allNodes, allItems, themes) ->
                _state.update { it.copy(allNodes = allNodes, allItems = allItems, themes = themes) }
            }
        }
        viewModelScope.launch {
            actions.collect { action ->
                when (action) {
                    is GraphAction.ShowAddEdge -> {
                        _state.update { it.copy(addEdgeTarget = action.node) }
                    }
                    is GraphAction.DismissAddEdge -> {
                        _state.update { it.copy(addEdgeTarget = null) }
                    }
                    is GraphAction.AddEdge -> {
                        val target = _state.value.addEdgeTarget ?: return@collect
                        nodeRepository.addEdge(fromId = action.fromId, toId = target.id)
                        _state.update { it.copy(addEdgeTarget = null) }
                    }
                    is GraphAction.RemoveEdge -> {
                        nodeRepository.removeEdge(action.edge)
                    }
                    is GraphAction.ToggleTheme -> {
                        _state.update { s ->
                            val ids = s.expandedThemeIds.toMutableSet()
                            if (action.themeId in ids) ids.remove(action.themeId) else ids.add(action.themeId)
                            s.copy(expandedThemeIds = ids)
                        }
                    }
                    is GraphAction.ShowAssignTheme -> {
                        _state.update { it.copy(assignThemeTarget = action.node) }
                    }
                    is GraphAction.DismissAssignTheme -> {
                        _state.update { it.copy(assignThemeTarget = null) }
                    }
                    is GraphAction.AssignTheme -> {
                        val target = _state.value.assignThemeTarget ?: return@collect
                        themeRepository.assignNode(target.id, action.themeId)
                        _state.update { it.copy(assignThemeTarget = null) }
                    }
                    is GraphAction.ShowCreateTheme -> {
                        _state.update { it.copy(showCreateTheme = true) }
                    }
                    is GraphAction.DismissCreateTheme -> {
                        _state.update { it.copy(showCreateTheme = false) }
                    }
                    is GraphAction.CreateTheme -> {
                        if (action.name.isNotBlank()) {
                            themeRepository.create(action.name)
                        }
                        _state.update { it.copy(showCreateTheme = false) }
                    }
                    is GraphAction.ShowDeleteTheme -> {
                        _state.update { it.copy(deleteThemeTarget = action.theme) }
                    }
                    is GraphAction.DismissDeleteTheme -> {
                        _state.update { it.copy(deleteThemeTarget = null) }
                    }
                    is GraphAction.ConfirmDeleteTheme -> {
                        val target = _state.value.deleteThemeTarget ?: return@collect
                        _state.update { it.copy(deleteThemeTarget = null) }
                        themeRepository.delete(target)
                    }
                    is GraphAction.ShowEdit -> {
                        _state.update { it.copy(editTarget = action.node) }
                    }
                    is GraphAction.DismissEdit -> {
                        _state.update { it.copy(editTarget = null) }
                    }
                    is GraphAction.SaveEdit -> {
                        val target = _state.value.editTarget ?: return@collect
                        if (action.title.isNotBlank()) {
                            nodeRepository.updateNode(target, action.title, action.body)
                        }
                        _state.update { it.copy(editTarget = null) }
                    }
                    is GraphAction.DeleteNode -> {
                        val target = _state.value.editTarget ?: return@collect
                        _state.update { it.copy(editTarget = null) }
                        nodeRepository.deleteNode(target)
                    }
                    is GraphAction.AbandonNode -> {
                        val target = _state.value.editTarget ?: return@collect
                        _state.update { it.copy(editTarget = null) }
                        nodeRepository.updateStatus(target, NodeStatus.ABANDONED)
                    }
                }
            }
        }
    }
}
