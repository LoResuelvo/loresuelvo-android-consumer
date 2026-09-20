package com.loresuelvo.consumer.ui.screens.turnos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the "Mis Turnos" screen (`Route.Turnos`).
 *
 * Drives the round trip against
 * [com.loresuelvo.consumer.domain.turno.TurnosRepository] via
 * [GetTurnosUseCase]. The full state machine
 * ([Loading] → [Ready] / [Error]) is now wired:
 *
 *  - 01-VT → Loading.
 *  - 02-VT → Ready (non-empty).
 *  - 03-VT → Ready (empty).
 *  - 13-VT → Error(Network) — surfaces the connection-lost copy
 *    + retry CTA.
 *  - 14-VT → Error(Server) — surfaces the typed server copy
 *    + retry CTA.
 *
 * [load] is re-entrant so the screen can re-fire on retry
 * (scenarios 13-VT / 14-VT).
 */
@HiltViewModel
class TurnosViewModel @Inject constructor(
    private val getTurnos: GetTurnosUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TurnosUiState>(TurnosUiState.Loading)
    val uiState: StateFlow<TurnosUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { TurnosUiState.Loading }
            _uiState.update {
                when (val outcome = getTurnos()) {
                    is TurnosOutcome.Success ->
                        TurnosUiState.Ready(outcome.turnos)
                    is TurnosOutcome.Failure ->
                        TurnosUiState.Error(outcome)
                }
            }
        }
    }
}
