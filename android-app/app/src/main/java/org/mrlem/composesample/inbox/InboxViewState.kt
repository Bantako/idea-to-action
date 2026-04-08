package org.mrlem.composesample.inbox

data class InboxViewState(
    val entries: List<InboxEntry> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isSelectMode: Boolean = false,
)

sealed interface InboxViewAction {
    data class AddEntry(val text: String) : InboxViewAction
    data class EntryClick(val id: String) : InboxViewAction
    data class LongPress(val id: String) : InboxViewAction
    data class ToggleSelect(val id: String) : InboxViewAction
    data object StartBatchCoaching : InboxViewAction
    data object ExitSelectMode : InboxViewAction
}

sealed interface InboxViewEffect {
    data class NavigateToEntry(val id: String) : InboxViewEffect
    data class NavigateToBatchCoaching(val entryIds: List<String>) : InboxViewEffect
}
