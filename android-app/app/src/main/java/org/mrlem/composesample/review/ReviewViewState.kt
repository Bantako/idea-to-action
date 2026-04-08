package org.mrlem.composesample.review

data class ReviewViewState(
    val items: List<ReviewStepUi> = emptyList(),
    val detailItem: ReviewStepUi? = null,
)

data class ReviewStepUi(
    val scheduledStepId: String,
    val stepId: String,
    val title: String,
    val starterAction: String?,
    val themeName: String,
    val started: Boolean,
    val done: Boolean,
    val memo: String?,
    val result: String? = null,  // "done" | "started" | "not_done"
)

sealed interface ReviewViewAction {
    data class ShowDetail(val item: ReviewStepUi) : ReviewViewAction
    data object HideDetail : ReviewViewAction
    data class MarkResult(val scheduledStepId: String, val stepId: String, val result: String) : ReviewViewAction
    data class ArchiveStep(val stepId: String) : ReviewViewAction
    data class CarryOver(val scheduledStepId: String, val stepId: String) : ReviewViewAction
}
