package com.loresuelvo.consumer.ui.screens.serviceagreement

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.PaymentIntent
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome

/**
 * UDF state for the service-agreement confirmation screen
 * (US-21). The screen has two phases:
 *
 *  1. **Review the agreement** — render the immutable proposal
 *     details (description, amount, scheduled time, deposit
 *     amount when available) and offer a "Confirm" CTA.
 *  2. **Start the deposit checkout** — open a Mercado Pago
 *     Custom Tab with the returned `checkout_url` and capture
 *     the `payment_intent_id` for the post-redirect polling loop.
 *
 * The state machine below mirrors those two phases plus loading
 * and error branches. The error branches share copy with the
 * rest of the consumer app via
 * [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailScreen]
 * conventions; the CTA labels are in `values/strings.xml` to
 * keep the BDD step regexes language-agnostic.
 */
sealed interface ServiceAgreementUiState {
    val serviceProposalId: Int

    data class Loading(
        override val serviceProposalId: Int,
    ) : ServiceAgreementUiState

    /**
     * Ready-to-review state. The proposal carries the immutable
     * terms (description, amount, scheduled time) and the
     * [depositCents] is non-null whenever the backend supplied a
     * `booking_payment_deadline` (only then the deposit is
     * effectively owed to the provider). The CTA "Confirm" is
     * shown only when [depositCents] != null.
     */
    data class Ready(
        override val serviceProposalId: Int,
        val proposal: ServiceProposal,
        val providerName: String,
        val depositCents: Long?,
        val currency: String,
    ) : ServiceAgreementUiState

    /**
     * The user tapped "Confirm"; we are talking to the API to
     * start (or reuse) the checkout session. The CTA is disabled
     * and a `CircularProgressIndicator` replaces it so a rapid
     * double-tap cannot re-issue the same POST.
     */
    data class StartingCheckout(
        override val serviceProposalId: Int,
        val proposal: ServiceProposal,
        val providerName: String,
        val depositCents: Long?,
        val currency: String,
    ) : ServiceAgreementUiState

    /**
     * The checkout session was created (or reused) and the screen
     * has the `payment_intent_id` to take to the result route. The
     * Custom Tab should have been launched by the VM right before
     * the state was emitted; the screen never displays anything
     * but emits a navigation event so the host can move the
     * `NavController` to the result route (which owns the polling
     * loop).
     */
    data class CheckoutReady(
        override val serviceProposalId: Int,
        val paymentIntentId: String,
        val checkoutUrl: String,
    ) : ServiceAgreementUiState

    data class NetworkError(
        override val serviceProposalId: Int,
        val cause: Throwable,
    ) : ServiceAgreementUiState

    data class ServerError(
        override val serviceProposalId: Int,
        val code: Int,
        val message: String,
    ) : ServiceAgreementUiState

    data class AlreadyPaid(
        override val serviceProposalId: Int,
        val message: String,
    ) : ServiceAgreementUiState
}

/**
 * One-shot events emitted by [ServiceAgreementViewModel]. The host
 * (`LoResuelvoNav`) consumes them via `viewModel.events.collect`
 * to fire `navController.navigate(...)`. Events are NOT part of
 * the UDF state — they are signals the screen fires once, then
 * the VM drops them from its internal channel.
 */
sealed interface ServiceAgreementEvent {
    /**
     * The user just confirmed the agreement and a Mercado Pago
     * Checkout Pro session is now live. The host opens the
     * `checkoutUrl` in a Custom Tab and navigates to the
     * payment-result route keyed on [paymentIntentId].
     */
    data class OpenCheckout(
        val checkoutUrl: String,
        val paymentIntentId: String,
    ) : ServiceAgreementEvent

    /**
     * The user backed out before confirming (cancelled the
     * confirmation dialog). The host pops the back stack so the
     * caller (ProposalDetail) stays on screen.
     */
    data object Cancelled : ServiceAgreementEvent
}

/**
 * Outcome of a `GET /service-proposals` round-trip that the
 * agreement screen piggy-backs on. Wraps the same
 * [ServiceProposalsOutcome] the rest of the consumer app uses;
 * kept here so the screen can decode failures consistently with
 * `ProposalDetailScreen`.
 */
sealed interface AgreementLoadOutcome {
    data class Loaded(val proposal: ServiceProposal) : AgreementLoadOutcome
    data class Network(val cause: Throwable) : AgreementLoadOutcome
    data class Server(val code: Int, val message: String) : AgreementLoadOutcome
    data object NotFound : AgreementLoadOutcome
}
