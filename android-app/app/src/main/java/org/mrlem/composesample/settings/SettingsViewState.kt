package org.mrlem.composesample.settings

data class SettingsViewState(
    val apiKey: String = "",
    val saved: Boolean = false,
)

sealed interface SettingsViewAction {
    data class ApiKeyChange(val text: String) : SettingsViewAction
    data object Save : SettingsViewAction
}
