package org.mrlem.composesample.feature.capture

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
import org.mrlem.composesample.domain.NodeRepository
import javax.inject.Inject

data class AiActivityEntry(
    val newNodeTitle: String,
    val relatedNodeTitle: String,
)

data class CaptureState(
    val input: String = "",
    val nodes: List<NodeEntity> = emptyList(),
    val aiLog: List<AiActivityEntry> = emptyList(),
    val editTarget: NodeEntity? = null,
)

sealed class CaptureAction {
    data class InputChanged(val text: String) : CaptureAction()
    object Submit : CaptureAction()
    data class ShowEdit(val node: NodeEntity) : CaptureAction()
    object DismissEdit : CaptureAction()
    data class SaveEdit(val title: String, val body: String) : CaptureAction()
    object DeleteNode : CaptureAction()
}

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val repository: NodeRepository,
    private val aiService: AiService,
) : UnidirectionalViewModel<CaptureState, CaptureAction, Unit>() {

    private val _state = MutableStateFlow(CaptureState())
    override val state: StateFlow<CaptureState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeUnorganized().collect { nodes ->
                _state.update { it.copy(nodes = nodes) }
            }
        }
        viewModelScope.launch {
            actions.collect { action ->
                when (action) {
                    is CaptureAction.InputChanged -> _state.update { it.copy(input = action.text) }
                    is CaptureAction.Submit -> handleSubmit()
                    is CaptureAction.ShowEdit -> _state.update { it.copy(editTarget = action.node) }
                    is CaptureAction.DismissEdit -> _state.update { it.copy(editTarget = null) }
                    is CaptureAction.SaveEdit -> {
                        val target = _state.value.editTarget ?: return@collect
                        if (action.title.isNotBlank()) {
                            repository.updateNode(target, action.title, action.body)
                        }
                        _state.update { it.copy(editTarget = null) }
                    }
                    is CaptureAction.DeleteNode -> {
                        val target = _state.value.editTarget ?: return@collect
                        _state.update { it.copy(editTarget = null) }
                        repository.deleteNode(target)
                    }
                }
            }
        }
    }

    private suspend fun handleSubmit() {
        val title = _state.value.input.trim()
        if (title.isEmpty()) return

        val existingNodes = _state.value.nodes
        val newNodeId = repository.createNode(title)
        _state.update { it.copy(input = "") }

        if (aiService.isAvailable && existingNodes.isNotEmpty()) {
            val relatedIds = aiService.suggestRelatedNodeIds(title, existingNodes)
            val newEntries = relatedIds.mapNotNull { id ->
                existingNodes.find { it.id == id }?.let { related ->
                    repository.addEdge(fromId = related.id, toId = newNodeId)
                    AiActivityEntry(newNodeTitle = title, relatedNodeTitle = related.title)
                }
            }
            if (newEntries.isNotEmpty()) {
                _state.update { it.copy(aiLog = newEntries + it.aiLog) }
            }
        }
    }
}
