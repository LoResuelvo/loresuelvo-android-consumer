package com.loresuelvo.consumer.ui.screens.turnos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * Landed minimally for scenario 01-VT: it holds the screen in
 * the [Loading] state and exposes nothing else. The `init` block
 * triggers a no-op `update { Loading }` so collectors attached
 * via `StateFlow.collect` (the pattern the BDD world uses with
 * `StandardTestDispatcher`) reliably see the initial emission.
 *
 * The full `init { load() }` round trip against
 * `com.loresuelvo.consumer.domain.turno.TurnosRepository` lands
 * with scenario 02-VT, alongside the [TurnosUiState.Ready] /
 * [TurnosUiState.Error] branches.
 */
@HiltViewModel
class TurnosViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<TurnosUiState>(TurnosUiState.Loading)
    val uiState: StateFlow<TurnosUiState> = _uiState.asStateFlow()

    init {
        // Forced no-op update so the initial Loading emission
        // is observed by collectors that subscribed after the
        // field initializer ran (StateFlow's conflated contract
        // does not re-emit the current value to a late
        // subscriber without an explicit update).
        viewModelScope.launch {
            _uiState.update { TurnosUiState.Loading }
        }
    }
}
