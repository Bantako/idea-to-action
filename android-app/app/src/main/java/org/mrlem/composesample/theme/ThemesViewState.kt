package org.mrlem.composesample.theme

data class ThemesViewState(
    val themes: List<ThemeItem> = emptyList(),
    val archivedThemes: List<ThemeItem> = emptyList(),
    val showArchived: Boolean = false,
    val input: String = "",
    val inputWeight: String = "medium",
)

data class ThemeItem(
    val id: String,
    val title: String,
    val weight: String,
    val pendingStepCount: Int,
)

sealed interface ThemesViewAction {
    data class InputChange(val text: String) : ThemesViewAction
    data class InputWeightChange(val weight: String) : ThemesViewAction
    data object AddTheme : ThemesViewAction
    data object ToggleShowArchived : ThemesViewAction
}
