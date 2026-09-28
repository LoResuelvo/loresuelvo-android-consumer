package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test-only [WorkOrderDetailRepository] for the acceptance /
 * integration tests that `@UninstallModules(RepositoryModule::class)`.
 *
 * Mirrors `FakeTurnosRepository` — the production binding lives in
 * `com.loresuelvo.consumer.di.RepositoryModule`; once a test
 * uninstalls that module it must rebind every port the
 * ViewModels under test transitively depend on.
 *
 * Default seed: `NotFound`. Tests that need a populated
 * detail call [set] from the main thread before
 * `scenario.recreate()` so the new seed lands in the same
 * `@Singleton` instance the activity resolves through
 * `hiltViewModel()`.
 *
 * US-30 `calify-provider-service`: also records the last
 * `submitReview` call (US-30 instrumentation wire-assertion)
 * and lets the failure-path tests pin a queued outcome via
 * [enqueueSubmitOutcome] before the Enviar tap lands.
 */
@Singleton
class FakeWorkOrderDetailRepository @Inject constructor() : WorkOrderDetailRepository {
    private var seed: WorkOrderDetail? = null
    private var nextSubmitOutcome: SubmitWorkOrderReviewOutcome =
        SubmitWorkOrderReviewOutcome.Server(
            code = 0,
            message = "no outcome queued",
        )

    var lastSubmission: Submission? = null
        private set

    data class Submission(
        val workOrderId: String,
        val rating: Int,
        val description: String,
    )

    /**
     * Stamps the [WorkOrderDetail] the next [getWorkOrderDetail]
     * call returns. Pass `null` to force [NotFound] (the default).
     */
    fun set(workOrder: WorkOrderDetail?) {
        seed = workOrder
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
        val current = seed ?: return GetWorkOrderOutcome.NotFound
        if (provider == null) return GetWorkOrderOutcome.NotFound
        return if (current.proposalId == workOrderId) {
            GetWorkOrderOutcome.Found(current)
        } else {
            GetWorkOrderOutcome.NotFound
        }
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
