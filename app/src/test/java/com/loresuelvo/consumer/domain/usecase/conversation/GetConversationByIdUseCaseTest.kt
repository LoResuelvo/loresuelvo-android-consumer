package com.loresuelvo.consumer.domain.usecase.conversation

import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [GetConversationByIdUseCase]. The use case is a
 * thin orchestrator: it must delegate to the port and propagate
 * the typed outcome verbatim, without reshaping failures.
 *
 * Mirrors the discipline of `GetConversationsUseCaseTest`.
 */
class GetConversationByIdUseCaseTest {

    private val repository = mockk<ConversationRepository>()
    private val useCase = GetConversationByIdUseCase(repository)

    private val sampleDetail = ConversationDetail(
        id = "1",
        status = ConversationStatus.Pending,
        counterpart = ConversationCounterpart(
            id = 20L,
            name = "Juan",
            surname = "Gómez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        messages = emptyList(),
        updatedOnEpochMillis = 0L,
    )

    @Test
    fun delegates_to_repository_and_returns_success() = runTest {
        coEvery { repository.getConversationById("1") } returns
            ConversationDetailOutcome.Success(sampleDetail)

        val outcome = useCase("1")

        assertTrue(outcome is ConversationDetailOutcome.Success)
        assertEquals(
            sampleDetail,
            (outcome as ConversationDetailOutcome.Success).detail,
        )
        coVerify(exactly = 1) { repository.getConversationById("1") }
    }

    @Test
    fun delegates_conversationId_verbatim() = runTest {
        coEvery { repository.getConversationById("42") } returns
            ConversationDetailOutcome.Success(sampleDetail)

        useCase("42")

        coVerify(exactly = 1) { repository.getConversationById("42") }
    }

    @Test
    fun propagates_network_failure() = runTest {
        val cause = IOException("dns")
        coEvery { repository.getConversationById("1") } returns
            ConversationDetailOutcome.Failure.Network(cause)

        val outcome = useCase("1")

        assertTrue(outcome is ConversationDetailOutcome.Failure.Network)
        assertTrue(
            (outcome as ConversationDetailOutcome.Failure.Network).cause === cause,
        )
    }

    @Test
    fun propagates_server_failure() = runTest {
        coEvery { repository.getConversationById("1") } returns
            ConversationDetailOutcome.Failure.Server(500, "boom")

        val outcome = useCase("1")

        assertTrue(outcome is ConversationDetailOutcome.Failure.Server)
        val failure = outcome as ConversationDetailOutcome.Failure.Server
        assertEquals(500, failure.code)
        assertEquals("boom", failure.message)
    }

    @Test
    fun propagates_unauthorized_failure() = runTest {
        coEvery { repository.getConversationById("1") } returns
            ConversationDetailOutcome.Failure.Unauthorized("token expired")

        val outcome = useCase("1")

        assertTrue(outcome is ConversationDetailOutcome.Failure.Unauthorized)
        assertEquals(
            "token expired",
            (outcome as ConversationDetailOutcome.Failure.Unauthorized).message,
        )
    }

    @Test
    fun list_summary_unchanged_after_detail_load() = runTest {
        // The list endpoint and the detail endpoint share the
        // same Conversation base fields. This is a documentation
        // test — it pins that the use case does NOT mutate the
        // detail's status / counterpart when delegating.
        coEvery { repository.getConversationById("1") } returns
            ConversationDetailOutcome.Success(sampleDetail)

        val outcome = useCase("1")
        val detail = (outcome as ConversationDetailOutcome.Success).detail

        // Status and counterpart fields are preserved end-to-end.
        assertEquals(ConversationStatus.Pending, detail.status)
        assertEquals("Juan", detail.counterpart.name)
        // The detail's fields can be projected back into the
        // list-summary `Conversation` type without loss —
        // pinning the projection keeps the contract explicit.
        val projected: Conversation = detail.let {
            Conversation(
                id = it.id,
                status = it.status,
                counterpart = it.counterpart,
                lastMessage = null,
                updatedOnEpochMillis = it.updatedOnEpochMillis,
            )
        }
        assertEquals(detail.id, projected.id)
    }
}