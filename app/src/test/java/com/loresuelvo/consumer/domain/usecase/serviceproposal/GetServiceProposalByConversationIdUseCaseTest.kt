package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests for [GetServiceProposalByConversationIdUseCase].
 * Pins the three branches the conversation-summary surface
 * (US-54 scenario 14-VSP) depends on:
 *
 *  - Linked: when the seed carries a proposal with the same
 *    `conversationId`, the use case surfaces it on the outcome.
 *  - NotLinked: when no proposal matches the conversation id,
 *    the use case surfaces `NotLinked` rather than throwing or
 *    returning null.
 *  - Failure: when the repository round trip fails, the use
 *    case collapses to `Failure(failure)` so the chat can keep
 *    working without a summary card.
 */
class GetServiceProposalByConversationIdUseCaseTest {

    @Test
    fun returns_linked_when_a_proposal_matches_the_conversation_id() = runTest {
        val proposal = proposal(id = "1", conversationId = "c-100")
        val useCase = GetServiceProposalByConversationIdUseCase(
            FakeServiceProposalRepository(
                outcome = ServiceProposalsOutcome.Success(listOf(proposal)),
            ),
        )

        val outcome = useCase("c-100")

        assertTrue(
            "expected Linked, was $outcome",
            outcome is GetServiceProposalByConversationIdOutcome.Linked,
        )
        assertEquals(proposal, (outcome as GetServiceProposalByConversationIdOutcome.Linked).proposal)
    }

    @Test
    fun returns_not_linked_when_no_proposal_matches_the_conversation_id() = runTest {
        val proposal = proposal(id = "1", conversationId = "c-100")
        val useCase = GetServiceProposalByConversationIdUseCase(
            FakeServiceProposalRepository(
                outcome = ServiceProposalsOutcome.Success(listOf(proposal)),
            ),
        )

        val outcome = useCase("c-999")

        assertEquals(
            GetServiceProposalByConversationIdOutcome.NotLinked,
            outcome,
        )
    }

    @Test
    fun returns_not_linked_when_repository_succeeds_with_empty_list() = runTest {
        val useCase = GetServiceProposalByConversationIdUseCase(
            FakeServiceProposalRepository(
                outcome = ServiceProposalsOutcome.Success(emptyList()),
            ),
        )

        assertEquals(
            GetServiceProposalByConversationIdOutcome.NotLinked,
            useCase("c-100"),
        )
    }

    @Test
    fun returns_failure_when_repository_surfaces_a_failure() = runTest {
        val failure = ServiceProposalsOutcome.Failure.Server(503, "down for maintenance")
        val useCase = GetServiceProposalByConversationIdUseCase(
            FakeServiceProposalRepository(outcome = failure),
        )

        val result = useCase("c-100")

        assertTrue(
            "expected Failure, was $result",
            result is GetServiceProposalByConversationIdOutcome.Failure,
        )
        assertEquals(failure, (result as GetServiceProposalByConversationIdOutcome.Failure).failure)
    }

    @Test
    fun picks_the_first_match_when_multiple_proposals_share_the_conversation_id() = runTest {
        val first = proposal(id = "1", conversationId = "c-100")
        val second = proposal(id = "2", conversationId = "c-100")
        val useCase = GetServiceProposalByConversationIdUseCase(
            FakeServiceProposalRepository(
                outcome = ServiceProposalsOutcome.Success(listOf(first, second)),
            ),
        )

        val outcome = useCase("c-100")

        assertEquals(
            GetServiceProposalByConversationIdOutcome.Linked(first),
            outcome,
        )
    }

    @Test
    fun ignores_proposals_with_null_conversation_id() = runTest {
        val orphan = proposal(id = "1", conversationId = null)
        val linked = proposal(id = "2", conversationId = "c-100")
        val useCase = GetServiceProposalByConversationIdUseCase(
            FakeServiceProposalRepository(
                outcome = ServiceProposalsOutcome.Success(listOf(orphan, linked)),
            ),
        )

        val outcome = useCase("c-100")

        assertEquals(
            GetServiceProposalByConversationIdOutcome.Linked(linked),
            outcome,
        )
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
        )

    private class FakeServiceProposalRepository(
        private val outcome: ServiceProposalsOutcome,
    ) : ServiceProposalRepository {
        override suspend fun getServiceProposals(): ServiceProposalsOutcome = outcome
    }
}