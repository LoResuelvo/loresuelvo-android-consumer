package com.loresuelvo.consumer.domain.usecase.conversation

import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [GetConversationsUseCase]. The use case is a thin
 * orchestrator: it must delegate to the port and propagate the
 * typed outcome verbatim, without swallowing or reshaping
 * failures.
 *
 * Mirrors the discipline of `GetCategoriesUseCaseTest`.
 */
class GetConversationsUseCaseTest {

    private val repository = mockk<ConversationRepository>()
    private val useCase = GetConversationsUseCase(repository)

    private val sampleConversation = Conversation(
        id = "1",
        status = ConversationStatus.Pending,
        counterpart = ConversationCounterpart(
            id = 20,
            name = "Juan",
            surname = "Gómez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        lastMessage = null,
        updatedOnEpochMillis = 0L,
    )

    @Test
    fun delegates_to_repository_and_returns_success() = runTest {
        coEvery { repository.getConversations() } returns
            ConversationsOutcome.Success(listOf(sampleConversation))

        val outcome = useCase()

        assertTrue(outcome is ConversationsOutcome.Success)
        assertEquals(
            listOf(sampleConversation),
            (outcome as ConversationsOutcome.Success).conversations,
        )
        coVerify(exactly = 1) { repository.getConversations() }
    }

    @Test
    fun delegates_to_repository_and_returns_empty_success() = runTest {
        coEvery { repository.getConversations() } returns
            ConversationsOutcome.Success(emptyList())

        val outcome = useCase()

        assertTrue(outcome is ConversationsOutcome.Success)
        assertEquals(
            emptyList<Conversation>(),
            (outcome as ConversationsOutcome.Success).conversations,
        )
    }

    @Test
    fun propagates_network_failure() = runTest {
        val cause = IOException("dns")
        coEvery { repository.getConversations() } returns
            ConversationsOutcome.Failure.Network(cause)

        val outcome = useCase()

        assertTrue(outcome is ConversationsOutcome.Failure.Network)
        // IOException does not override `equals`, so identity comparison
        // is the only reliable assertion (the cause itself is wrapped
        // verbatim by the use case — no copy / transformation).
        assertTrue(
            (outcome as ConversationsOutcome.Failure.Network).cause === cause,
        )
    }

    @Test
    fun propagates_server_failure() = runTest {
        coEvery { repository.getConversations() } returns
            ConversationsOutcome.Failure.Server(503, "unavailable")

        val outcome = useCase()

        assertTrue(outcome is ConversationsOutcome.Failure.Server)
        assertEquals(503, (outcome as ConversationsOutcome.Failure.Server).code)
    }

    @Test
    fun propagates_unauthorized_failure() = runTest {
        coEvery { repository.getConversations() } returns
            ConversationsOutcome.Failure.Unauthorized("token expired")

        val outcome = useCase()

        assertTrue(outcome is ConversationsOutcome.Failure.Unauthorized)
        assertEquals(
            "token expired",
            (outcome as ConversationsOutcome.Failure.Unauthorized).message,
        )
    }
}