package com.loresuelvo.consumer.ui.screens.workorderdetail

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail

sealed interface WorkOrderDetailUiState {
    data object Loading : WorkOrderDetailUiState
    data class Ready(
        val workOrderId: String,
        val workOrder: WorkOrderDetail,
        val composer: ReviewComposerState = ReviewComposerState.Hidden,
    ) : WorkOrderDetailUiState
    data object NotFound : WorkOrderDetailUiState
    data class Error(val failure: ServiceProposalsOutcome.Failure) : WorkOrderDetailUiState
}

sealed interface ReviewComposerState {
    data object Hidden : ReviewComposerState

    data class Editing(
        val ratingDraft: Int? = null,
        val descriptionDraft: String = "",
        val submitting: Boolean = false,
        val error: SubmitWorkOrderReviewOutcome? = null,
    ) : ReviewComposerState {
        /** True while the composer can ship a valid submission. */
        val canSubmit: Boolean
            get() = !submitting && ratingDraft != null
    }
}
