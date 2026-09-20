package com.loresuelvo.consumer.ui.screens.turnos

/**
 * UDF state for the "Mis Turnos" screen (`Route.Turnos`).
 *
 * Landed minimally for scenario 01-VT: only the [Loading]
 * branch exists today. The full hierarchy (Ready / Error)
 * arrives with subsequent scenarios (02-VT..14-VT) — see the
 * `visualize-turns.feature` header for the per-scenario
 * cadence.
 */
sealed interface TurnosUiState {
    data object Loading : TurnosUiState
}
