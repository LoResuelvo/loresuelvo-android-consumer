package com.loresuelvo.consumer.ui.screens.home

import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal

/**
 * UDF state for the consumer Home screen.
 *
 * Two surfaces are rendered in parallel:
 *
 *  - The category grid (the primary conversion action; pre-US-54).
 *  - The "Propuestas que requieren atención" section introduced
 *    by US-54 (scenario 01-VSP).
 *
 * The global state — [Loading], [Ready], [Error] — is driven by
 * the categories round trip only: that is the action without
 * which the Home dashboard is not usable, so a categories
 * failure flips the global to [Error] and the screen surfaces
 * the retry CTA. The proposals round trip lands inside whatever
 * global state is current, exposed through
 * [pendingServiceProposals]:
 *
 *  - Loading: round trip in flight; section renders a spinner.
 *  - Ready(items): round trip succeeded; `items` may be empty
 *    (no pending proposals → section renders its own empty
 *    copy, scenario 17-VSP).
 *  - Error: round trip failed; section renders its own error
 *    copy while the categories grid keeps working.
 *
 * `categories` lives in every variant with a sensible default
 * (`CategoriesState.Loading` for the global `Loading`,
 * `CategoriesState.Error` for the global `Error`) so the
 * pre-US-54 [HomeScreen] keeps rendering the categories grid
 * identically when the proposals round trip is in flight. The
 * screen reads `state.categories` regardless of the global
 * variant; the sub-state does the right thing per branch.
 */
sealed interface HomeUiState {

    val categories: CategoriesState
    val pendingServiceProposals: ServiceProposalsState

    data class Loading(
        override val categories: CategoriesState = CategoriesState.Loading,
        override val pendingServiceProposals: ServiceProposalsState = ServiceProposalsState.Loading,
    ) : HomeUiState

    data class Ready(
        override val categories: CategoriesState,
        override val pendingServiceProposals: ServiceProposalsState,
    ) : HomeUiState

    data class Error(
        override val categories: CategoriesState = CategoriesState.Error,
        val messageResId: Int,
        override val pendingServiceProposals: ServiceProposalsState = ServiceProposalsState.Error,
    ) : HomeUiState
}

sealed interface CategoriesState {
    data object Loading : CategoriesState
    data class Ready(val items: List<Category>) : CategoriesState
    data object Error : CategoriesState
}

/**
 * Sub-state for the US-54 "Propuestas que requieren atención"
 * section. Mirrors [CategoriesState]: a `Ready` with an empty
 * [items] list means the round trip succeeded but no proposals
 * are in [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Pending]
 * — the screen renders the section's own empty copy rather than
 * the "Error" surface (see scenario 17-VSP).
 */
sealed interface ServiceProposalsState {
    data object Loading : ServiceProposalsState
    data class Ready(val items: List<ServiceProposal>) : ServiceProposalsState
    data object Error : ServiceProposalsState
}
