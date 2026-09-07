package com.loresuelvo.consumer.domain.workorder

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome

/**
 * Outcome for [com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderByProposalIdUseCase].
 * Mirrors the [ServiceProposalsOutcome] hierarchy so the caller
 * branches the same way it would on any other repository round
 * trip (US-54 scenario 16-VSP).
 *
 *  - [Found] — the proposal exists and is ready to be rendered
 *    as a work order.
 *  - [NotFound] — no proposal matches the given id; the screen
 *    renders its not-found copy.
 *  - [Failure] — the round trip failed; the screen falls back
 *    to its retry surface so the chat / Mis Servicios flow is
 *    never blocked by a missing work-order fetch.
 */
sealed interface GetWorkOrderOutcome {
    data class Found(val workOrder: WorkOrder) : GetWorkOrderOutcome
    data object NotFound : GetWorkOrderOutcome
    data class Failure(val failure: ServiceProposalsOutcome.Failure) : GetWorkOrderOutcome
}

/**
 * Port for the consumer-side work-order surface (US-54 scenario
 * 16-VSP). Adapters translate the backend wire shape into
 * [WorkOrder] without leaking JSON concerns past the data layer.
 *
 * Today the only legitimate response is the originating
 * [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal]
 * flattened into a [WorkOrder]; once the backend exposes a
 * dedicated work-order endpoint the adapter switches to it
 * without touching the use case or the screen.
 */
interface WorkOrderRepository {

    /**
     * Looks up the [WorkOrder] tied to [proposalId]. Returns
     * [GetWorkOrderOutcome.NotFound] when no proposal matches,
     * [GetWorkOrderOutcome.Failure] when the round trip failed.
     * Implementations never throw.
     */
    suspend fun getWorkOrderByProposalId(proposalId: String): GetWorkOrderOutcome
}