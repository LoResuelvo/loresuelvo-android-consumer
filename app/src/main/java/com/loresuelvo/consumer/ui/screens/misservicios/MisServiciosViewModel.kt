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
