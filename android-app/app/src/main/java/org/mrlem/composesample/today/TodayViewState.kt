package org.mrlem.composesample.today

data class TodayViewState(
    val selectedDate: String = "",
    val items: List<ScheduledStepUi> = emptyList(),
    val detailItem: ScheduledStepUi? = null,
    val isReviewMode: Boolean = false,
    val autoSuggestPending: Boolean = false,
    val showAddSheet: Boolean = false,
    val availableSteps: List<StepPickerUi> = emptyList(),
)

data class ScheduledStepUi(
    val scheduledStepId: String,
    val stepId: String,
    val title: String,
    val starterAction: String?,
    val themeName: String,
    val themeGoal: String?,
    val started: Boolean,
    val done: Boolean,
    val memo: String?,
)

data class StepPickerUi(
    val stepId: String,
    val stepTitle: String,
    val themeName: String,
)

sealed interface TodayViewAction {
    data class ShowDetail(val item: ScheduledStepUi) : TodayViewAction
    data object HideDetail : TodayViewAction
    data class MarkStarted(val scheduledStepId: String, val memo: String?) : TodayViewAction
    data class SwitchDate(val date: String) : TodayViewAction
    data object AutoSuggestHandled : TodayViewAction
    data object ShowAddSheet : TodayViewAction
    data object HideAddSheet : TodayViewAction
    data class AddStepToToday(val stepId: String) : TodayViewAction
    data class MoveStep(val scheduledStepId: String, val direction: Int) : TodayViewAction // -1=up, +1=down
}
