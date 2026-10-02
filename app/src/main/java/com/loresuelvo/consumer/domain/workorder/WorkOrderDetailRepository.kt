package com.loresuelvo.consumer.domain.workorder

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome

sealed interface GetWorkOrderOutcome {
    data class Found(val workOrder: WorkOrderDetail) : GetWorkOrderOutcome
    data object NotFound : GetWorkOrderOutcome
    data class Failure(val failure: ServiceProposalsOutcome.Failure) : GetWorkOrderOutcome
}

interface WorkOrderDetailRepository {

    /**
     * Looks up the [WorkOrderDetail] tied to [workOrderId].
     * Returns [GetWorkOrderOutcome.NotFound] when no work order
     * matches, [GetWorkOrderOutcome.Failure] when the round trip
     * failed. Implementations never throw.
     */
    suspend fun getWorkOrderDetail(
        workOrderId: String,
        provider: WorkOrderDetailCounterpart? = null,
    ): GetWorkOrderOutcome


    suspend fun submitReview(
        workOrderId: String,
        rating: Int,
        description: String,
    ): SubmitWorkOrderReviewOutcome
}