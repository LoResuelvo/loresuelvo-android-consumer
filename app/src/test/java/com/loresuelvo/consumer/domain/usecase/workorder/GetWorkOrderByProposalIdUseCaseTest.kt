package com.loresuelvo.consumer.domain.usecase.workorder

import com.loresuelvo.consumer.data.api.ApiWorkOrderMapping
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNull as junitAssertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests for [GetWorkOrderByProposalIdUseCase] and
 * the [WorkOrderRepository] adapter contract it depends on.
 * US-54 scenario 16-VSP pins the work-order detail surface;
 * these tests guard the data layer so the screen can rely on a
 * typed outcome.
 */
class GetWorkOrderByProposalIdUseCaseTest {

    @Test
    fun returns_found_when_proposal_matches() = runTest {
        val proposal = proposal(id = "wo-1", conversationId = "c-1")
        val useCase = GetWorkOrderByProposalIdUseCase(
            FakeWorkOrderRepository(ServiceProposalsOutcome.Success(listOf(proposal))),
        )

        val outcome = useCase("wo-1")

        assertTrue(
            "expected Found, was $outcome",
            outcome is GetWorkOrderOutcome.Found,
        )
        val found = (outcome as GetWorkOrderOutcome.Found).workOrder
        assertEquals("wo-1", found.proposalId)
        assertEquals("Plomería", found.categoryName)
        assertEquals("Fuga en el lavamanos", found.description)
        assertEquals(1500000L, found.amountCents)
        assertEquals(45, found.estimatedDurationMinutes)
        assertEquals(ServiceProposalStatus.Pending, found.status)
        assertEquals("Carlos López", found.providerName)
    }

    @Test
    fun returns_not_found_when_no_proposal_matches_the_id() = runTest {
        val proposal = proposal(id = "wo-1", conversationId = "c-1")
        val useCase = GetWorkOrderByProposalIdUseCase(
            FakeWorkOrderRepository(ServiceProposalsOutcome.Success(listOf(proposal))),
        )

        assertEquals(
            GetWorkOrderOutcome.NotFound,
            useCase("wo-999"),
        )
    }

    @Test
    fun returns_not_found_when_repository_succeeds_with_empty_list() = runTest {
        val useCase = GetWorkOrderByProposalIdUseCase(
            FakeWorkOrderRepository(ServiceProposalsOutcome.Success(emptyList())),
        )

        assertEquals(
            GetWorkOrderOutcome.NotFound,
            useCase("wo-1"),
        )
    }

    @Test
    fun returns_failure_when_repository_surfaces_a_failure() = runTest {
        val failure = ServiceProposalsOutcome.Failure.Server(500, "down for maintenance")
        val useCase = GetWorkOrderByProposalIdUseCase(
            FakeWorkOrderRepository(failure),
        )

        val result = useCase("wo-1")

        assertTrue(
            "expected Failure, was $result",
            result is GetWorkOrderOutcome.Failure,
        )
        assertEquals(failure, (result as GetWorkOrderOutcome.Failure).failure)
    }

    @Test
    fun proposal_without_estimated_duration_surfaces_null_in_the_work_order() = runTest {
        val proposal = proposal(id = "wo-1", conversationId = "c-1").copy(
            estimatedDurationMinutes = null,
        )
        val useCase = GetWorkOrderByProposalIdUseCase(
            FakeWorkOrderRepository(ServiceProposalsOutcome.Success(listOf(proposal))),
        )

        val outcome = useCase("wo-1")

        assertTrue(
            "expected Found, was $outcome",
            outcome is GetWorkOrderOutcome.Found,
        )
        junitAssertNull((outcome as GetWorkOrderOutcome.Found).workOrder.estimatedDurationMinutes)
    }

    private fun proposal(id: String, conversationId: String?): ServiceProposal =
        ServiceProposal(
            id = id,
            conversationId = conversationId,
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "100",
                name = "Carlos",
                surname = "López",
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
            description = "Fuga en el lavamanos",
            amountCents = 1500000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
            createdOnEpochMillis = 1_788_434_364_640L,
            estimatedDurationMinutes = 45,
        )

    /**
     * In-memory [WorkOrderRepository] that runs the same lookup
     * logic the production adapter uses (the proposal repo round
     * trip plus the [ApiWorkOrderMapping.toWorkOrder] conversion),
     * so the tests pin the end-to-end mapping without depending
     * on Hilt or the network.
     */
    private class FakeWorkOrderRepository(
        private val outcome: ServiceProposalsOutcome,
    ) : WorkOrderRepository {
        private val delegate = FakeServiceProposalRepository(outcome)

        override suspend fun getWorkOrderByProposalId(proposalId: String): GetWorkOrderOutcome =
            when (val result = delegate.getServiceProposals()) {
                is ServiceProposalsOutcome.Success ->
                    result.proposals
                        .firstOrNull { it.id == proposalId }
                        ?.let { GetWorkOrderOutcome.Found(ApiWorkOrderMapping.toWorkOrder(it)) }
                        ?: GetWorkOrderOutcome.NotFound
                is ServiceProposalsOutcome.Failure -> GetWorkOrderOutcome.Failure(result)
            }
    }

    private class FakeServiceProposalRepository(
        private val outcome: ServiceProposalsOutcome,
    ) : ServiceProposalRepository {
        override suspend fun getServiceProposals(): ServiceProposalsOutcome = outcome
    }
}