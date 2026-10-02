package com.loresuelvo.consumer.domain.usecase.payment

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import javax.inject.Inject

class StartServiceProposalCheckoutUseCase @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
) {
    suspend operator fun invoke(serviceProposalId: Int): CheckoutSessionOutcome =
        checkoutSessionRepository.startServiceProposalCheckout(serviceProposalId)
}
