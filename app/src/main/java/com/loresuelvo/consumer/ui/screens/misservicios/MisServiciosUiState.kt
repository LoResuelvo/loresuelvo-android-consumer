package com.loresuelvo.consumer.ui.screens.misservicios

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome

/**
 * UDF state for the "Mis Servicios" screen (`Route.MisServicios`),
 * the consumer-facing surface that lists every service proposal
 * regardless of status (US-54 scenario 03-VSP).
 *
 * Modelled as a sealed hierarchy so the screen renders exactly
 * one of the states without boolean flags — mirrors
 * [com.loresuelvo.consumer.ui.screens.messages.MessagesListUiState].
 *
 *  - [Loading] — initial fetch in flight (also reused on manual
 *    retry).
 *  - [Ready] — round trip succeeded; [proposals] may be empty
 *    (the screen renders the empty card in that case, per the
 *    "no items is a presentation concern" rationale documented
 *    on `MessagesListUiState`).
 *  - [Error] — round trip failed; the carried
 *    [ServiceProposalsOutcome.Failure] subtype lets the screen
 *    render network vs server vs unauthorized strings distinctly.
 */
sealed interface MisServiciosUiState {

    data object Loading : MisServiciosUiState

    data class Ready(val proposals: List<ServiceProposal>) : MisServiciosUiState

    data class Error(val failure: ServiceProposalsOutcome.Failure) : MisServiciosUiState
}
