package com.loresuelvo.consumer.domain.diagnosis.usecase

import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [SendDiagnosisPromptUseCase]. Six single-behaviour
 * cases, mirroring the convention of
 * `RegisterConsumerUseCaseTest` and
 * `GetCategoriesUseCaseTest`.
 *
 * Coverage:
 *  - empty / whitespace-only prompts short-circuit to
 *    [SendDiagnosisPromptOutcome.Failure.Server] (defensive guard
 *    so the backend never receives an empty body).
 *  - the prompt is trimmed BEFORE delegation.
 *  - typed failures (Network / Server / Unauthorized) from the
 *    repository are propagated unchanged — the use case is a
 *    pass-through guard, NOT a swallowing translator.
 */
class SendDiagnosisPromptUseCaseTest {

    private val diagnosisRepository = mockk<DiagnosisRepository>()
    private val useCase = SendDiagnosisPromptUseCase(diagnosisRepository)

    @Test
    fun returns_Server_zero_when_prompt_is_empty() = runTest {
        val outcome = useCase(prompt = "")

        assertTrue(outcome is SendDiagnosisPromptOutcome.Failure.Server)
        assertEquals(0, (outcome as SendDiagnosisPromptOutcome.Failure.Server).code)
        coVerify(exactly = 0) { diagnosisRepository.sendPrompt(any(), any()) }
    }

    @Test
    fun returns_Server_zero_when_prompt_is_only_whitespace() = runTest {
        val outcome = useCase(prompt = "   \n  \t  ")

        assertTrue(outcome is SendDiagnosisPromptOutcome.Failure.Server)
        assertEquals(0, (outcome as SendDiagnosisPromptOutcome.Failure.Server).code)
        coVerify(exactly = 0) { diagnosisRepository.sendPrompt(any(), any()) }
    }

    @Test
    fun delegates_to_repository_with_trimmed_prompt() = runTest {
        coEvery {
            diagnosisRepository.sendPrompt("Tengo una gotera", null)
        } returns SendDiagnosisPromptOutcome.Success(
            Diagnosis(conversationId = "conv-1", messages = emptyList()),
        )

        val outcome = useCase(prompt = "  Tengo una gotera  ")

        assertTrue(outcome is SendDiagnosisPromptOutcome.Success)
        coVerify(exactly = 1) {
            diagnosisRepository.sendPrompt(
                content = "Tengo una gotera",
                existingConversationId = null,
            )
        }
    }

    @Test
    fun forwards_existingConversationId_to_repository_on_follow_ups() = runTest {
        coEvery {
            diagnosisRepository.sendPrompt("segunda", "conv-1")
        } returns SendDiagnosisPromptOutcome.Success(
            Diagnosis(conversationId = "conv-1", messages = emptyList()),
        )

        val outcome = useCase(
            prompt = "segunda",
            existingConversationId = "conv-1",
        )

        assertTrue(outcome is SendDiagnosisPromptOutcome.Success)
        coVerify(exactly = 1) {
            diagnosisRepository.sendPrompt(
                content = "segunda",
                existingConversationId = "conv-1",
            )
        }
    }

    @Test
    fun propagates_Failure_Network_from_repository() = runTest {
        val cause = IOException("dns")
        coEvery { diagnosisRepository.sendPrompt(any(), any()) } returns
            SendDiagnosisPromptOutcome.Failure.Network(cause)

        val outcome = useCase(prompt = "Tengo una gotera")

        assertTrue(outcome is SendDiagnosisPromptOutcome.Failure.Network)
        assertEquals(cause, (outcome as SendDiagnosisPromptOutcome.Failure.Network).cause)
    }

    @Test
    fun propagates_Failure_Server_from_repository_with_code_and_message() = runTest {
        coEvery { diagnosisRepository.sendPrompt(any(), any()) } returns
            SendDiagnosisPromptOutcome.Failure.Server(code = 503, message = "unavailable")

        val outcome = useCase(prompt = "Tengo una gotera")

        assertTrue(outcome is SendDiagnosisPromptOutcome.Failure.Server)
        val failure = outcome as SendDiagnosisPromptOutcome.Failure.Server
        assertEquals(503, failure.code)
        assertEquals("unavailable", failure.message)
    }

    @Test
    fun propagates_Failure_Unauthorized_from_repository() = runTest {
        coEvery { diagnosisRepository.sendPrompt(any(), any()) } returns
            SendDiagnosisPromptOutcome.Failure.Unauthorized("token expired")

        val outcome = useCase(prompt = "Tengo una gotera")

        assertTrue(outcome is SendDiagnosisPromptOutcome.Failure.Unauthorized)
        assertEquals(
            "token expired",
            (outcome as SendDiagnosisPromptOutcome.Failure.Unauthorized).message,
        )
    }
}
