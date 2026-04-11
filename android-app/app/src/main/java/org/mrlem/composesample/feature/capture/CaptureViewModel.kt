package org.mrlem.composesample.feature.capture

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.data.db.NodeEntity
import org.mrlem.composesample.data.db.NodeStatus
import org.mrlem.composesample.domain.NodeRepository
import javax.inject.Inject

data class CaptureState(
    val input: String = "",
    val nodes: List<NodeEntity> = emptyList(),
)

sealed class CaptureAction {
    data class InputChanged(val text: String) : CaptureAction()
    object Submit : CaptureAction()
}

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val repository: NodeRepository,
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
                    is CaptureAction.Submit -> {
                        val title = _state.value.input.trim()
                        if (title.isNotEmpty()) {
                            repository.createNode(title)
                            _state.update { it.copy(input = "") }
                        }
                    }
                }
            }
        }
    }
}
