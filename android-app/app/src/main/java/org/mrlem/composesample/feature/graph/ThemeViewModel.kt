package org.mrlem.composesample.feature.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mrlem.composesample.data.db.ThemeEntity
import org.mrlem.composesample.domain.ThemeRepository
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val themes = themeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createTheme(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { themeRepository.create(name) }
    }

    fun renameTheme(theme: ThemeEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { themeRepository.rename(theme, newName) }
    }

    fun deleteTheme(theme: ThemeEntity) {
        viewModelScope.launch { themeRepository.delete(theme) }
    }

    fun assignNode(nodeId: Long, themeId: Long?) {
        viewModelScope.launch { themeRepository.assignNode(nodeId, themeId) }
    }
}
