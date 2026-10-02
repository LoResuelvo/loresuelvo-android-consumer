package com.loresuelvo.consumer.ui.screens.misservicios

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import java.io.IOException

sealed interface MisServiciosUiState {

    val selectedStatusFilter: ServiceProposalStatus?

    data object Loading : MisServiciosUiState {
        override val selectedStatusFilter: ServiceProposalStatus? = null
    }

    data class Ready(
        val proposals: List<ServiceProposal>,
        override val selectedStatusFilter: ServiceProposalStatus? = null,
    ) : MisServiciosUiState

    data class Error(
        val failure: ServiceProposalsOutcome.Failure,
        override val selectedStatusFilter: ServiceProposalStatus? = null,
    ) : MisServiciosUiState
}
