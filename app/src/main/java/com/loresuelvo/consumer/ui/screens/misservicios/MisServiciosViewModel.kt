package com.loresuelvo.consumer.ui.screens.misservicios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAllServiceProposalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the "Mis Servicios" screen (`Route.MisServicios`).
 * Drives `GET /service-proposals` through
 * [GetAllServiceProposalsUseCase] and maps the typed
 * [ServiceProposalsOutcome] into the sealed
 * [MisServiciosUiState].
 *
 * The first load fires from [init] so the screen never has to
 * dispatch it explicitly (`LoResuelvoNav.MisServiciosRoute` does
 * not call any method on the VM). [load] is exposed as the
 * public retry path (the screen's "Reintentar" button calls it
 * via the route bridge).
 *
 * Per the `MessagesListViewModel` rationale: an empty `Ready`
 * list is intentional — "the consumer has no proposals" is a
 * presentation concern (the empty card), not a failure branch.
 * No in-flight guard yet — out of scope for scenario 03-VSP.
 */
@HiltViewModel
class MisServiciosViewModel @Inject constructor(
    private val getAllServiceProposals: GetAllServiceProposalsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MisServiciosUiState>(MisServiciosUiState.Loading)
    val uiState: StateFlow<MisServiciosUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Loads the consumer's full service-proposals list. Public so
     * the screen can re-trigger it on retry.
     */
    fun load() {
        viewModelScope.launch {
            _uiState.update { MisServiciosUiState.Loading }
            val next = when (val outcome = getAllServiceProposals()) {
                is ServiceProposalsOutcome.Success ->
                    MisServiciosUiState.Ready(outcome.proposals)
                is ServiceProposalsOutcome.Failure ->
                    MisServiciosUiState.Error(outcome)
            }
            _uiState.update { next }
        }
    }
}
