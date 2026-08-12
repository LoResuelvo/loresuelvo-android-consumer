package com.loresuelvo.consumer.domain.usecase.jobrequest

import com.loresuelvo.consumer.domain.jobrequest.AiJobRequestRepository
import com.loresuelvo.consumer.domain.jobrequest.CreateAiJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the contract of [CreateAiJobRequestUseCase]:
 *  - delegates to [AiJobRequestRepository.createAiJobRequest] with
 *    the supplied `conversationId` and `providerId` on the happy
 *    path and returns the repository's success outcome;
 *  - short-circuits with a [CreateAiJobRequestOutcome.Failure.Server]
 *    (`code = 0`, synthetic non-HTTP) when `conversationId` is
 *    blank or `providerId <= 0`, mirroring the
 *    `CreateJobRequestUseCase` guard pattern;
 *  - propagates the repository's typed failures (Network / Server
 *    / Unauthorized) unchanged.
 *
 * The pre-fill semantics — the backend's AI fills `title` and
 * `description` — live in the endpoint, so the use case itself
 * has no string-level validation: it only protects the URL
 * parameters from a malformed caller (e.g. a VM that hasn't
 * resolved the conversation id yet).
 */
class CreateAiJobRequestUseCaseTest {

    private val repository: AiJobRequestRepository = mockk()
    private val useCase = CreateAiJobRequestUseCase(repository)

    @Test
    fun `forwards conversationId and providerId to the repository and returns its success`() = runTest {
        val expected = JobRequest(
            id = "1",
            conversationId = "10",
            title = "Reparación de fuga en la cocina",
            description = "Hola Ana, necesito reparar una fuga de agua.",
            status = "pending",
            images = emptyList(),
        )
        coEvery { repository.createAiJobRequest(any(), any()) } returns
            CreateAiJobRequestOutcome.Success(expected)

        val outcome = useCase(conversationId = "10", providerId = 1073741824)

        assertTrue(outcome is CreateAiJobRequestOutcome.Success)
        assertEquals(expected, (outcome as CreateAiJobRequestOutcome.Success).jobRequest)
    }

    @Test
    fun `passes the supplied conversationId and providerId through to the repository unchanged`() = runTest {
        val capturedConversationId = slot<String>()
        val capturedProviderId = slot<Int>()
        coEvery {
            repository.createAiJobRequest(capture(capturedConversationId), capture(capturedProviderId))
        } returns
            CreateAiJobRequestOutcome.Success(
                JobRequest(
                    id = "1",
                    conversationId = "10",
                    title = "x",
                    description = "y",
                    status = "pending",
                    images = emptyList(),
                ),
            )

        useCase(conversationId = "10", providerId = 1073741824)

        assertEquals("10", capturedConversationId.captured)
        assertEquals(1073741824, capturedProviderId.captured)
    }

    @Test
    fun `returns Server failure when conversationId is blank after trim`() = runTest {
        val outcome = useCase(conversationId = "   ", providerId = 1)

        assertTrue(outcome is CreateAiJobRequestOutcome.Failure.Server)
        assertEquals(0, (outcome as CreateAiJobRequestOutcome.Failure.Server).code)
        coVerify(exactly = 0) { repository.createAiJobRequest(any(), any()) }
    }

    @Test
    fun `returns Server failure when providerId is zero`() = runTest {
        val outcome = useCase(conversationId = "10", providerId = 0)

        assertTrue(outcome is CreateAiJobRequestOutcome.Failure.Server)
        assertEquals(0, (outcome as CreateAiJobRequestOutcome.Failure.Server).code)
        coVerify(exactly = 0) { repository.createAiJobRequest(any(), any()) }
    }

    @Test
    fun `returns Server failure when providerId is negative`() = runTest {
        val outcome = useCase(conversationId = "10", providerId = -1)

        assertTrue(outcome is CreateAiJobRequestOutcome.Failure.Server)
        assertEquals(0, (outcome as CreateAiJobRequestOutcome.Failure.Server).code)
        coVerify(exactly = 0) { repository.createAiJobRequest(any(), any()) }
    }

    @Test
    fun `propagates Network failure from the repository unchanged`() = runTest {
        val cause = RuntimeException("dns")
        coEvery { repository.createAiJobRequest(any(), any()) } returns
            CreateAiJobRequestOutcome.Failure.Network(cause)

        val outcome = useCase(conversationId = "10", providerId = 1)

        assertTrue(outcome is CreateAiJobRequestOutcome.Failure.Network)
        assertEquals(cause, (outcome as CreateAiJobRequestOutcome.Failure.Network).cause)
    }

    @Test
    fun `propagates Unexpected-token-style Unauthorized failure from the repository unchanged`() = runTest {
        coEvery { repository.createAiJobRequest(any(), any()) } returns
            CreateAiJobRequestOutcome.Failure.Unauthorized("token expired")

        val outcome = useCase(conversationId = "10", providerId = 1)

        assertTrue(outcome is CreateAiJobRequestOutcome.Failure.Unauthorized)
        assertEquals("token expired", (outcome as CreateAiJobRequestOutcome.Failure.Unauthorized).message)
    }

    @Test
    fun `propagates Server failure with the backend code and message unchanged`() = runTest {
        coEvery { repository.createAiJobRequest(any(), any()) } returns
            CreateAiJobRequestOutcome.Failure.Server(code = 502, message = "upstream down")

        val outcome = useCase(conversationId = "10", providerId = 1)

        assertTrue(outcome is CreateAiJobRequestOutcome.Failure.Server)
        val failure = outcome as CreateAiJobRequestOutcome.Failure.Server
        assertEquals(502, failure.code)
        assertEquals("upstream down", failure.message)
    }
}
