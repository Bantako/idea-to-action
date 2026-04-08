package org.mrlem.composesample.inbox

data class InboxDetailViewState(
    val text: String = "",
)

sealed interface InboxDetailViewAction {
    data object Delete : InboxDetailViewAction
    data object StartCoaching : InboxDetailViewAction
    data object CreateTheme : InboxDetailViewAction
}

sealed interface InboxDetailViewEffect {
    data object NavigateBack : InboxDetailViewEffect
    data class NavigateToCoaching(val entryId: String) : InboxDetailViewEffect
    data class NavigateToTheme(val themeId: String) : InboxDetailViewEffect
}
