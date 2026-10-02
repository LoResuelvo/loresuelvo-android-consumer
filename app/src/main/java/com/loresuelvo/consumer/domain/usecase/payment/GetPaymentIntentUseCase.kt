package com.loresuelvo.consumer.domain.usecase.payment

import com.loresuelvo.consumer.domain.payment.GetPaymentIntentOutcome
import com.loresuelvo.consumer.domain.payment.PaymentIntentRepository
import javax.inject.Inject

class GetPaymentIntentUseCase @Inject constructor(
    private val paymentIntentRepository: PaymentIntentRepository,
) {
    suspend operator fun invoke(paymentIntentId: String): GetPaymentIntentOutcome =
        paymentIntentRepository.get(paymentIntentId)
}
