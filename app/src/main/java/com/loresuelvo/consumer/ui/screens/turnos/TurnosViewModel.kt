package com.loresuelvo.consumer.ui.screens.turnos

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UDF ViewModel for the "Mis Turnos" screen (`Route.Turnos`).
 *
 * Landed minimally for scenario 01-VT: it holds the screen in
 * the [Loading] state and exposes nothing else. The full
 * `init { load() }` round trip against
 * `com.loresuelvo.consumer.domain.turno.TurnosRepository` lands
 * with scenario 02-VT, alongside the [TurnosUiState.Ready] /
 * [TurnosUiState.Error] branches.
 */
@HiltViewModel
class TurnosViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<TurnosUiState>(TurnosUiState.Loading)
    val uiState: StateFlow<TurnosUiState> = _uiState.asStateFlow()
}
