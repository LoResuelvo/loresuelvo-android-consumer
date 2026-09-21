package com.loresuelvo.consumer.domain.workorder

import com.loresuelvo.consumer.domain.turno.TurnoStatus

/**
 * The "orden de trabajo" the consumer sees once a proposal has
 * been accepted. Pure domain type: camelCase, no framework
 * dependencies, no JSON. The class is a **flattened snapshot** of
 * the originating [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal]
 * until US-27 wires the dedicated
 * `GET /work-orders/{workOrderID}` endpoint that supersedes
 * this provisional shape.
 *
 * Right now the work order is just the accepted proposal; a
 * future revision may add booking-terms fields (platform fee,
 * deposit, etc.) without changing the consumer-facing shape.
 *
 * @property proposalId the originating [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal]
 *   id. Acts as the join key between the two domains; the work
 *   order id is the proposal id itself until the backend exposes
 *   a dedicated work-order endpoint.
 * @property providerName the counterpart's full name
 *   (`name + surname`) so the screen does not have to assemble it.
 * @property estimatedDurationMinutes the provider's estimate of
 *   how long the visit will take. Nullable because the backend
 *   may not have it when the proposal was just accepted.
 *
 * **US-27 migration** ([TurnoStatus] supersedes the proposal-side
 * `ServiceProposalStatus`): the work-order detail surface now
 * shares its status vocabulary with the listing endpoint so
 * `awaiting_payment` / `paid` are reachable from both flows.
 */
data class WorkOrderDetail(
    val proposalId: String,
    val providerName: String,
    val categoryName: String,
    val description: String,
    val amountCents: Long,
    val scheduledOnEpochMillis: Long,
    val estimatedDurationMinutes: Int?,
    val status: TurnoStatus,
)