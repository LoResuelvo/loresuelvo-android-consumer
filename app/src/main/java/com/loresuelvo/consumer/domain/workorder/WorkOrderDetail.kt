package com.loresuelvo.consumer.domain.workorder

import com.loresuelvo.consumer.domain.turno.TurnoStatus

/**
 * The "orden de trabajo" the consumer sees once a proposal has
 * been accepted. Pure domain type: camelCase, no framework
 * dependencies, no JSON.
 *
 * Today the work order is just the accepted proposal flattened;
 * the dedicated `GET /work-orders/{workOrderID}` endpoint is
 * wired up in a follow-up US-27 commit so the same shape is
 * sourced from the backend rather than from
 * [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal].
 *
 * @property proposalId the originating [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal]
 *   id. Acts as the join key between the two domains; the work
 *   order id is the proposal id itself until the backend exposes
 *   a dedicated work-order endpoint.
 * @property provider the counterpart that will carry out the
 *   service. Replaces the legacy `providerName: String +
 *   categoryName: String` pair so the screen can render the
 *   avatar + full name + category without round-tripping to
 *   the chat / provider endpoints.
 * @property estimatedDurationMinutes the provider's estimate of
 *   how long the visit will take. Nullable because the backend
 *   may not have it when the proposal was just accepted.
 *
 * **US-27 migration** ([TurnoStatus] supersedes the proposal-side
 * `ServiceProposalStatus`, [WorkOrderDetailCounterpart]
 * supersedes the legacy `providerName` / `categoryName`):
 * the work-order detail surface now shares its status
 * vocabulary with the listing endpoint so `awaiting_payment` /
 * `paid` are reachable from both flows.
 */
data class WorkOrderDetail(
    val proposalId: String,
    val provider: WorkOrderDetailCounterpart,
    val description: String,
    val amountCents: Long,
    val scheduledOnEpochMillis: Long,
    val estimatedDurationMinutes: Int?,
    val status: TurnoStatus,
)