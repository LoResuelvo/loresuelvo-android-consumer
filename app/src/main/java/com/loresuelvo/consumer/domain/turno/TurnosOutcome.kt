package com.loresuelvo.consumer.domain.turno

/**
 * Outcome for `GET /work-orders` round trips.
 *
 * Landed minimally for scenario 02-VT: the [Success] / [Failure]
 * hierarchy is in place so the VM can transition to
 * [com.loresuelvo.consumer.ui.screens.turnos.TurnosUiState.Ready]
 * when the round trip lands.
 *
 *  - [Success] — round trip succeeded; `turnos` may be empty
 *    (the consumer has no scheduled appointments yet — scenario
 *    03-VT).
 *  - [Failure.Network] — transport-level failure.
 *  - [Failure.Server] — backend returned a non-2xx.
 */
sealed interface TurnosOutcome {
    data class Success(val turnos: List<Turno>) : TurnosOutcome
    sealed interface Failure : TurnosOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Server(val code: Int, val message: String) : Failure
    }
}
