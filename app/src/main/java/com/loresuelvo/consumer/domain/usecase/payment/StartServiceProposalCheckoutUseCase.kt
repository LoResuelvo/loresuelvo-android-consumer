package com.loresuelvo.consumer.domain.usecase.payment

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import javax.inject.Inject

/**
 * One-shot use case that starts (or reuses) a Mercado Pago
 * Checkout Pro session for the booking deposit of a service
 * proposal (US-21 confirm-agreement flow).
 *
 * Thin pass-through over [CheckoutSessionRepository.startServiceProposalCheckout]
 * that exists to keep the call site consistent with the rest of
 * the project (every VM depends on a use case, not a repository
 * directly). Pure / no state / no side effects.
 */
class StartServiceProposalCheckoutUseCase @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
) {
    suspend operator fun invoke(serviceProposalId: Int): CheckoutSessionOutcome =
        checkoutSessionRepository.startServiceProposalCheckout(serviceProposalId)
}
