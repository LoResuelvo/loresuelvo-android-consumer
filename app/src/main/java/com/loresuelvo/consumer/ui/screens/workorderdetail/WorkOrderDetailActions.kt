package com.loresuelvo.consumer.ui.screens.workorderdetail

/** User interactions exposed by the work-order detail screen. */
data class WorkOrderDetailActions(
    val onRetry: () -> Unit = {},
    val onPayNow: () -> Unit = {},
    val review: ReviewActions = ReviewActions(),
) {
    data class ReviewActions(
        val onOpen: () -> Unit = {},
        val onRatingChange: (Int) -> Unit = {},
        val onDescriptionChange: (String) -> Unit = {},
        val onSubmit: () -> Unit = {},
        val onCancel: () -> Unit = {},
    )
}
