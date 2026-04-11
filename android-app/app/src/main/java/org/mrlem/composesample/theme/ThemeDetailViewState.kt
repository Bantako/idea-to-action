package org.mrlem.composesample.theme

import org.mrlem.composesample.step.StepEntity

data class ThemeDetailViewState(
    val title: String = "",
    val goal: String = "",
    val weight: String = "medium",
    val steps: List<StepEntity> = emptyList(),
    val completedStepCount: Int = 0,
    val totalStepCount: Int = 0,
    val activityLog: List<ActivityLogEntry> = emptyList(),
)

data class ActivityLogEntry(
    val date: String,
    val stepTitle: String,
    val result: String,  // "done" | "started" | "not_done"
    val memo: String?,
)

sealed interface ThemeDetailViewAction {
    data class SaveGoal(val text: String) : ThemeDetailViewAction
    data class SaveTitle(val text: String) : ThemeDetailViewAction
    data class SetWeight(val weight: String) : ThemeDetailViewAction
    data object Archive : ThemeDetailViewAction
    data class AddStep(val title: String) : ThemeDetailViewAction
    data class ArchiveStep(val stepId: String) : ThemeDetailViewAction
    data class NavigateToStep(val stepId: String) : ThemeDetailViewAction
    data class ScheduleStep(val stepId: String, val date: String, val startTime: Int?, val durationMinutes: Int?, val notificationEnabled: Boolean = false) : ThemeDetailViewAction
}

sealed interface ThemeDetailViewEffect {
    data object NavigateBack : ThemeDetailViewEffect
    data class NavigateToStep(val stepId: String) : ThemeDetailViewEffect
}
