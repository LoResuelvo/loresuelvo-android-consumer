package com.loresuelvo.consumer.ui.screens.workorderdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun WorkOrderDetailScreen(
    state: WorkOrderDetailUiState,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    onPayNow: () -> Unit = {},
    onOpenReviewForm: () -> Unit = {},
    onRatingChange: (Int) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onSubmitReview: () -> Unit = {},
    onCancelReview: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    WorkOrderDetailScreen(
        state = state,
        actions = WorkOrderDetailActions(
            onRetry = onRetry,
            onPayNow = onPayNow,
            review = WorkOrderDetailActions.ReviewActions(
                onOpen = onOpenReviewForm,
                onRatingChange = onRatingChange,
                onDescriptionChange = onDescriptionChange,
                onSubmit = onSubmitReview,
                onCancel = onCancelReview,
            ),
        ),
        modifier = modifier,
    )
}
