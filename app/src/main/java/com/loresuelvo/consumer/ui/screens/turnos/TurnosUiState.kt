package com.loresuelvo.consumer.ui.screens.turnos

import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus

sealed interface TurnosUiState {
    data object Loading : TurnosUiState
    data class Ready(
        val turnos: List<Turno>,
        val calendarConnectionStatus: CalendarConnectionStatus = CalendarConnectionStatus.UNKNOWN,
    ) : TurnosUiState
    data class Error(val failure: TurnosOutcome.Failure) : TurnosUiState
}
