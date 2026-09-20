package com.loresuelvo.consumer.ui.screens.turnos

import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnosOutcome

/**
 * UDF state for the "Mis Turnos" screen (`Route.Turnos`,
 * visualize-turns.feature scenarios 02-VT / 03-VT).
 *
 * Landed minimally for scenario 02-VT: [Loading] + [Ready] are
 * in place; [Error] arrives with scenarios 13-VT / 14-VT.
 *
 *  - [Loading] — round trip in flight.
 *  - [Ready] — round trip succeeded; `turnos` may be empty
 *    (no scheduled appointments yet — scenario 03-VT).
 */
sealed interface TurnosUiState {
    data object Loading : TurnosUiState
    data class Ready(val turnos: List<Turno>) : TurnosUiState
}
