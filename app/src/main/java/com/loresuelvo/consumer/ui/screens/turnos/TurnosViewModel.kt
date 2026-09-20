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
 * Landed minimally for scenario 02-VT: it injects
 * [GetTurnosUseCase] and round-trips on `init { load() }`,
 * transitioning [TurnosUiState.Loading] → [TurnosUiState.Ready]
 * on `Success`. The [TurnosUiState.Error] branch arrives with
 * scenarios 13-VT / 14-VT.
 *
 * Errors are not swallowed: [TurnosOutcome.Failure.Network] and
 * [TurnosOutcome.Failure.Server] propagate verbatim so the
 * screen can render the typed retry CTA.
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

    /**
     * Public so the screen can re-trigger on retry (13-VT /
     * 14-VT).
     */
    fun load() {
        viewModelScope.launch {
            _uiState.update { TurnosUiState.Loading }
            _uiState.update {
                when (val outcome = getTurnos()) {
                    is TurnosOutcome.Success ->
                        TurnosUiState.Ready(outcome.turnos)
                    is TurnosOutcome.Failure ->
                        // Landed minimally: for 02-VT the world
                        // only seeds `Success`. The Error branch
                        // is plugged in with scenarios 13-VT /
                        // 14-VT.
                        TurnosUiState.Loading
                }
            }
        }
    }
}
