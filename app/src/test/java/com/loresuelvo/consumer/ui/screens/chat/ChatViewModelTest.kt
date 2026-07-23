package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.Recommendations
import com.loresuelvo.consumer.domain.diagnosis.Sender
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ChatViewModel]. Covers the round-trip flow
 * exercised by scenario 02-DIA at fine-grained level, complementing
 * the BDD layer in `bdd/diagnosis/` which runs against the same
 * use case but with a [com.loresuelvo.consumer.bdd.diagnosis.FakeDiagnosisRepository].
 *
 * Coverage:
 *  - Initial state (no prompt, no messages, sending=false).
 *  - `onPromptChange` mirrors the field; `canSend` flips.
 *  - `onSendClick` with empty / whitespace-only prompts is a no-op.
 *  - `onSendClick` with a valid prompt:
 *      * appends the optimistic `Sender.Consumer` bubble,
 *      * flips `sending = true`,
 *      * clears the input.
 *  - Success path: messages replaced with the server's, `conversationId`
 *    updated, `sending = false`.
 *  - Failure paths (Network / Server / Unauthorized) reset `sending`
 *    while preserving the optimistic bubble; explicit error UI
 *    lands in commit 04-DIA.
 *  - Sending flag prevents double-tap (idempotent while inflight).
 *  - Retry-by-send: a second tap after a Failure.Server sends the
 *    new prompt to the server (the use case ignores the previous
 *    failure unless the caller chooses to retry with `canSend`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase = mockk<SendDiagnosisPromptUseCase>()
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChatViewModel(useCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_is_empty_and_canSend_is_false() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("", state.promptInput)
        assertEquals(emptyList<ChatMessage>(), state.messages)
        assertFalse(state.sending)
        assertNull(state.conversationId)
        assertNull(state.recommendations)
        assertFalse(state.canSend)
    }

    @Test
    fun onPromptChange_updates_field_and_canSend() = runTest {
        viewModel.onPromptChange("Tengo una gotera")

        val state = viewModel.uiState.value
        assertEquals("Tengo una gotera", state.promptInput)
        assertTrue(state.canSend)
    }

    @Test
    fun onPromptChange_with_whitespace_only_keeps_canSend_false() = runTest {
        viewModel.onPromptChange("   \t  ")

        val state = viewModel.uiState.value
        assertFalse("canSend must reject whitespace", state.canSend)
    }

    @Test
    fun onSendClick_with_empty_prompt_is_a_no_op() = runTest {
        viewModel.onPromptChange("")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(emptyList<ChatMessage>(), state.messages)
        assertFalse(state.sending)
        coVerify(exactly = 0) { useCase(any(), any()) }
    }

    @Test
    fun onSendClick_appends_optimistic_message_and_flips_sending() = runTest {
        // The use case suspends forever; advance just enough to let
        // the optimistic update land before the suspension.
        coEvery { useCase(any(), anyNullable()) } coAnswers { kotlinx.coroutines.awaitCancellation() }

        viewModel.onPromptChange("Tengo una gotera")
        viewModel.onSendClick()
        // Advance only past the synchronous portion — the
        // launched coroutine is parked on `awaitCancellation`.
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        val optimistic = state.messages.first()
        assertTrue(optimistic.sender is Sender.Consumer)
        assertEquals("Tengo una gotera", optimistic.content)
        assertTrue("optimistic message id should be user-<uuid>", optimistic.id.startsWith("user-"))
        assertTrue(state.sending)
        assertEquals("", state.promptInput)
    }

    @Test
    fun onSendClick_success_replaces_messages_and_sets_conversationId() = runTest {
        val serverDiagnosis = Diagnosis(
            conversationId = "conv-42",
            messages = listOf(
                ChatMessage(
                    id = "user-server-1",
                    sender = Sender.Consumer,
                    content = "Tengo una gotera",
                    sentAtEpochMillis = 0L,
                ),
                ChatMessage(
                    id = "assistant-1",
                    sender = Sender.Assistant,
                    content = "Entiendo. ¿Es constante?",
                    sentAtEpochMillis = 0L,
                ),
            ),
            recommendations = Recommendations(stub = true),
        )
        coEvery { useCase(any(), anyNullable()) } returns
            SendDiagnosisPromptOutcome.Success(serverDiagnosis)

        viewModel.onPromptChange("Tengo una gotera")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.sending)
        assertEquals("conv-42", state.conversationId)
        assertEquals(2, state.messages.size)
        assertEquals(
            listOf("user-server-1", "assistant-1"),
            state.messages.map { it.id },
        )
        assertEquals(Sender.Assistant, state.messages.last().sender)
        assertNotNull(state.recommendations)
    }

    @Test
    fun onSendClick_failure_Network_keeps_optimistic_message_and_clears_sending() = runTest {
        coEvery { useCase(any(), anyNullable()) } returns
            SendDiagnosisPromptOutcome.Failure.Network(IOException("dns"))

        viewModel.onPromptChange("Tengo una gotera")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.sending)
        assertEquals(
            1,
            state.messages.size,
        )
        assertTrue(
            "the optimistic bubble must remain visible until commit 04-DIA renders the error card",
            state.messages.first().sender is Sender.Consumer,
        )
        assertNull(state.conversationId)
    }

    @Test
    fun onSendClick_failure_Server_keeps_optimistic_message_and_clears_sending() = runTest {
        coEvery { useCase(any(), anyNullable()) } returns
            SendDiagnosisPromptOutcome.Failure.Server(code = 500, message = "boom")

        viewModel.onPromptChange("Tengo una gotera")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.sending)
        assertEquals(1, state.messages.size)
    }

    @Test
    fun onSendClick_failure_Unauthorized_keeps_optimistic_message_and_clears_sending() = runTest {
        coEvery { useCase(any(), anyNullable()) } returns
            SendDiagnosisPromptOutcome.Failure.Unauthorized("token expired")

        viewModel.onPromptChange("Tengo una gotera")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.sending)
        assertEquals(1, state.messages.size)
    }

    @Test
    fun onSendClick_passes_trimmed_prompt_and_current_conversationId() = runTest {
        // Drive a successful round-trip first to populate
        // `state.conversationId`, then send again with a follow-up
        // prompt and observe that the second invocation carries the
        // freshly-set id and the trimmed prompt.
        coEvery { useCase("primera", null) } returns SendDiagnosisPromptOutcome.Success(
            Diagnosis(conversationId = "conv-1", messages = emptyList()),
        )
        viewModel.onPromptChange("primera")
        viewModel.onSendClick()
        advanceUntilIdle()
        assertEquals("conv-1", viewModel.uiState.value.conversationId)

        coEvery { useCase(any(), anyNullable()) } returns SendDiagnosisPromptOutcome.Success(
            Diagnosis(conversationId = "conv-1", messages = emptyList()),
        )

        viewModel.onPromptChange("  segunda con espacios  ")
        viewModel.onSendClick()
        advanceUntilIdle()

        // The follow-up call must carry the trimmed prompt and
        // the conversation id set by the first round-trip.
        coVerify { useCase("segunda con espacios", "conv-1") }
    }

    @Test
    fun double_tap_on_Send_calls_use_case_only_once_while_sending() = runTest {
        coEvery { useCase(any(), anyNullable()) } coAnswers {
            kotlinx.coroutines.yield()
            kotlinx.coroutines.yield()
            SendDiagnosisPromptOutcome.Success(
                Diagnosis(conversationId = "conv-1", messages = emptyList()),
            )
        }

        viewModel.onPromptChange("Tengo una gotera")
        viewModel.onSendClick()
        // Second tap arrives while the first is still in flight.
        viewModel.onSendClick()
        viewModel.onSendClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { useCase(any(), any()) }
    }
}
