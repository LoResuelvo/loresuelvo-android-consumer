package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [GetAllServiceProposalsUseCase].
 *
 * The use case is the simplest in the service-proposal family: it
 * delegates to the [ServiceProposalRepository] port WITHOUT
 * applying any status filter (unlike
 * [GetPendingServiceProposalsUseCase] and
 * [GetAcceptedServiceProposalsUseCase]) so the "Mis Servicios"
 * surface can render every proposal regardless of status. Status
 * filtering on top of this base list is a presentation concern
 * (scenario 05-VSP / 06-VSP / 07-VSP).
 *
 * Failures propagate verbatim — the use case must never swallow
 * a typed [ServiceProposalsOutcome.Failure] into a fabricated
 * empty `Success`.
 */
class GetAllServiceProposalsUseCaseTest {

    private val repository = mockk<ServiceProposalRepository>()
    private val useCase = GetAllServiceProposalsUseCase(repository)

    private fun proposal(
        id: String,
        status: ServiceProposalStatus = ServiceProposalStatus.Pending,
        createdOnEpochMillis: Long = 1_699_999_000_000L,
    ): ServiceProposal = ServiceProposal(
        id = id,
        conversationId = null,
        status = status,
        counterpart = ServiceProposalCounterpart(
            id = "100",
            name = "Juan",
            surname = "Pérez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "irrelevant",
        amountCents = 15000L,
        scheduledOnEpochMillis = 1_700_000_000_000L,
        createdOnEpochMillis = createdOnEpochMillis,
    )

    @Test
    fun delegates_to_repository_and_returns_every_proposal_without_filtering() = runTest {
        val mixed = listOf(
            proposal(id = "1", status = ServiceProposalStatus.Pending),
            proposal(id = "2", status = ServiceProposalStatus.Accepted),
            proposal(id = "3", status = ServiceProposalStatus.Rejected),
            proposal(id = "4", status = ServiceProposalStatus.Pending),
        )
        coEvery { repository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(mixed)

        val outcome = useCase()

        assertTrue("expected Success, was $outcome", outcome is ServiceProposalsOutcome.Success)
        val returned = (outcome as ServiceProposalsOutcome.Success).proposals
        // The whole list must come back unchanged — no status
        // filtering happens here (that's the Pending / Accepted
        // use cases' job).
        assertEquals(listOf("1", "2", "3", "4"), returned.map { it.id })
        coVerify(exactly = 1) { repository.getServiceProposals() }
    }

    @Test
    fun empty_repository_response_yields_empty_list() = runTest {
        coEvery { repository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(emptyList())

        val outcome = useCase()

        assertTrue(outcome is ServiceProposalsOutcome.Success)
        assertEquals(emptyList<ServiceProposal>(), (outcome as ServiceProposalsOutcome.Success).proposals)
    }

    @Test
    fun propagates_network_failure_unchanged() = runTest {
        val failure = ServiceProposalsOutcome.Failure.Network(IOException("dns"))
        coEvery { repository.getServiceProposals() } returns failure

        val outcome = useCase()

        assertSame(failure, outcome)
    }

    @Test
    fun propagates_server_failure_unchanged() = runTest {
        val failure = ServiceProposalsOutcome.Failure.Server(code = 500, message = "boom")
        coEvery { repository.getServiceProposals() } returns failure

        val outcome = useCase()

        assertSame(failure, outcome)
    }

    @Test
    fun orders_proposals_by_recency_descending_by_created_on() = runTest {
        // Seed is intentionally NOT in chronological order so the
        // sort is observable; if the use case returned the list as-is
        // the assertion would fail.
        val middle = proposal(id = "mid", createdOnEpochMillis = 1_700_000_000_000L)
        val newest = proposal(id = "new", createdOnEpochMillis = 1_700_000_500_000L)
        val oldest = proposal(id = "old", createdOnEpochMillis = 1_699_999_500_000L)
        coEvery { repository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(listOf(middle, newest, oldest))

        val outcome = useCase()

        assertTrue(outcome is ServiceProposalsOutcome.Success)
        val ids = (outcome as ServiceProposalsOutcome.Success).proposals.map { it.id }
        assertEquals(
            "expected newest first, then middle, then oldest",
            listOf("new", "mid", "old"),
            ids,
        )
    }

    @Test
    fun order_is_stable_for_equal_created_on_timestamps() = runTest {
        // `sortedByDescending` is stable: two proposals with the same
        // timestamp keep the insertion order. Pin that here so a
        // future switch to an unstable sort would break the test.
        val shared = 1_700_000_000_000L
        val first = proposal(id = "first", createdOnEpochMillis = shared)
        val second = proposal(id = "second", createdOnEpochMillis = shared)
        val third = proposal(id = "third", createdOnEpochMillis = shared)
        coEvery { repository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(listOf(third, first, second))

        val outcome = useCase()

        val ids = (outcome as ServiceProposalsOutcome.Success).proposals.map { it.id }
        assertEquals(
            "expected insertion order to survive the sort for equal timestamps",
            listOf("third", "first", "second"),
            ids,
        )
    }
}
