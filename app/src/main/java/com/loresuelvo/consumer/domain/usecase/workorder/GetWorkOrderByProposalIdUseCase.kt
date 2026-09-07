package com.loresuelvo.consumer.domain.usecase.workorder

import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderRepository
import javax.inject.Inject

/**
 * Looks up the [com.loresuelvo.consumer.domain.workorder.WorkOrder]
 * tied to a proposal id. Drives the consumer work-order detail
 * screen (US-54 scenario 16-VSP). Pure passthrough — the use
 * case exists to keep the [com.loresuelvo.consumer.ui.screens.workorder.WorkOrderViewModel]
 * free of any repository import and to match the
 * one-use-case-per-action convention the rest of the app follows.
 */
class GetWorkOrderByProposalIdUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository,
) {
    suspend operator fun invoke(proposalId: String): GetWorkOrderOutcome =
        workOrderRepository.getWorkOrderByProposalId(proposalId)
}