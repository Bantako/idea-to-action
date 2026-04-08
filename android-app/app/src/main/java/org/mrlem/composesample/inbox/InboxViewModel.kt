package org.mrlem.composesample.inbox

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val dao: InboxDao,
) : UnidirectionalViewModel<InboxViewState, InboxViewAction, InboxViewEffect>() {

    private val selectedIds = MutableStateFlow(emptySet<String>())
    private val isSelectMode = MutableStateFlow(false)

    override val state = combine(
        dao.observeActive(),
        selectedIds,
        isSelectMode,
    ) { entities, selected, selectMode ->
        InboxViewState(
            entries = entities.map { InboxEntry(it.id, it.text, it.createdAt) },
            selectedIds = selected,
            isSelectMode = selectMode,
        )
    }.stateIn(viewModelScope, WhileSubscribed(), InboxViewState())

    init {
        actions
            .onEach { action ->
                when (action) {
                    is InboxViewAction.AddEntry -> {
                        val text = action.text.trim()
                        if (text.isNotEmpty()) {
                            dao.insert(
                                InboxEntryEntity(
                                    id = UUID.randomUUID().toString(),
                                    text = text,
                                    createdAt = System.currentTimeMillis(),
                                )
                            )
                        }
                    }
                    is InboxViewAction.EntryClick -> {
                        if (isSelectMode.value) {
                            selectedIds.update { if (action.id in it) it - action.id else it + action.id }
                        } else {
                            trigger(InboxViewEffect.NavigateToEntry(action.id))
                        }
                    }
                    is InboxViewAction.LongPress -> {
                        isSelectMode.value = true
                        selectedIds.update { it + action.id }
                    }
                    is InboxViewAction.ToggleSelect -> {
                        selectedIds.update { if (action.id in it) it - action.id else it + action.id }
                    }
                    is InboxViewAction.StartBatchCoaching -> {
                        val ids = selectedIds.value.toList()
                        if (ids.isNotEmpty()) {
                            trigger(InboxViewEffect.NavigateToBatchCoaching(ids))
                        }
                    }
                    is InboxViewAction.ExitSelectMode -> {
                        isSelectMode.value = false
                        selectedIds.value = emptySet()
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
