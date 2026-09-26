package com.loresuelvo.consumer.domain.usecase.workorder

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests for [GetWorkOrderDetailUseCase] and the
 * [WorkOrderDetailRepository] adapter contract it depends on.
 * US-54 scenario 16-VSP pins the work-order detail surface;
 * these tests guard the data layer so the screen can rely on a
 * typed outcome. US-27 keeps them green while migrating the
 * surface to the new `GET /work-orders/{workOrderID}` endpoint.
 */
class GetWorkOrderDetailUseCaseTest {

    private fun workOrder(
        proposalId: String = "wo-1",
        description: String = "Fuga en el lavamanos",
        amountCents: Long = 1_500_000L,
        scheduledOnEpochMillis: Long = 1_792_074_600_000L,
        acceptedOnEpochMillis: Long = 1_788_434_364_640L,
    ): WorkOrderDetail = WorkOrderDetail(
        proposalId = proposalId,
        provider = WorkOrderDetailCounterpart(
            id = "100",
            name = "Carlos",
            surname = "López",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = description,
        amountCents = amountCents,
        scheduledOnEpochMillis = scheduledOnEpochMillis,
        acceptedOnEpochMillis = acceptedOnEpochMillis,
        paidOnEpochMillis = null,
        status = TurnoStatus.Pending,
        completionReport = null,
        review = null,
        estimatedDurationMinutes = 90,
    )

    @Test
    fun returns_found_when_work_order_matches() = runTest {
        val useCase = GetWorkOrderDetailUseCase(
            FakeWorkOrderDetailRepository(workOrder(proposalId = "wo-1")),
        )

        val outcome = useCase("wo-1")

        assertTrue(
            "expected Found, was $outcome",
            outcome is GetWorkOrderOutcome.Found,
        )
        val found = (outcome as GetWorkOrderOutcome.Found).workOrder
        assertEquals("wo-1", found.proposalId)
        assertEquals("Plomería", found.provider.categoryName)
        assertEquals("Fuga en el lavamanos", found.description)
        assertEquals(1_500_000L, found.amountCents)
        assertEquals(90, found.estimatedDurationMinutes)
        assertEquals(TurnoStatus.Pending, found.status)
        assertEquals("Carlos López", "${found.provider.name} ${found.provider.surname}")
    }

    @Test
    fun returns_not_found_when_id_does_not_match() = runTest {
        val useCase = GetWorkOrderDetailUseCase(
            FakeWorkOrderDetailRepository(workOrder(proposalId = "wo-1")),
        )

        assertEquals(
            GetWorkOrderOutcome.NotFound,
            useCase("wo-999"),
        )
    }

    @Test
    fun returns_not_found_when_repository_has_no_work_order() = runTest {
        val useCase = GetWorkOrderDetailUseCase(
            FakeWorkOrderDetailRepository(),
        )

        assertEquals(
            GetWorkOrderOutcome.NotFound,
            useCase("wo-1"),
        )
    }

    @Test
    fun returns_failure_when_repository_surfaces_a_failure() = runTest {
        val failure = ServiceProposalsOutcome.Failure.Server(500, "down for maintenance")
        val useCase = GetWorkOrderDetailUseCase(
            FakeWorkOrderDetailRepository(failure = failure),
        )

        val result = useCase("wo-1")

        assertTrue(
            "expected Failure, was $result",
            result is GetWorkOrderOutcome.Failure,
        )
        assertEquals(failure, (result as GetWorkOrderOutcome.Failure).failure)
    }

    @Test
    fun work_order_without_estimated_duration_surfaces_null() = runTest {
        val detail = workOrder().copy(estimatedDurationMinutes = null)
        val useCase = GetWorkOrderDetailUseCase(
            FakeWorkOrderDetailRepository(detail),
        )

        val outcome = useCase("wo-1")

        assertTrue(
            "expected Found, was $outcome",
            outcome is GetWorkOrderOutcome.Found,
        )
        assertEquals(null, (outcome as GetWorkOrderOutcome.Found).workOrder.estimatedDurationMinutes)
    }

    /**
     * Port-level fake. Holds a single `WorkOrderDetail` or a
     * failure to assert the typed outcomes the use case passes
     * through unchanged.
     */
    private class FakeWorkOrderDetailRepository(
        private val detail: WorkOrderDetail? = null,
        private val failure: ServiceProposalsOutcome.Failure? = null,
    ) : WorkOrderDetailRepository {
        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome {
            failure?.let { return GetWorkOrderOutcome.Failure(it) }
            return detail
                ?.takeIf { it.proposalId == workOrderId }
                ?.let { GetWorkOrderOutcome.Found(it) }
                ?: GetWorkOrderOutcome.NotFound
        }
    }
}
