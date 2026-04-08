package org.mrlem.composesample.step

data class StepDetailViewState(
    val title: String = "",
    val starterAction: String = "",
    val weight: String = "medium",
)

sealed interface StepDetailViewAction {
    data class SaveTitle(val text: String) : StepDetailViewAction
    data class SaveStarterAction(val text: String) : StepDetailViewAction
    data class SetWeight(val weight: String) : StepDetailViewAction
}

sealed interface StepDetailViewEffect {
    data object NavigateBack : StepDetailViewEffect
}
