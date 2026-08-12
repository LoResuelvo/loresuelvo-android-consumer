package com.loresuelvo.consumer.domain.usecase.assistant

import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.assistant.AiConversationRepository
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the contract of [GetAiConversationsUseCase]:
 *  - delegates to [AiConversationRepository.getConversations] and
 *    returns the repository's outcomes unchanged.
 *
 * The use case is procedural today (just a pass-through to the
 * repository) — the test is short on purpose. The future
 * "filter by diagnosis concluded" / "trim to the last N"
 * rules will land here and these tests will grow to cover them.
 */
class GetAiConversationsUseCaseTest {

    private val repository: AiConversationRepository = mockk()
    private val useCase = GetAiConversationsUseCase(repository)

    @Test
    fun `forwards the repository's success list unchanged`() = runTest {
        val expected = AiConversationListOutcome.Success(
            conversations = listOf(
                AiConversationSummary(
                    id = "1",
                    title = "Pérdida de agua en la cocina",
                    lastMessageAtEpochMillis = 1_716_080_400_000L,
                    lastMessagePreview = "Revisá si el agua sale desde la rosca del sifón.",
                ),
            ),
        )
        coEvery { repository.getConversations() } returns expected

        val outcome = useCase()

        assertTrue(outcome is AiConversationListOutcome.Success)
        assertEquals(expected, outcome)
    }

    @Test
    fun `forwards the repository's failure unchanged`() = runTest {
        val expected = AiConversationListOutcome.Failure.Server(
            code = 500,
            message = "boom",
        )
        coEvery { repository.getConversations() } returns expected

        val outcome = useCase()

        assertEquals(expected, outcome)
    }

    @Test
    fun `invokes the repository exactly once`() = runTest {
        coEvery { repository.getConversations() } returns
            AiConversationListOutcome.Success(conversations = emptyList())

        useCase()

        coVerify(exactly = 1) { repository.getConversations() }
    }
}
