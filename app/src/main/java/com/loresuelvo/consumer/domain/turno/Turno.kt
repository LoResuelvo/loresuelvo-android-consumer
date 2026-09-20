package com.loresuelvo.consumer.domain.turno

/**
 * A scheduled appointment (a "turno") between the consumer and a
 * provider. Sourced from `GET /work-orders` and surfaced by the
 * "Mis Turnos" surface.
 *
 * Landed minimally for scenario 02-VT: it carries the fields
 * needed to render a row in the list (id, status, counterpart,
 * description, amount, scheduled timestamp). Fields that the
 * wire does not expose today (`conversation_id`,
 * `estimated_duration_minutes`, `accepted_on`) land alongside
 * the scenarios that surface them.
 *
 * Pure domain type: camelCase, no framework dependencies, no
 * JSON.
 */
data class Turno(
    val id: String,
    val serviceProposalId: String,
    val status: TurnoStatus,
    val counterpart: TurnoCounterpart,
    val description: String,
    val amountCents: Long,
    val scheduledOnEpochMillis: Long,
)
