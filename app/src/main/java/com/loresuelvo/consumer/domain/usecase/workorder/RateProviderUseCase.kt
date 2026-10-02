package com.loresuelvo.consumer.domain.usecase.workorder

import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import javax.inject.Inject

class RateProviderUseCase @Inject constructor(
    private val workOrderDetailRepository: WorkOrderDetailRepository,
) {
    suspend operator fun invoke(
        workOrderId: String,
        rating: Int,
        description: String,
    ): SubmitWorkOrderReviewOutcome =
        workOrderDetailRepository.submitReview(
            workOrderId = workOrderId,
            rating = rating,
            description = description,
        )
}
