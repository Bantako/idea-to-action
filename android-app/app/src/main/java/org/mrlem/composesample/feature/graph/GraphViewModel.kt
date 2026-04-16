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
import org.mrlem.composesample.data.db.NodeEntity
import org.mrlem.composesample.data.db.NodeStatus
import org.mrlem.composesample.data.db.ThemeEntity
import org.mrlem.composesample.data.ai.AiService
import org.mrlem.composesample.domain.NodeRepository
import org.mrlem.composesample.domain.ThemeRepository
import javax.inject.Inject

data class NodeItem(
    val node: NodeEntity,
    val isConnected: Boolean = false,
)

data class GraphState(
    val themes: List<ThemeEntity> = emptyList(),
    val allItems: List<NodeItem> = emptyList(),
    val deferredItems: List<NodeItem> = emptyList(),
    val allNodes: List<NodeEntity> = emptyList(),
    val selectedNodeIds: Set<Long> = emptySet(),
    val showDeferred: Boolean = false,
    val showBulkAssign: Boolean = false,
    val assignThemeTarget: NodeEntity? = null,
    val editTarget: NodeEntity? = null,
    val deleteThemeTarget: ThemeEntity? = null,
    val showCreateTheme: Boolean = false,
    val themeSuggestions: List<String> = emptyList(),
) {
    val isSelectMode: Boolean get() = selectedNodeIds.isNotEmpty()

    fun themeFor(themeId: Long?): ThemeEntity? = themes.find { it.id == themeId }
}

sealed class GraphAction {
    data class ShowAssignTheme(val node: NodeEntity) : GraphAction()
    object DismissAssignTheme : GraphAction()
    data class AssignTheme(val themeId: Long?) : GraphAction()
    object ShowCreateTheme : GraphAction()
    object DismissCreateTheme : GraphAction()
    data class CreateTheme(val name: String) : GraphAction()
    data class ToggleSelect(val nodeId: Long) : GraphAction()
    object ClearSelection : GraphAction()
    object ShowBulkAssign : GraphAction()
    object DismissBulkAssign : GraphAction()
    data class BulkAssign(val themeId: Long) : GraphAction()
    data class ShowDeleteTheme(val theme: ThemeEntity) : GraphAction()
    object DismissDeleteTheme : GraphAction()
    object ConfirmDeleteTheme : GraphAction()
    data class ShowEdit(val node: NodeEntity) : GraphAction()
    object DismissEdit : GraphAction()
    data class SaveEdit(val title: String, val body: String) : GraphAction()
    object DeleteNode : GraphAction()
    object AbandonNode : GraphAction()
    object DeferNode : GraphAction()
    data class RestoreNode(val node: NodeEntity) : GraphAction()
    object ToggleDeferred : GraphAction()
}

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val themeRepository: ThemeRepository,
    private val aiService: AiService,
) : UnidirectionalViewModel<GraphState, GraphAction, Unit>() {

    private val _state = MutableStateFlow(GraphState())
    override val state: StateFlow<GraphState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                nodeRepository.observeAll(),
                themeRepository.observeAll(),
            ) { nodes, themes ->
                val activeStatuses = setOf(NodeStatus.IDEA, NodeStatus.READY, NodeStatus.ACTIVE, NodeStatus.DONE)
                val allItems = nodes
                    .filter { it.status in activeStatuses }
                    .map { NodeItem(node = it) }
                val deferredItems = nodes
                    .filter { it.status == NodeStatus.DEFERRED }
                    .map { NodeItem(node = it) }
                Triple(nodes, Pair(allItems, deferredItems), themes)
            }.collect { (allNodes, items, themes) ->
                val (allItems, deferredItems) = items
                _state.update { it.copy(allNodes = allNodes, allItems = allItems, deferredItems = deferredItems, themes = themes) }
            }
        }
        viewModelScope.launch {
            actions.collect { action ->
                when (action) {
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
                        _state.update { it.copy(showCreateTheme = true, themeSuggestions = emptyList()) }
                        val selectedTitles = _state.value.selectedNodeIds
                            .mapNotNull { id -> _state.value.allNodes.find { it.id == id }?.title }
                        if (selectedTitles.isNotEmpty()) {
                            viewModelScope.launch {
                                val suggestions = aiService.suggestThemeNames(selectedTitles)
                                _state.update { it.copy(themeSuggestions = suggestions) }
                            }
                        }
                    }
                    is GraphAction.DismissCreateTheme -> {
                        _state.update { it.copy(showCreateTheme = false, themeSuggestions = emptyList()) }
                    }
                    is GraphAction.CreateTheme -> {
                        if (action.name.isNotBlank()) {
                            val themeId = themeRepository.create(action.name)
                            val selectedIds = _state.value.selectedNodeIds.toList()
                            if (selectedIds.isNotEmpty()) {
                                themeRepository.assignNodes(selectedIds, themeId)
                            }
                        }
                        _state.update { it.copy(showCreateTheme = false, selectedNodeIds = emptySet()) }
                    }
                    is GraphAction.ToggleSelect -> {
                        _state.update { s ->
                            val ids = s.selectedNodeIds.toMutableSet()
                            if (action.nodeId in ids) ids.remove(action.nodeId) else ids.add(action.nodeId)
                            s.copy(selectedNodeIds = ids)
                        }
                    }
                    is GraphAction.ClearSelection -> {
                        _state.update { it.copy(selectedNodeIds = emptySet()) }
                    }
                    is GraphAction.ShowBulkAssign -> {
                        _state.update { it.copy(showBulkAssign = true) }
                    }
                    is GraphAction.DismissBulkAssign -> {
                        _state.update { it.copy(showBulkAssign = false) }
                    }
                    is GraphAction.BulkAssign -> {
                        val selectedIds = _state.value.selectedNodeIds.toList()
                        themeRepository.assignNodes(selectedIds, action.themeId)
                        _state.update { it.copy(showBulkAssign = false, selectedNodeIds = emptySet()) }
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
                    is GraphAction.DeferNode -> {
                        val target = _state.value.editTarget ?: return@collect
                        _state.update { it.copy(editTarget = null) }
                        nodeRepository.updateStatus(target, NodeStatus.DEFERRED)
                    }
                    is GraphAction.RestoreNode -> {
                        nodeRepository.updateStatus(action.node, NodeStatus.IDEA)
                    }
                    is GraphAction.ToggleDeferred -> {
                        _state.update { it.copy(showDeferred = !it.showDeferred) }
                    }
                }
            }
        }
    }
}
