package com.loresuelvo.consumer.domain.diagnosis

import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.diagnosis.LoadAiConversationOutcome
import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the contract of [LoadAiConversationUseCase]:
 *  - delegates to [DiagnosisRepository.getAiConversation] with the
 *    supplied conversationId and returns the repository's
 *    success outcome;
 *  - short-circuits with a [LoadAiConversationOutcome.Failure.Server]
 *    (`code = 0`, synthetic non-HTTP) when the conversationId is
 *    blank, mirroring the established repo-guard pattern;
 *  - propagates the repository's typed failures (Network /
 *    Server / Unauthorized) unchanged.
 *
 * The use case is the bridge between the AI session list (where
 * the consumer taps a row to resume) and the chat scroll
 * (which loads the saved messages). Tests are TDD-ordered: the
 * contract is pinned first, then the implementation lands.
 */
class LoadAiConversationUseCaseTest {

    private val repository: DiagnosisRepository = mockk()
    private val useCase = LoadAiConversationUseCase(repository)

    private fun sampleDiagnosis(): Diagnosis = Diagnosis(
        conversationId = "10",
        messages = listOf(
            com.loresuelvo.consumer.domain.diagnosis.ChatMessage(
                id = "1",
                sender = com.loresuelvo.consumer.domain.diagnosis.Sender.Consumer,
                content = "Tengo una gotera",
                sentAtEpochMillis = 0L,
            ),
            com.loresuelvo.consumer.domain.diagnosis.ChatMessage(
                id = "2",
                sender = com.loresuelvo.consumer.domain.diagnosis.Sender.Assistant,
                content = "Revisá el sifón.",
                sentAtEpochMillis = 0L,
            ),
        ),
        assessment = null,
        recommendedProviders = emptyList<Provider>(),
    )

    @Test
    fun `forwards the conversationId to the repository and returns its success`() = runTest {
        val expected = LoadAiConversationOutcome.Success(sampleDiagnosis())
        coEvery { repository.getAiConversation(any()) } returns expected

        val outcome = useCase(conversationId = "10")

        assertTrue(outcome is LoadAiConversationOutcome.Success)
        assertEquals(expected, outcome)
    }

    @Test
    fun `passes the supplied conversationId through to the repository unchanged`() = runTest {
        val captured = slot<String>()
        coEvery { repository.getAiConversation(capture(captured)) } returns
            LoadAiConversationOutcome.Success(sampleDiagnosis())

        useCase(conversationId = "10")

        assertEquals("10", captured.captured)
    }

    @Test
    fun `returns Server failure when conversationId is blank after trim`() = runTest {
        val outcome = useCase(conversationId = "")

        assertTrue(outcome is LoadAiConversationOutcome.Failure.Server)
        assertEquals(0, (outcome as LoadAiConversationOutcome.Failure.Server).code)
        coVerify(exactly = 0) { repository.getAiConversation(any()) }
    }

    @Test
    fun `returns Server failure when conversationId is whitespace`() = runTest {
        val outcome = useCase(conversationId = "   ")

        assertTrue(outcome is LoadAiConversationOutcome.Failure.Server)
        assertEquals(0, (outcome as LoadAiConversationOutcome.Failure.Server).code)
        coVerify(exactly = 0) { repository.getAiConversation(any()) }
    }

    @Test
    fun `propagates Network failure from the repository unchanged`() = runTest {
        val cause = RuntimeException("dns")
        coEvery { repository.getAiConversation(any()) } returns
            LoadAiConversationOutcome.Failure.Network(cause)

        val outcome = useCase(conversationId = "10")

        assertTrue(outcome is LoadAiConversationOutcome.Failure.Network)
        assertEquals(cause, (outcome as LoadAiConversationOutcome.Failure.Network).cause)
    }

    @Test
    fun `propagates Unauthorized failure from the repository unchanged`() = runTest {
        coEvery { repository.getAiConversation(any()) } returns
            LoadAiConversationOutcome.Failure.Unauthorized("token expired")

        val outcome = useCase(conversationId = "10")

        assertTrue(outcome is LoadAiConversationOutcome.Failure.Unauthorized)
        assertEquals("token expired", (outcome as LoadAiConversationOutcome.Failure.Unauthorized).message)
    }

    @Test
    fun `propagates Server failure with the backend code and message unchanged`() = runTest {
        coEvery { repository.getAiConversation(any()) } returns
            LoadAiConversationOutcome.Failure.Server(code = 502, message = "upstream down")

        val outcome = useCase(conversationId = "10")

        assertTrue(outcome is LoadAiConversationOutcome.Failure.Server)
        val failure = outcome as LoadAiConversationOutcome.Failure.Server
        assertEquals(502, failure.code)
        assertEquals("upstream down", failure.message)
    }
}
