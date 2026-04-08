package org.mrlem.composesample.theme

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.step.StepDao
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ThemesViewModel @Inject constructor(
    private val themeDao: ThemeDao,
    private val stepDao: StepDao,
) : UnidirectionalViewModel<ThemesViewState, ThemesViewAction, Unit>() {

    private val inputFlow = MutableStateFlow("")
    private val inputWeightFlow = MutableStateFlow("medium")
    private val showArchivedFlow = MutableStateFlow(false)

    override val state = combine(
        themeDao.observeActive(),
        themeDao.observeArchived(),
        inputFlow,
        inputWeightFlow,
        showArchivedFlow,
    ) { active, archived, input, inputWeight, showArchived ->
        val activeItems = active.map { theme ->
            ThemeItem(
                id = theme.id,
                title = theme.title,
                weight = theme.weight,
                pendingStepCount = 0,
            )
        }
        val archivedItems = archived.map { theme ->
            ThemeItem(
                id = theme.id,
                title = theme.title,
                weight = theme.weight,
                pendingStepCount = 0,
            )
        }
        ThemesViewState(
            themes = activeItems,
            archivedThemes = archivedItems,
            showArchived = showArchived,
            input = input,
            inputWeight = inputWeight,
        )
    }.stateIn(viewModelScope, WhileSubscribed(), ThemesViewState())

    init {
        actions
            .onEach { action ->
                when (action) {
                    is ThemesViewAction.InputChange -> inputFlow.value = action.text
                    is ThemesViewAction.InputWeightChange -> inputWeightFlow.value = action.weight
                    is ThemesViewAction.AddTheme -> {
                        val text = inputFlow.value.trim()
                        if (text.isNotEmpty()) {
                            themeDao.insert(
                                ThemeEntity(
                                    id = UUID.randomUUID().toString(),
                                    title = text,
                                    weight = inputWeightFlow.value,
                                    createdAt = System.currentTimeMillis(),
                                )
                            )
                            inputFlow.value = ""
                            inputWeightFlow.value = "medium"
                        }
                    }
                    is ThemesViewAction.ToggleShowArchived -> showArchivedFlow.update { !it }
                }
            }
            .launchIn(viewModelScope)
    }
}
