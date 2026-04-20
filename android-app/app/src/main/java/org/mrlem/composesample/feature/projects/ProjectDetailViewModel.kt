package org.mrlem.composesample.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mrlem.composesample.data.db.MemoEntity
import org.mrlem.composesample.data.db.ProjectEntity
import org.mrlem.composesample.data.db.StepEntity
import org.mrlem.composesample.domain.MemoRepository
import org.mrlem.composesample.domain.ProjectRepository
import org.mrlem.composesample.domain.StepRepository
import org.mrlem.composesample.domain.UsageLogRepository
import javax.inject.Inject

data class ProjectDetailState(
    val project: ProjectEntity? = null,
    val steps: List<StepEntity> = emptyList(),
    val memos: List<MemoEntity> = emptyList(),
    val stepInput: String = "",
    val editingBackground: Boolean = false,
    val backgroundInput: String = "",
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val stepRepository: StepRepository,
    private val memoRepository: MemoRepository,
    private val usageLogRepository: UsageLogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectDetailState())
    val state: StateFlow<ProjectDetailState> = _state.asStateFlow()

    private var observeJob: Job? = null
    private var currentProjectId: Long = -1L

    fun initProject(projectId: Long) {
        if (projectId == currentProjectId) return
        currentProjectId = projectId
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                projectRepository.observeActive(),
                stepRepository.observeByProject(projectId),
                memoRepository.observeByProject(projectId),
            ) { projects, steps, memos ->
                Triple(projects.find { it.id == projectId }, steps, memos)
            }.collect { (project, steps, memos) ->
                _state.update { it.copy(project = project, steps = steps, memos = memos) }
            }
        }
    }

    fun onStepInputChanged(text: String) {
        _state.update { it.copy(stepInput = text) }
    }

    fun addStep() {
        val title = _state.value.stepInput.trim()
        if (title.isEmpty()) return
        _state.update { it.copy(stepInput = "") }
        viewModelScope.launch {
            stepRepository.create(currentProjectId, title)
            usageLogRepository.record("step_added")
        }
    }

    fun markStepDone(step: StepEntity) {
        viewModelScope.launch {
            stepRepository.markDone(step)
        }
    }

    fun deleteStep(step: StepEntity) {
        viewModelScope.launch {
            stepRepository.delete(step)
        }
    }

    fun startEditingBackground() {
        val current = _state.value.project?.background ?: ""
        _state.update { it.copy(editingBackground = true, backgroundInput = current) }
    }

    fun onBackgroundInputChanged(text: String) {
        _state.update { it.copy(backgroundInput = text) }
    }

    fun saveBackground() {
        val project = _state.value.project ?: return
        val background = _state.value.backgroundInput
        _state.update { it.copy(editingBackground = false) }
        viewModelScope.launch {
            projectRepository.updateBackground(project, background)
        }
    }

    fun cancelEditingBackground() {
        _state.update { it.copy(editingBackground = false, backgroundInput = "") }
    }

    fun setFocus() {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projectRepository.setFocus(project)
            usageLogRepository.record("project_focused")
        }
    }

    fun clearFocus() {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projectRepository.clearFocus(project)
        }
    }

    fun pause() {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projectRepository.pause(project)
        }
    }

    fun archive() {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projectRepository.archive(project)
        }
    }
}
