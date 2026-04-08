package org.mrlem.composesample.coaching

data class ThemeOption(val id: String, val title: String)

data class SuggestedStep(val title: String, val themeId: String)

data class CoachingViewState(
    val inboxText: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val availableThemes: List<ThemeOption> = emptyList(),
    val showAddStep: Boolean = false,
    val suggestedSteps: List<SuggestedStep> = emptyList(),
)

data class ChatMessage(
    val role: String,  // "user" | "assistant"
    val content: String,
)

sealed interface CoachingViewAction {
    data class Send(val text: String) : CoachingViewAction
    data object CreateTheme : CoachingViewAction
    data object ShowAddStep : CoachingViewAction
    data object HideAddStep : CoachingViewAction
    data class AddStep(val title: String, val themeId: String) : CoachingViewAction
}

sealed interface CoachingViewEffect {
    data class NavigateToTheme(val themeId: String) : CoachingViewEffect
}
