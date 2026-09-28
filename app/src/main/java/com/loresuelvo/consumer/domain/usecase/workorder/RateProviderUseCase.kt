package com.loresuelvo.consumer.domain.usecase.workorder

import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import javax.inject.Inject

/**
 * Submits the consumer's rating and optional comment for a paid
 * [com.loresuelvo.consumer.domain.workorder.WorkOrderDetail]
 * (US-30 `calify-provider-service`). Drives
 * `POST /work-orders/{workOrderID}/review` through the
 * [WorkOrderDetailRepository] port.
 *
 * Pure passthrough — the use case exists to keep the
 * [com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailViewModel]
 * free of any repository import and to match the
 * one-use-case-per-action convention the rest of the app follows.
 *
 * The repository is responsible for translating transport and
 * HTTP failures into the typed [SubmitWorkOrderReviewOutcome]
 * variants; the use case never swallows errors and never throws.
 *
 * @param workOrderId the id of the paid work order being
 *   reviewed (the screen reads this off the route argument).
 * @param rating integer in `1..5`. The UI composer enforces the
 *   range before invoking the use case.
 * @param description free-form comment; empty / blank is
 *   permitted (the consumer can rate without commenting).
 */
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
