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
import org.mrlem.composesample.data.db.NodeStatus
import org.mrlem.composesample.domain.NodeRepository
import javax.inject.Inject

data class NodeSuggestion(
    val newNodeId: Long,
    val relatedNode: NodeEntity,
)

data class CaptureState(
    val input: String = "",
    val nodes: List<NodeEntity> = emptyList(),
    val suggestions: List<NodeSuggestion> = emptyList(),
    val isLoadingAi: Boolean = false,
)

sealed class CaptureAction {
    data class InputChanged(val text: String) : CaptureAction()
    object Submit : CaptureAction()
    data class AcceptSuggestion(val suggestion: NodeSuggestion) : CaptureAction()
    data class DismissSuggestion(val suggestion: NodeSuggestion) : CaptureAction()
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
            repository.observeByStatus(NodeStatus.IDEA).collect { nodes ->
                _state.update { it.copy(nodes = nodes) }
            }
        }
        viewModelScope.launch {
            actions.collect { action ->
                when (action) {
                    is CaptureAction.InputChanged -> _state.update { it.copy(input = action.text) }
                    is CaptureAction.Submit -> handleSubmit()
                    is CaptureAction.AcceptSuggestion -> {
                        repository.addEdge(
                            fromId = action.suggestion.relatedNode.id,
                            toId = action.suggestion.newNodeId,
                        )
                        _state.update { it.copy(suggestions = it.suggestions - action.suggestion) }
                    }
                    is CaptureAction.DismissSuggestion ->
                        _state.update { it.copy(suggestions = it.suggestions - action.suggestion) }
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
            _state.update { it.copy(isLoadingAi = true) }
            val relatedIds = aiService.suggestRelatedNodeIds(title, existingNodes)
            val newSuggestions = relatedIds.mapNotNull { id ->
                existingNodes.find { it.id == id }?.let { NodeSuggestion(newNodeId, it) }
            }
            _state.update {
                it.copy(
                    suggestions = it.suggestions + newSuggestions,
                    isLoadingAi = false,
                )
            }
        }
    }
}
