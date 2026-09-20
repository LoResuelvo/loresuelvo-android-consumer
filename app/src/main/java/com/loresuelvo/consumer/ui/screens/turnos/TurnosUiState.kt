package com.loresuelvo.consumer.ui.screens.turnos

import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnosOutcome

/**
 * UDF state for the "Mis Turnos" screen (`Route.Turnos`).
 *
 * Landed incrementally per scenario:
 *  - 01-VT → [Loading].
 *  - 02-VT → [Ready] (non-empty + empty).
 *  - 13-VT / 14-VT → [Error] (network + server).
 *
 *  - [Loading] — round trip in flight.
 *  - [Ready] — round trip succeeded; `turnos` may be empty
 *    (no scheduled appointments yet — scenario 03-VT).
 *  - [Error] — round trip failed; the typed [TurnosOutcome.Failure]
 *    lets the screen render the network vs server copy
 *    distinctly (13-VT vs 14-VT) and offer a retry CTA.
 */
sealed interface TurnosUiState {
    data object Loading : TurnosUiState
    data class Ready(val turnos: List<Turno>) : TurnosUiState
    data class Error(val failure: TurnosOutcome.Failure) : TurnosUiState
}
