package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.data.api.WebSocketClient
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMediaMessageUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import com.loresuelvo.consumer.data.media.MediaMetadataRetrieverReader
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Send / retry / dismiss coverage for [ConversationViewModel].
 * Companion file `ConversationViewModelTest.kt` covers the load
 * + prompt surface. Splitting the two files keeps each within the
 * team's per-commit LoC budget.
 *
 * The setup here repeats the minimal seed from
 * `ConversationViewModelTest`: a Ready state with the given
 * `getConversationById` stubbed to Success.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelSendRetryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getConversationById = mockk<GetConversationByIdUseCase>()
    private val sendMessage = mockk<SendMessageUseCase>()
    private val sendMediaMessage = mockk<SendMediaMessageUseCase>(relaxed = true)
    private val mediaReader = mockk<MediaReader>(relaxed = true)
    private val mediaMetadataRetriever = mockk<MediaMetadataRetrieverReader>(relaxed = true)
    private val webSocketClient = mockk<WebSocketClient>(relaxed = true)
    private lateinit var viewModel: ConversationViewModel

    private fun serverMessage(id: String = "10", content: String = "hola") =
        ConversationMessage(
            id = id,
            sender = ConversationSender.Consumer,
            content = content,
            createdOnEpochMillis = 1_700_000_000_000L,
        )

    private fun driveIntoReady() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(
                com.loresuelvo.consumer.domain.conversation.ConversationDetail(
                    id = "1",
                    status = com.loresuelvo.consumer.domain.conversation.ConversationStatus.Pending,
                    counterpart = com.loresuelvo.consumer.domain.conversation.ConversationCounterpart(
                        id = 20L,
                        name = "Juan",
                        surname = "Gómez",
                        categoryName = "Plomería",
                        profilePhotoUrl = null,
                    ),
                    messages = emptyList(),
                    updatedOnEpochMillis = 0L,
                ),
            )
        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, mediaMetadataRetriever, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- onPromptChange → onSendClick: prompt-clearing semantics -------

    @Test
    fun onPromptChange_after_failure_clears_transientError() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(
                com.loresuelvo.consumer.domain.conversation.ConversationDetail(
                    id = "1",
                    status = com.loresuelvo.consumer.domain.conversation.ConversationStatus.Pending,
                    counterpart = com.loresuelvo.consumer.domain.conversation.ConversationCounterpart(
                        id = 20L,
                        name = "Juan",
                        surname = "Gómez",
                        categoryName = "Plomería",
                        profilePhotoUrl = null,
                    ),
                    messages = emptyList(),
                    updatedOnEpochMillis = 0L,
                ),
            )
        coEvery { sendMessage("1", "primera") } returns
            SendMessageOutcome.Failure.Server(500, "boom")
        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, mediaMetadataRetriever, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()
        viewModel.onPromptChange("primera")
        viewModel.onSendClick()
        advanceUntilIdle()
        val readyWithError = viewModel.uiState.value as ConversationUiState.Ready
        assertTrue(readyWithError.transientError != null)

        viewModel.onPromptChange("primera editada")

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals("primera editada", state.promptInput)
        assertNull(
            "transientError must clear as soon as the user edits the prompt",
            state.transientError,
        )
    }

    // ---- onSendClick: guards -------------------------------------------

    @Test
    fun onSendClick_with_empty_prompt_is_a_no_op() = runTest {
        driveIntoReady()

        viewModel.onPromptChange("")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(emptyList<ConversationMessage>(), state.detail.messages)
        assertFalse(state.sending)
        coVerify(exactly = 0) { sendMessage(any(), any()) }
    }

    @Test
    fun onSendClick_with_whitespace_only_prompt_is_a_no_op() = runTest {
        driveIntoReady()

        viewModel.onPromptChange("   \t  ")
        viewModel.onSendClick()
        advanceUntilIdle()

        coVerify(exactly = 0) { sendMessage(any(), any()) }
    }

    @Test
    fun double_tap_on_Send_calls_use_case_only_once_while_sending() = runTest {
        driveIntoReady()
        coEvery { sendMessage("1", "hola") } coAnswers {
            kotlinx.coroutines.yield()
            kotlinx.coroutines.yield()
            SendMessageOutcome.Success(serverMessage(id = "10", content = "hola"))
        }

        viewModel.onPromptChange("hola")
        viewModel.onSendClick()
        viewModel.onSendClick()
        viewModel.onSendClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { sendMessage("1", "hola") }
    }

    // ---- onSendClick: optimistic + success -----------------------------

    @Test
    fun onSendClick_with_valid_prompt_clears_input_and_flips_sending() = runTest {
        driveIntoReady()
        coEvery { sendMessage("1", "hola") } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }

        viewModel.onPromptChange("hola")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertTrue(state.sending)
        assertEquals("", state.promptInput)
        assertEquals("hola", state.lastAttemptedPrompt)
        assertNull(state.transientError)
    }

    @Test
    fun onSendClick_success_appends_server_message_to_detail() = runTest {
        driveIntoReady()
        val serverMessage = serverMessage(id = "10", content = "hola")
        coEvery { sendMessage("1", "hola") } returns
            SendMessageOutcome.Success(serverMessage)

        viewModel.onPromptChange("hola")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertFalse(state.sending)
        assertEquals(listOf(serverMessage), state.detail.messages)
        assertNull(state.transientError)
        assertNull(
            "lastAttemptedPrompt clears on success",
            state.lastAttemptedPrompt,
        )
    }

    // ---- onSendClick: failure paths -------------------------------------

    @Test
    fun onSendClick_failure_Network_surfaces_transientError_and_preserves_lastAttemptedPrompt() = runTest {
        driveIntoReady()
        coEvery { sendMessage("1", "hola") } returns
            SendMessageOutcome.Failure.Network(IOException("dns"))

        viewModel.onPromptChange("hola")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertFalse(state.sending)
        assertTrue(
            "expected transientError, was ${state.transientError}",
            state.transientError is SendMessageOutcome.Failure.Network,
        )
        assertEquals("hola", state.lastAttemptedPrompt)
        assertEquals(
            "no optimistic append — the bubble lands only on Success",
            emptyList<ConversationMessage>(),
            state.detail.messages,
        )
    }

    @Test
    fun onSendClick_failure_Server_keeps_state_consistent() = runTest {
        driveIntoReady()
        coEvery { sendMessage("1", "hola") } returns
            SendMessageOutcome.Failure.Server(500, "boom")

        viewModel.onPromptChange("hola")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertFalse(state.sending)
        assertTrue(state.transientError is SendMessageOutcome.Failure.Server)
        assertEquals("hola", state.lastAttemptedPrompt)
    }

    @Test
    fun onSendClick_failure_Unauthorized_keeps_state_consistent() = runTest {
        driveIntoReady()
        coEvery { sendMessage("1", "hola") } returns
            SendMessageOutcome.Failure.Unauthorized("token expired")

        viewModel.onPromptChange("hola")
        viewModel.onSendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertTrue(state.transientError is SendMessageOutcome.Failure.Unauthorized)
    }

    // ---- onRetryClick ---------------------------------------------------

    @Test
    fun onRetryClick_after_failure_resends_lastAttemptedPrompt_and_clears_transientError() = runTest {
        driveIntoReady()
        coEvery { sendMessage("1", "primera") } returns
            SendMessageOutcome.Failure.Server(500, "boom") andThen
            SendMessageOutcome.Success(serverMessage(id = "10", content = "primera"))

        viewModel.onPromptChange("primera")
        viewModel.onSendClick()
        advanceUntilIdle()
        val readyWithError = viewModel.uiState.value as ConversationUiState.Ready
        assertTrue(readyWithError.transientError is SendMessageOutcome.Failure.Server)
        assertEquals("primera", readyWithError.lastAttemptedPrompt)

        viewModel.onRetryClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertNull("transientError must clear once retry fires", state.transientError)
        assertFalse(state.sending)
        assertEquals(listOf("10"), state.detail.messages.map { it.id })
        coVerify(exactly = 2) { sendMessage("1", "primera") }
    }

    @Test
    fun onRetryClick_with_no_previous_failure_is_a_no_op() = runTest {
        driveIntoReady()

        viewModel.onRetryClick()
        advanceUntilIdle()

        coVerify(exactly = 0) { sendMessage(any(), any()) }
    }

    @Test
    fun onRetryClick_while_sending_is_a_no_op() = runTest {
        driveIntoReady()
        coEvery { sendMessage("1", "hola") } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }
        viewModel.onPromptChange("hola")
        viewModel.onSendClick()
        advanceUntilIdle()
        assertTrue((viewModel.uiState.value as ConversationUiState.Ready).sending)

        viewModel.onRetryClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { sendMessage("1", "hola") }
    }

    // ---- onErrorDismiss -------------------------------------------------

    @Test
    fun onErrorDismiss_clears_transientError_without_resending() = runTest {
        driveIntoReady()
        coEvery { sendMessage("1", "primera") } returns
            SendMessageOutcome.Failure.Server(500, "boom")

        viewModel.onPromptChange("primera")
        viewModel.onSendClick()
        advanceUntilIdle()
        assertTrue(
            (viewModel.uiState.value as ConversationUiState.Ready).transientError != null,
        )

        viewModel.onErrorDismiss()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertNull(state.transientError)
        assertEquals(
            "lastAttemptedPrompt is kept so the retry CTA can still fire",
            "primera",
            state.lastAttemptedPrompt,
        )
        coVerify(exactly = 1) { sendMessage("1", "primera") }
    }
}