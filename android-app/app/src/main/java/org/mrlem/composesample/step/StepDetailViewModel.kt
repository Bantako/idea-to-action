package org.mrlem.composesample.step

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel

@HiltViewModel(assistedFactory = StepDetailViewModel.Factory::class)
class StepDetailViewModel @AssistedInject constructor(
    private val stepDao: StepDao,
    @Assisted private val key: StepKey,
) : UnidirectionalViewModel<StepDetailViewState, StepDetailViewAction, StepDetailViewEffect>() {

    @AssistedFactory
    interface Factory {
        fun create(key: StepKey): StepDetailViewModel
    }

    override val state = stepDao.observeById(key.stepId)
        .filterNotNull()
        .map { step ->
            StepDetailViewState(
                title = step.title,
                starterAction = step.starterAction ?: "",
                weight = step.weight,
            )
        }
        .stateIn(viewModelScope, WhileSubscribed(), StepDetailViewState())

    init {
        actions
            .onEach { action ->
                when (action) {
                    is StepDetailViewAction.SaveTitle -> {
                        if (action.text.isNotBlank()) stepDao.update(
                            id = key.stepId,
                            title = action.text,
                            starterAction = state.value.starterAction.ifEmpty { null },
                            weight = state.value.weight,
                        )
                    }
                    is StepDetailViewAction.SaveStarterAction -> stepDao.update(
                        id = key.stepId,
                        title = state.value.title,
                        starterAction = action.text.ifEmpty { null },
                        weight = state.value.weight,
                    )
                    is StepDetailViewAction.SetWeight -> stepDao.update(
                        id = key.stepId,
                        title = state.value.title,
                        starterAction = state.value.starterAction.ifEmpty { null },
                        weight = action.weight,
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
