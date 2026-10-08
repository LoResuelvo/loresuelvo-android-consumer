package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeWorkOrderDetailRepository @Inject constructor() : WorkOrderDetailRepository {
    private data class Seed(
        val workOrderId: String,
        val workOrder: WorkOrderDetail,
    )

    private var seed: Seed? = null
    private var nextSubmitOutcome: SubmitWorkOrderReviewOutcome =
        SubmitWorkOrderReviewOutcome.Server(
            code = 0,
            message = "no outcome queued",
        )

    var failure: ServiceProposalsOutcome.Failure? = null

    var lastSubmission: Submission? = null
        private set
    var lastRequestedWorkOrderId: String? = null
        private set

    data class Submission(
        val workOrderId: String,
        val rating: Int,
        val description: String,
    )

    /**
     * Stamps the [WorkOrderDetail] returned for [workOrderId]. Pass
     * `null` to force [NotFound] after provider metadata is resolved.
     */
    fun set(workOrderId: String, workOrder: WorkOrderDetail?) {
        seed = workOrder?.let { Seed(workOrderId, it) }
    }

    /**
     * Stamps the next [submitReview] call's outcome. Failure-path
     * tests pin a [SubmitWorkOrderReviewOutcome.Server] here
     * before tapping "Enviar" so the VM surfaces the typed
     * error stamp on the composer.
     */
    fun enqueueSubmitOutcome(outcome: SubmitWorkOrderReviewOutcome) {
        nextSubmitOutcome = outcome
    }

    override suspend fun getWorkOrderDetail(
        workOrderId: String,
        provider: WorkOrderDetailCounterpart?,
    ): GetWorkOrderOutcome {
        lastRequestedWorkOrderId = workOrderId
        failure?.let { return GetWorkOrderOutcome.Failure(it) }
        if (provider == null) {
            return GetWorkOrderOutcome.Failure(
                ServiceProposalsOutcome.Failure.Server(0, "provider metadata missing"),
            )
        }
        val current = seed ?: return GetWorkOrderOutcome.NotFound
        if (current.workOrderId != workOrderId) return GetWorkOrderOutcome.NotFound
        return GetWorkOrderOutcome.Found(current.workOrder.copy(provider = provider))
    }

    override suspend fun submitReview(
        workOrderId: String,
        rating: Int,
        description: String,
    ): SubmitWorkOrderReviewOutcome {
        lastSubmission = Submission(workOrderId, rating, description)
        return nextSubmitOutcome
    }
}
