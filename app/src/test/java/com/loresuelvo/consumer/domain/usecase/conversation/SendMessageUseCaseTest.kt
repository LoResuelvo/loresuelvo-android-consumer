package com.loresuelvo.consumer.domain.usecase.conversation

import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SendMessageUseCase]. The use case has a single
 * transformation of its own (the empty-content rule) and must
 * delegate everything else verbatim to the repository.
 *
 * Coverage:
 *  - Empty / whitespace-only content short-circuits to a
 *    synthetic `Failure.Server(0, …)` WITHOUT calling the
 *    repository.
 *  - Non-empty content delegates the TRIMMED value to the
 *    repository.
 *  - The repository's typed outcomes (Success / Network /
 *    Server / Unauthorized) propagate unchanged.
 */
class SendMessageUseCaseTest {

    private val repository = mockk<ConversationRepository>()
    private val useCase = SendMessageUseCase(repository)

    private val sentMessage = ConversationMessage(
        id = "10",
        sender = ConversationSender.Consumer,
        content = "hola",
        createdOnEpochMillis = 1_700_000_000_000L,
    )

    @Test
    fun empty_content_short_circuits_with_synthetic_Server_failure_without_calling_repo() = runTest {
        val outcome = useCase("1", "")

        assertTrue(outcome is SendMessageOutcome.Failure.Server)
        val failure = outcome as SendMessageOutcome.Failure.Server
        assertEquals(0, failure.code)
        coVerify(exactly = 0) { repository.sendMessage(any(), any()) }
    }

    @Test
    fun whitespace_only_content_short_circuits() = runTest {
        val outcome = useCase("1", "   \t  ")

        assertTrue(outcome is SendMessageOutcome.Failure.Server)
        coVerify(exactly = 0) { repository.sendMessage(any(), any()) }
    }

    @Test
    fun non_empty_content_delegates_trimmed_value_and_returns_success() = runTest {
        coEvery { repository.sendMessage("1", "hola") } returns
            SendMessageOutcome.Success(sentMessage)

        val outcome = useCase("1", "  hola  ")

        assertTrue(outcome is SendMessageOutcome.Success)
        assertEquals(
            sentMessage,
            (outcome as SendMessageOutcome.Success).message,
        )
        coVerify(exactly = 1) { repository.sendMessage("1", "hola") }
    }

    @Test
    fun propagates_network_failure() = runTest {
        val cause = IOException("dns")
        coEvery { repository.sendMessage("1", "hola") } returns
            SendMessageOutcome.Failure.Network(cause)

        val outcome = useCase("1", "hola")

        assertTrue(outcome is SendMessageOutcome.Failure.Network)
        assertTrue(
            (outcome as SendMessageOutcome.Failure.Network).cause === cause,
        )
    }

    @Test
    fun propagates_server_failure() = runTest {
        coEvery { repository.sendMessage("1", "hola") } returns
            SendMessageOutcome.Failure.Server(500, "boom")

        val outcome = useCase("1", "hola")

        assertTrue(outcome is SendMessageOutcome.Failure.Server)
        val failure = outcome as SendMessageOutcome.Failure.Server
        assertEquals(500, failure.code)
        assertEquals("boom", failure.message)
    }

    @Test
    fun propagates_unauthorized_failure() = runTest {
        coEvery { repository.sendMessage("1", "hola") } returns
            SendMessageOutcome.Failure.Unauthorized("token expired")

        val outcome = useCase("1", "hola")

        assertTrue(outcome is SendMessageOutcome.Failure.Unauthorized)
        assertEquals(
            "token expired",
            (outcome as SendMessageOutcome.Failure.Unauthorized).message,
        )
    }

    @Test
    fun delegates_conversationId_verbatim() = runTest {
        coEvery { repository.sendMessage("42", "hola") } returns
            SendMessageOutcome.Success(sentMessage)

        useCase("42", "hola")

        coVerify(exactly = 1) { repository.sendMessage("42", "hola") }
    }
}