package com.loresuelvo.consumer.ui.screens.home

import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.turno.Turno

/**
 * UDF state for the consumer Home screen.
 *
 * Five surfaces are rendered in parallel:
 *
 *  - The category grid (the primary conversion action; pre-US-54).
 *  - The "Pagos pendientes" section (US-27, post-visualize-
 *    turns-detail follow-up) — turnos in
 *    [com.loresuelvo.consumer.domain.turno.TurnoStatus.AwaitingPayment].
 *    Hidden when the list is empty so the dashboard does not
 *    surface an empty / placeholder block.
 *  - The "Propuestas que requieren atención" section (US-54,
 *    scenario 01-VSP) — proposals in
 *    [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Pending].
 *  - The "Trabajos próximos" section (US-54, scenario 02-VSP) —
 *    proposals in `Accepted` status.
 *  - The "Mis Turnos" section (visualize-turns.feature, scenario
 *    01-VT) — scheduled appointments surfaced via
 *    `GET /work-orders`.
 *
 * The global state — [Loading], [Ready], [Error] — is driven by
 * the categories round trip only: that is the action without
 * which the Home dashboard is not usable, so a categories
 * failure flips the global to [Error] and the screen surfaces the
 * retry CTA. The proposals and turnos round trips land inside
 * whatever global state is current, exposed through
 * [pendingServiceProposals], [upcomingServiceProposals],
 * [awaitingPaymentTurnos] and [turnos]:
 *
 *  - Loading: round trip in flight; section renders a spinner.
 *  - Ready(items): round trip succeeded; `items` may be empty
 *    (no proposals / turnos in that status → section renders its
 *    own empty copy, scenarios 17-VSP / 03-VT; the "Pagos
 *    pendientes" section hides entirely on empty).
 *  - Error: round trip failed; section renders its own error
 *    copy while the categories grid keeps working.
 */
sealed interface HomeUiState {

    val categories: CategoriesState
    val pendingServiceProposals: ServiceProposalsState
    val upcomingServiceProposals: ServiceProposalsState
    val awaitingPaymentTurnos: TurnosState
    val turnos: TurnosState

    data class Loading(
        override val categories: CategoriesState = CategoriesState.Loading,
        override val pendingServiceProposals: ServiceProposalsState = ServiceProposalsState.Loading,
        override val upcomingServiceProposals: ServiceProposalsState = ServiceProposalsState.Loading,
        override val awaitingPaymentTurnos: TurnosState = TurnosState.Loading,
        override val turnos: TurnosState = TurnosState.Loading,
    ) : HomeUiState

    data class Ready(
        override val categories: CategoriesState,
        override val pendingServiceProposals: ServiceProposalsState,
        override val upcomingServiceProposals: ServiceProposalsState,
        override val awaitingPaymentTurnos: TurnosState,
        override val turnos: TurnosState,
    ) : HomeUiState

    data class Error(
        override val categories: CategoriesState = CategoriesState.Error,
        val messageResId: Int,
        override val pendingServiceProposals: ServiceProposalsState = ServiceProposalsState.Error,
        override val upcomingServiceProposals: ServiceProposalsState = ServiceProposalsState.Error,
        override val awaitingPaymentTurnos: TurnosState = TurnosState.Error,
        override val turnos: TurnosState = TurnosState.Error,
    ) : HomeUiState
}

sealed interface CategoriesState {
    data object Loading : CategoriesState
    data class Ready(val items: List<Category>) : CategoriesState
    data object Error : CategoriesState
}

/**
 * Sub-state for the two US-54 "proposals" sections on the Home
 * dashboard. Mirrors [CategoriesState]: a `Ready` with an empty
 * [items] list means the round trip succeeded but no proposals
 * are in the section's target status — the screen renders the
 * section's own empty copy rather than the "Error" surface (see
 * scenario 17-VSP).
 */
sealed interface ServiceProposalsState {
    data object Loading : ServiceProposalsState
    data class Ready(val items: List<ServiceProposal>) : ServiceProposalsState
    data object Error : ServiceProposalsState
}

/**
 * Sub-state for the "Mis Turnos" preview section on the Home
 * dashboard (visualize-turns.feature scenario 01-VT). Mirrors
 * [ServiceProposalsState]: a `Ready` with an empty [items] list
 * triggers the dedicated empty card (scenario 03-VT); an `Error`
 * silently hides the section so a failed `GET /work-orders`
 * doesn't break the dashboard. The dedicated MisTurnos screen
 * surfaces the typed error / retry branch instead.
 */
sealed interface TurnosState {
    data object Loading : TurnosState
    data class Ready(val items: List<Turno>) : TurnosState
    data object Error : TurnosState
}
