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
import javax.inject.Inject

data class ProjectDetailState(
    val project: ProjectEntity? = null,
    val steps: List<StepEntity> = emptyList(),
    val memos: List<MemoEntity> = emptyList(),
    val stepInput: String = "",
    val editingGoal: Boolean = false,
    val goalInput: String = "",
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val stepRepository: StepRepository,
    private val memoRepository: MemoRepository,
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

    fun startEditingGoal() {
        val current = _state.value.project?.goal ?: ""
        _state.update { it.copy(editingGoal = true, goalInput = current) }
    }

    fun onGoalInputChanged(text: String) {
        _state.update { it.copy(goalInput = text) }
    }

    fun saveGoal() {
        val project = _state.value.project ?: return
        val goal = _state.value.goalInput
        _state.update { it.copy(editingGoal = false) }
        viewModelScope.launch {
            projectRepository.updateGoal(project, goal)
        }
    }

    fun cancelEditingGoal() {
        _state.update { it.copy(editingGoal = false, goalInput = "") }
    }

    fun setFocus() {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projectRepository.setFocus(project)
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
