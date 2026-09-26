package com.loresuelvo.consumer.domain.workorder

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome

/**
 * Outcome for [com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase].
 * Mirrors the [ServiceProposalsOutcome] hierarchy so the caller
 * branches the same way it would on any other repository round
 * trip (US-54 scenario 16-VSP).
 *
 *  - [Found] — the work order exists and is ready to be rendered.
 *  - [NotFound] — no work order matches the given id; the screen
 *    renders its not-found copy.
 *  - [Failure] — the round trip failed; the screen falls back
 *    to its retry surface so the chat / Mis Servicios flow is
 *    never blocked by a missing work-order fetch.
 */
sealed interface GetWorkOrderOutcome {
    data class Found(val workOrder: WorkOrderDetail) : GetWorkOrderOutcome
    data object NotFound : GetWorkOrderOutcome
    data class Failure(val failure: ServiceProposalsOutcome.Failure) : GetWorkOrderOutcome
}

/**
 * Port for the consumer-side work-order detail surface (US-54
 * scenario 16-VSP, US-27 `visualize-turns-detail`). Adapters
 * translate the backend wire shape into [WorkOrderDetail]
 * without leaking JSON concerns past the data layer.
 *
 * Today the only legitimate response is the originating
 * [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal]
 * flattened into a [WorkOrderDetail]; once the dedicated
 * `GET /work-orders/{workOrderID}` endpoint is wired in US-27,
 * the adapter switches to it without touching the use case or
 * the screen.
 */
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
}