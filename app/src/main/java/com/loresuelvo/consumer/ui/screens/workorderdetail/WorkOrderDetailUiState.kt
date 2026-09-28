package com.loresuelvo.consumer.ui.screens.workorderdetail

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail

/**
 * UDF state for the work-order detail screen (US-54 scenario
 * 16-VSP, US-27 `visualize-turns-detail`, US-30
 * `calify-provider-service`).
 *
 * Modelled as a sealed hierarchy so the screen renders exactly
 * one branch without boolean flags — mirrors
 * [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState].
 *
 *  - [Loading] — initial fetch in flight.
 *  - [Ready] — work order loaded; the screen renders every
 *    pinned field (monto, fecha, descripción, duración estimada,
 *    estado) plus the provider's name and category. Also carries
 *    the [composer] review-collection state (see US-30): when the
 *    work order is `paid` and has no review on file, the screen
 *    surfaces a "Calificar servicio" CTA that opens the
 *    [ReviewComposerState.Editing] form; once the consumer files
 *    a review, the VM flips [composer] back to [Hidden] and the
 *    read-only review section takes over.
 *  - [NotFound] — no proposal matches the given id; the screen
 *    renders a not-found copy.
 *  - [Error] — fetch failed; the typed failure lets the screen
 *    render network vs server strings distinctly and offer a
 *    retry CTA.
 */
sealed interface WorkOrderDetailUiState {
    data object Loading : WorkOrderDetailUiState
    data class Ready(
        val workOrder: WorkOrderDetail,
        val composer: ReviewComposerState = ReviewComposerState.Hidden,
    ) : WorkOrderDetailUiState
    data object NotFound : WorkOrderDetailUiState
    data class Error(val failure: ServiceProposalsOutcome.Failure) : WorkOrderDetailUiState
}

/**
 * State for the "Calificar servicio" composer surfaced in the
 * `paid + no review` branch of [WorkOrderDetailUiState.Ready]
 * (US-30 `calify-provider-service`).
 *
 *  - [Hidden] — composer is collapsed; the screen shows the
 *    "Calificar servicio" CTA instead. Default after `load()`
 *    settles into a paid work order without a review.
 *  - [Editing] — the consumer opened the form. Carries the
 *    draft ([ratingDraft] in `1..5`, [descriptionDraft] free-form),
 *    whether a network round-trip is in flight ([submitting]),
 *    and the latest typed failure so the composer can render an
 *    inline error message without re-deriving the HTTP status.
 *    The submit button stays disabled while
 *    `ratingDraft == null || submitting` so the consumer cannot
 *    ship an empty rating; [descriptionDraft] is capped to 500
 *    characters at the data-layer mapper (the composer enforces
 *    the same cap on the input side).
 *
 * On [SubmitWorkOrderReviewOutcome.AlreadyReviewed] the VM
 * flips back to [Hidden] and merges the existing review into
 * [WorkOrderDetailUiState.Ready.workOrder] — same UX as a fresh
 * successful submit, since the backend is signalling "the review
 * is already on file".
 */
sealed interface ReviewComposerState {
    data object Hidden : ReviewComposerState

    data class Editing(
        val ratingDraft: Int? = null,
        val descriptionDraft: String = "",
        val submitting: Boolean = false,
        val error: SubmitWorkOrderReviewOutcome? = null,
    ) : ReviewComposerState {
        /** True while the composer can ship a valid submission. */
        val canSubmit: Boolean
            get() = !submitting && ratingDraft != null
    }
}
