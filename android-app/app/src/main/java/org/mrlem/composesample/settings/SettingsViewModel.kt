package org.mrlem.composesample.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore,
) : UnidirectionalViewModel<SettingsViewState, SettingsViewAction, Unit>() {

    private val _state = MutableStateFlow(SettingsViewState(apiKey = apiKeyStore.claudeApiKey))

    override val state = _state.stateIn(viewModelScope, WhileSubscribed(), _state.value)

    init {
        actions
            .onEach { action ->
                when (action) {
                    is SettingsViewAction.ApiKeyChange -> _state.update { it.copy(apiKey = action.text, saved = false) }
                    is SettingsViewAction.Save -> {
                        apiKeyStore.claudeApiKey = _state.value.apiKey.trim()
                        _state.update { it.copy(saved = true) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
