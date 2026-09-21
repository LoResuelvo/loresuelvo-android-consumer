package com.loresuelvo.consumer.domain.usecase.payment

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import javax.inject.Inject

/**
 * Thin pass-through over
 * [CheckoutSessionRepository.startWorkOrderCheckout] so the
 * work-order detail screen / VM can depend on the use case
 * instead of the repository (matches the
 * one-use-case-per-action convention the rest of the app
 * follows). US-27 `visualize-turns-detail` scenario 09-VTD.
 */
class StartWorkOrderCheckoutUseCase @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
) {
    suspend operator fun invoke(workOrderId: Int): CheckoutSessionOutcome =
        checkoutSessionRepository.startWorkOrderCheckout(workOrderId)
}
