package com.loresuelvo.consumer.domain.turno

/**
 * Port for the consumer-facing "Mis Turnos" surface. Adapters
 * translate the backend wire shape into [Turno] without leaking
 * JSON concerns past the data layer.
 *
 * Landed minimally for scenario 02-VT: the port has a single
 * `getTurnos()` method that fetches the consumer's full list of
 * scheduled appointments. Filtering by status happens in the
 * presentation layer if/when needed.
 *
 * Implementations never throw: every transport / HTTP failure
 * is mapped to a typed [TurnosOutcome.Failure].
 */
interface TurnosRepository {
    suspend fun getTurnos(): TurnosOutcome
}
