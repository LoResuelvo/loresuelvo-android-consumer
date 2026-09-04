package com.loresuelvo.consumer.ui.screens.misservicios

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import java.io.IOException

/**
 * UDF state for the "Mis Servicios" screen (`Route.MisServicios`),
 * the consumer-facing list of every service proposal regardless of
 * status (US-54 scenarios 03-VSP / 04-VSP / 05-VSP / 06-VSP /
 * 07-VSP).
 *
 * Three surfaces are rendered in parallel:
 *
 *  - The category grid (the primary conversion action; pre-US-54).
 *  - The status filter chips (introduced in scenario 05-VSP):
 *    "Todos" | "Pendientes" | "Aceptadas" | "Rechazadas". The
 *    currently selected chip is driven by [selectedStatusFilter].
 *  - The proposal list: every proposal regardless of status
 *    (03-VSP / 04-VSP) OR the filtered subset (05-VSP / 06-VSP /
 *    07-VSP).
 *
 * The global state — [Loading], [Ready], [Error] — is driven by
 * the categories round trip only (see [com.loresuelvo.consumer.ui.screens.home.HomeViewModel]):
 * the proposals round trip lands inside whatever global state is
 * current.
 *
 * [selectedStatusFilter] lives in **every** variant (Loading /
 * Ready / Error) so the screen can keep the active chip
 * highlighted even while the round trip is in flight or has
 * failed. `null` means "Todos" — the default filter that the VM
 * starts with, so scenarios 03-VSP / 04-VSP (which never tap a
 * chip) observe every proposal regardless of status.
 */
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
