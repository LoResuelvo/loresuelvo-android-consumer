package com.loresuelvo.consumer.domain.usecase.workorder

import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import javax.inject.Inject

/**
 * Looks up the [com.loresuelvo.consumer.domain.workorder.WorkOrderDetail]
 * tied to a work-order id. Drives the consumer work-order detail
 * screen (US-54 scenario 16-VSP, US-27 `visualize-turns-detail`).
 * Pure passthrough — the use case exists to keep the
 * [com.loresuelvo.consumer.ui.screens.workorder.WorkOrderViewModel]
 * free of any repository import and to match the
 * one-use-case-per-action convention the rest of the app follows.
 */
class GetWorkOrderDetailUseCase @Inject constructor(
    private val workOrderDetailRepository: WorkOrderDetailRepository,
) {
    suspend operator fun invoke(workOrderId: String): GetWorkOrderOutcome =
        workOrderDetailRepository.getWorkOrderDetail(workOrderId)
}