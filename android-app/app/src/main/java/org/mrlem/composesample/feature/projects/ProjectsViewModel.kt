package org.mrlem.composesample.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mrlem.composesample.data.db.ProjectEntity
import org.mrlem.composesample.domain.ProjectRepository
import javax.inject.Inject

data class ProjectsState(
    val projects: List<ProjectEntity> = emptyList(),
    val showCreateDialog: Boolean = false,
    val createInput: String = "",
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsState())
    val state: StateFlow<ProjectsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            projectRepository.observeActive().collect { projects ->
                _state.update { it.copy(projects = projects) }
            }
        }
    }

    fun showCreateDialog() {
        _state.update { it.copy(showCreateDialog = true, createInput = "") }
    }

    fun dismissCreateDialog() {
        _state.update { it.copy(showCreateDialog = false, createInput = "") }
    }

    fun onCreateInputChanged(text: String) {
        _state.update { it.copy(createInput = text) }
    }

    fun createProject() {
        val title = _state.value.createInput.trim()
        if (title.isEmpty()) return
        _state.update { it.copy(showCreateDialog = false, createInput = "") }
        viewModelScope.launch {
            projectRepository.create(title)
        }
    }
}
