package org.mrlem.composesample.today

data class TodayViewState(
    val selectedDate: String = "",
    val timedItems: List<ScheduledStepUi> = emptyList(),
    val untimedItems: List<ScheduledStepUi> = emptyList(),
    val detailItem: ScheduledStepUi? = null,
)

data class ScheduledStepUi(
    val scheduledStepId: String,
    val stepId: String,
    val title: String,
    val starterAction: String?,
    val themeName: String,
    val themeGoal: String?,
    val startTime: Int?,
    val durationMinutes: Int?,
    val started: Boolean,
    val done: Boolean,
    val memo: String?,
)

sealed interface TodayViewAction {
    data class ShowDetail(val item: ScheduledStepUi) : TodayViewAction
    data object HideDetail : TodayViewAction
    data class MarkStarted(val scheduledStepId: String, val memo: String?) : TodayViewAction
    data class SwitchDate(val date: String) : TodayViewAction
}
