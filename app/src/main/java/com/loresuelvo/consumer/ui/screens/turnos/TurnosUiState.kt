package com.loresuelvo.consumer.ui.screens.turnos

import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus

sealed interface TurnosUiState {
    val selectedStatusFilter: TurnoStatus?
    val searchQuery: String

    data object Loading : TurnosUiState {
        override val selectedStatusFilter: TurnoStatus? = null
        override val searchQuery: String = ""
    }
    data class Ready(
        val turnos: List<Turno>,
        override val selectedStatusFilter: TurnoStatus? = null,
        override val searchQuery: String = "",
        val calendarConnectionStatus: CalendarConnectionStatus = CalendarConnectionStatus.UNKNOWN,
    ) : TurnosUiState
    data class Error(
        val failure: TurnosOutcome.Failure,
        override val selectedStatusFilter: TurnoStatus? = null,
        override val searchQuery: String = "",
    ) : TurnosUiState
}
