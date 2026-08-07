package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
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
 * Unit tests for [ConversationViewModel] — load + prompt flow.
 * Companion file
 * `ConversationViewModelSendRetryTest.kt` covers the
 * send / retry / dismiss surface.
 *
 * Mirrors the discipline of `ChatViewModelTest`: fine-grained
 * state coverage that complements the BDD layer in
 * `bdd/message/`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getConversationById = mockk<GetConversationByIdUseCase>()
    private val sendMessage = mockk<SendMessageUseCase>()
    private lateinit var viewModel: ConversationViewModel

    private fun detail(
        id: String = "1",
        status: ConversationStatus = ConversationStatus.Pending,
        messages: List<ConversationMessage> = emptyList(),
    ) = ConversationDetail(
        id = id,
        status = status,
        counterpart = ConversationCounterpart(
            id = 20L,
            name = "Juan",
            surname = "Gómez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        messages = messages,
        updatedOnEpochMillis = 0L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- load() ---------------------------------------------------------

    @Test
    fun initial_state_is_Loading_until_load_fires() = runTest {
        // Park the use case so the initial Loading state is observable
        // before any round-trip lands.
        coEvery { getConversationById("1") } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }

        viewModel = ConversationViewModel(getConversationById, sendMessage)
        advanceUntilIdle()

        assertEquals(ConversationUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun load_with_success_transitions_to_Ready_with_detail() = runTest {
        val detail = detail(id = "1")
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail)

        viewModel = ConversationViewModel(getConversationById, sendMessage)
        viewModel.load("1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("expected Ready, was $state", state is ConversationUiState.Ready)
        assertEquals(detail, (state as ConversationUiState.Ready).detail)
        assertEquals("", state.promptInput)
        assertFalse(state.sending)
        assertNull(state.transientError)
        assertNull(state.lastAttemptedPrompt)
    }

    @Test
    fun load_with_server_failure_transitions_to_Error() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Failure.Server(500, "boom")

        viewModel = ConversationViewModel(getConversationById, sendMessage)
        viewModel.load("1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("expected Error, was $state", state is ConversationUiState.Error)
        val failure = (state as ConversationUiState.Error).failure
        assertTrue(failure is ConversationDetailOutcome.Failure.Server)
        assertEquals(500, (failure as ConversationDetailOutcome.Failure.Server).code)
    }

    @Test
    fun load_with_network_failure_transitions_to_Error_carrying_cause() = runTest {
        val cause = IOException("dns")
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Failure.Network(cause)

        viewModel = ConversationViewModel(getConversationById, sendMessage)
        viewModel.load("1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ConversationUiState.Error)
        val failure = (state as ConversationUiState.Error).failure
        assertTrue(failure is ConversationDetailOutcome.Failure.Network)
        assertTrue(
            (failure as ConversationDetailOutcome.Failure.Network).cause === cause,
        )
    }

    // ---- onPromptChange --------------------------------------------------

    @Test
    fun onPromptChange_updates_field_on_Ready() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(getConversationById, sendMessage)
        viewModel.load("1")
        advanceUntilIdle()

        viewModel.onPromptChange("hola")

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals("hola", state.promptInput)
    }

    @Test
    fun onPromptChange_outside_Ready_is_a_no_op() = runTest {
        coEvery { getConversationById("1") } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }
        viewModel = ConversationViewModel(getConversationById, sendMessage)
        advanceUntilIdle()
        assertEquals(ConversationUiState.Loading, viewModel.uiState.value)

        viewModel.onPromptChange("cualquier cosa")

        assertEquals(ConversationUiState.Loading, viewModel.uiState.value)
    }
}