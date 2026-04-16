package org.mrlem.composesample.feature.capture

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
import org.mrlem.composesample.data.db.MemoEntity
import org.mrlem.composesample.data.db.ProjectEntity
import org.mrlem.composesample.domain.MemoRepository
import org.mrlem.composesample.domain.ProjectRepository
import javax.inject.Inject

data class CaptureState(
    val input: String = "",
    val memos: List<MemoEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val aiSuggestedProjectIds: List<Long> = emptyList(),
    val linkTarget: MemoEntity? = null,
)

sealed class CaptureAction {
    data class InputChanged(val text: String) : CaptureAction()
    object Submit : CaptureAction()
    data class ShowLinkSheet(val memo: MemoEntity) : CaptureAction()
    object DismissLinkSheet : CaptureAction()
    data class LinkToProject(val projectId: Long) : CaptureAction()
    data class DeleteMemo(val memo: MemoEntity) : CaptureAction()
}

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val projectRepository: ProjectRepository,
    private val aiService: AiService,
) : UnidirectionalViewModel<CaptureState, CaptureAction, Unit>() {

    private val _state = MutableStateFlow(CaptureState())
    override val state: StateFlow<CaptureState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                memoRepository.observeUnorganized(),
                projectRepository.observeActive(),
            ) { memos, projects -> Pair(memos, projects) }
                .collect { (memos, projects) ->
                    _state.update { it.copy(memos = memos, projects = projects) }
                }
        }
        viewModelScope.launch {
            actions.collect { action ->
                when (action) {
                    is CaptureAction.InputChanged -> _state.update { it.copy(input = action.text) }
                    is CaptureAction.Submit -> handleSubmit()
                    is CaptureAction.ShowLinkSheet -> {
                        _state.update { it.copy(linkTarget = action.memo, aiSuggestedProjectIds = emptyList()) }
                    }
                    is CaptureAction.DismissLinkSheet -> _state.update { it.copy(linkTarget = null) }
                    is CaptureAction.LinkToProject -> {
                        val target = _state.value.linkTarget ?: return@collect
                        _state.update { it.copy(linkTarget = null) }
                        memoRepository.linkToProject(target, action.projectId)
                    }
                    is CaptureAction.DeleteMemo -> {
                        _state.update { it.copy(linkTarget = null) }
                        memoRepository.delete(action.memo)
                    }
                }
            }
        }
    }

    private suspend fun handleSubmit() {
        val text = _state.value.input.trim()
        if (text.isEmpty()) return
        _state.update { it.copy(input = "") }
        memoRepository.create(text)
        if (aiService.isAvailable) {
            val projects = _state.value.projects
            if (projects.isNotEmpty()) {
                viewModelScope.launch {
                    val ids = aiService.suggestRelatedProjectIds(text, projects)
                    _state.update { it.copy(aiSuggestedProjectIds = ids) }
                }
            }
        }
    }
}
