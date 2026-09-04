package com.loresuelvo.consumer.ui.screens.misservicios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAcceptedServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAllServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetRejectedServiceProposalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the "Mis Servicios" screen (`Route.MisServicios`).
 *
 * Drives the four proposal-list round trips:
 *
 *  - [GetAllServiceProposalsUseCase] (default, no filter) — US-54
 *    scenarios 03-VSP / 04-VSP.
 *  - [GetPendingServiceProposalsUseCase] — US-54 scenario 05-VSP.
 *  - [GetAcceptedServiceProposalsUseCase] — US-54 scenario 02-VSP
 *    (also used by the Home dashboard) and 06-VSP.
 *  - [GetRejectedServiceProposalsUseCase] — US-54 scenario 07-VSP.
 *
 * The VM is **stateless across config changes** (the AndroidX
 * `ViewModel` survives rotation), so `selectedStatusFilter` lives
 * on the VM and is mirrored into every `MisServiciosUiState`
 * variant so the filter chips can stay highlighted across Loading
 * and Error transitions.
 *
 * [onFilterSelected] is the single entry point the screen uses
 * to switch filters; it always re-fires [load] so the new list
 * (or Error) lands under the new filter label.
 *
 * Per the `MessagesListViewModel` rationale: an empty `Ready`
 * list is intentional — "the consumer has no proposals for this
 * filter" is a presentation concern (the empty card), not a
 * failure branch.
 */
@HiltViewModel
class MisServiciosViewModel @Inject constructor(
    private val getAllServiceProposals: GetAllServiceProposalsUseCase,
    private val getPendingServiceProposals: GetPendingServiceProposalsUseCase,
    private val getAcceptedServiceProposals: GetAcceptedServiceProposalsUseCase,
    private val getRejectedServiceProposals: GetRejectedServiceProposalsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MisServiciosUiState>(MisServiciosUiState.Loading)
    val uiState: StateFlow<MisServiciosUiState> = _uiState.asStateFlow()

    /**
     * The currently selected status filter. `null` means "Todos"
     * (the initial state) — surfaces every proposal regardless
     * of status. Survives [load] so a retry after Error keeps
     * the same filter active.
     */
    private var selectedStatusFilter: ServiceProposalStatus? = null

    init {
        load()
    }

    /**
     * Loads the proposals list for the current [selectedStatusFilter]
     * and emits the next [MisServiciosUiState] on [uiState].
     *
     * Public so the screen can re-trigger on retry and on
     * explicit refresh.
     */
    fun load() {
        viewModelScope.launch {
            _uiState.update { current ->
                current.asLoading()
            }
            val next = when (selectedStatusFilter) {
                null -> getAllServiceProposals()
                ServiceProposalStatus.Pending -> getPendingServiceProposals()
                ServiceProposalStatus.Accepted -> getAcceptedServiceProposals()
                ServiceProposalStatus.Rejected -> getRejectedServiceProposals()
            }
            _uiState.update {
                when (next) {
                    is ServiceProposalsOutcome.Success ->
                        MisServiciosUiState.Ready(
                            proposals = next.proposals,
                            selectedStatusFilter = selectedStatusFilter,
                        )
                    is ServiceProposalsOutcome.Failure ->
                        MisServiciosUiState.Error(
                            failure = next,
                            selectedStatusFilter = selectedStatusFilter,
                        )
                }
            }
        }
    }

    /**
     * Switches the active filter chip and re-fires [load]. Pass
     * `null` to return to the "Todos" view (every status).
     */
    fun onFilterSelected(filter: ServiceProposalStatus?) {
        selectedStatusFilter = filter
        load()
    }

    private fun MisServiciosUiState.asLoading(): MisServiciosUiState = when (this) {
        is MisServiciosUiState.Loading -> this
        is MisServiciosUiState.Ready -> MisServiciosUiState.Loading
        is MisServiciosUiState.Error -> MisServiciosUiState.Loading
    }
}
