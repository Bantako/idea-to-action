package org.mrlem.composesample.inbox

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.theme.ThemeDao
import org.mrlem.composesample.theme.ThemeEntity
import java.util.UUID

@HiltViewModel(assistedFactory = InboxDetailViewModel.Factory::class)
class InboxDetailViewModel @AssistedInject constructor(
    private val inboxDao: InboxDao,
    private val themeDao: ThemeDao,
    @Assisted private val key: InboxEntryKey,
) : UnidirectionalViewModel<InboxDetailViewState, InboxDetailViewAction, InboxDetailViewEffect>() {

    @AssistedFactory
    interface Factory {
        fun create(key: InboxEntryKey): InboxDetailViewModel
    }

    override val state = inboxDao.observeById(key.entryId)
        .map { entity -> InboxDetailViewState(text = entity?.text ?: "") }
        .stateIn(viewModelScope, WhileSubscribed(), InboxDetailViewState())

    init {
        actions
            .onEach { action ->
                when (action) {
                    is InboxDetailViewAction.Delete -> {
                        inboxDao.archive(key.entryId)
                        trigger(InboxDetailViewEffect.NavigateBack)
                    }
                    is InboxDetailViewAction.StartCoaching -> {
                        trigger(InboxDetailViewEffect.NavigateToCoaching(key.entryId))
                    }
                    is InboxDetailViewAction.CreateTheme -> {
                        val entry = inboxDao.getById(key.entryId) ?: return@onEach
                        val themeId = UUID.randomUUID().toString()
                        themeDao.insert(
                            ThemeEntity(
                                id = themeId,
                                title = entry.text.take(80),
                                goal = null,
                                weight = "medium",
                                status = "active",
                                createdAt = System.currentTimeMillis(),
                            )
                        )
                        inboxDao.archiveWithTheme(key.entryId, themeId)
                        trigger(InboxDetailViewEffect.NavigateToTheme(themeId))
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
