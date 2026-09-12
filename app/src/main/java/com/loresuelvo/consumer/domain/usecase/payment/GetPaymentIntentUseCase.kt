package com.loresuelvo.consumer.domain.usecase.payment

import com.loresuelvo.consumer.domain.payment.GetPaymentIntentOutcome
import com.loresuelvo.consumer.domain.payment.PaymentIntentRepository
import javax.inject.Inject

/**
 * One-shot use case that polls the current status of a payment
 * intent (US-21 confirm-agreement flow + US-28 service-balance
 * flow). The caller passes a `paymentIntentId` obtained from the
 * initial checkout response; the use case returns the latest
 * status as a typed [GetPaymentIntentOutcome].
 *
 * Thin pass-through over [PaymentIntentRepository.get]. Exists to
 * keep the VM depending on a use case (project convention) rather
 * than a repository directly. Pure / no state / no side effects.
 */
class GetPaymentIntentUseCase @Inject constructor(
    private val paymentIntentRepository: PaymentIntentRepository,
) {
    suspend operator fun invoke(paymentIntentId: String): GetPaymentIntentOutcome =
        paymentIntentRepository.get(paymentIntentId)
}
