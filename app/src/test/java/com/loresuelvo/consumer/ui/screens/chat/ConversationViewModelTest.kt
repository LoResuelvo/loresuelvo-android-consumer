package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.data.api.WebSocketClient
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.realtime.WsEvent
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMediaMessageUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val sendMediaMessage = mockk<SendMediaMessageUseCase>(relaxed = true)
    private val mediaReader = mockk<MediaReader>(relaxed = true)
    private val webSocketClient = mockk<WebSocketClient>(relaxed = true)
    private lateinit var webSocketEvents: MutableSharedFlow<WsEvent>
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
        // The VM's init {} subscribes to the WebSocket flow. Stub
        // the flow as a MutableSharedFlow the test can push events
        // into; relaxed-mock the rest of WebSocketClient so the
        // VM's `start()` call doesn't throw.
        webSocketEvents = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        every { webSocketClient.events } returns webSocketEvents
        every { webSocketClient.start() } returns Unit
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

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        advanceUntilIdle()

        assertEquals(ConversationUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun load_with_success_transitions_to_Ready_with_detail() = runTest {
        val detail = detail(id = "1")
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail)

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
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

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
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

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
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

    @Test
    fun rebuilding_VM_and_reloading_surfaces_persisted_messages() = runTest {
        // Scenario 06-IC: the user leaves the conversation screen
        // (Hilt discards the NavBackStackEntry → the VM is gone)
        // and re-enters it (new VM, fresh `load`). The previously
        // sent message must surface in the new VM's state because
        // the backend persisted it. This test pins that contract:
        // a second `load()` call against a freshly-built VM
        // replaces the state with whatever the backend returns,
        // including the persisted bubble.
        val initialDetail = com.loresuelvo.consumer.domain.conversation.ConversationDetail(
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
        )
        val persistedMessage = com.loresuelvo.consumer.domain.conversation.ConversationMessage(
            id = "42",
            sender = com.loresuelvo.consumer.domain.conversation.ConversationSender.Consumer,
            content = "Hola Juan, necesito una mano",
            createdOnEpochMillis = 1_700_000_000_000L,
        )
        val rehydratedDetail = initialDetail.copy(
            messages = listOf(persistedMessage),
        )

        // First load returns the empty thread (brand-new conversation).
        coEvery { getConversationById("1") } returnsMany listOf(
            ConversationDetailOutcome.Success(initialDetail),
            ConversationDetailOutcome.Success(rehydratedDetail),
        )

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()
        val firstState = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(emptyList<Any>(), firstState.detail.messages)

        // Discard the VM (simulates navigation away — Hilt discards
        // the NavBackStackEntry, the VM goes out of scope). Build a
        // fresh one and re-load; the backend returns the persisted
        // message that the consumer sent before navigating away.
        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()

        val rehydratedState = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(
            "the persisted bubble must surface after re-entry",
            listOf(persistedMessage),
            rehydratedState.detail.messages,
        )
    }

    // ---- onPromptChange --------------------------------------------------

    @Test
    fun onPromptChange_updates_field_on_Ready() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
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
        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        advanceUntilIdle()
        assertEquals(ConversationUiState.Loading, viewModel.uiState.value)

        viewModel.onPromptChange("cualquier cosa")

        assertEquals(ConversationUiState.Loading, viewModel.uiState.value)
    }

    // ---- WebSocket event subscription ----------------------------

    private fun providerEvent(
        conversationId: Long = 1,
        messageId: String = "100",
        content: String = "Hola desde el provider",
    ) = WsEvent(
        type = WsEvent.CONVERSATION_MESSAGE_CREATED,
        conversationId = conversationId,
        message = ConversationMessage(
            id = messageId,
            sender = ConversationSender.Provider,
            content = content,
            createdOnEpochMillis = 1_700_000_000_000L,
        ),
    )

    @Test
    fun ws_event_for_current_conversation_is_appended_to_messages() = runTest {
        // Scenario 07-IC: provider's message arrives in
        // real-time while the consumer is viewing the chat.
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()
        val ready = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(0, ready.detail.messages.size)

        // Provider pushes a message.
        webSocketEvents.tryEmit(providerEvent(messageId = "100", content = "¡Hola!"))
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(1, state.detail.messages.size)
        assertEquals("100", state.detail.messages[0].id)
        assertEquals(ConversationSender.Provider, state.detail.messages[0].sender)
        assertEquals("¡Hola!", state.detail.messages[0].content)
    }

    @Test
    fun ws_event_for_other_conversation_is_ignored() = runTest {
        // Scenario 08-IC: messages from another conversation must
        // NOT leak into the current chat.
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()

        // Event for a DIFFERENT conversation id.
        webSocketEvents.tryEmit(providerEvent(conversationId = 99, messageId = "999"))
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(
            "messages from other conversations must not leak",
            0,
            state.detail.messages.size,
        )
    }

    @Test
    fun ws_event_with_consumer_sender_is_ignored_to_avoid_echo_duplication() = runTest {
        // The consumer's own sendMessage Success already appends
        // the message to `detail.messages`. The WS echo of the same
        // message would be a duplicate (the server returns the
        // same id both via the POST response and the WS push).
        // The filter on `sender == Provider` prevents that.
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        coEvery { sendMessage("1", "yo") } returns
            SendMessageOutcome.Success(
                ConversationMessage(
                    id = "10",
                    sender = ConversationSender.Consumer,
                    content = "yo",
                    createdOnEpochMillis = 1_700_000_000_000L,
                ),
            )

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()

        viewModel.onPromptChange("yo")
        viewModel.onSendClick()
        advanceUntilIdle()
        val readyAfterSend = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(1, readyAfterSend.detail.messages.size)

        // The WS echoes the same message with sender=consumer.
        // Must NOT be appended (already present).
        webSocketEvents.tryEmit(
            WsEvent(
                type = WsEvent.CONVERSATION_MESSAGE_CREATED,
                conversationId = 1,
                message = ConversationMessage(
                    id = "10",
                    sender = ConversationSender.Consumer,
                    content = "yo",
                    createdOnEpochMillis = 1_700_000_000_000L,
                ),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(
            "WS echo of own send must not duplicate the message",
            1,
            state.detail.messages.size,
        )
    }

    @Test
    fun duplicate_ws_event_with_same_id_is_de_duped() = runTest {
        // Defensive: if the server double-pushes the same id
        // (unlikely but possible across reconnects), the thread
        // must not show two bubbles.
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()

        webSocketEvents.tryEmit(providerEvent(messageId = "100"))
        webSocketEvents.tryEmit(providerEvent(messageId = "100"))
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(
            "duplicate WS event with same id must be de-duped",
            1,
            state.detail.messages.size,
        )
    }

    @Test
    fun ws_event_before_loaded_state_is_silently_dropped() = runTest {
        // The VM may receive a WS event before the initial
        // `load()` finishes (race). The append must no-op because
        // the state isn't `Ready` yet.
        coEvery { getConversationById("1") } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        // No load() call yet → state is Loading.
        webSocketEvents.tryEmit(providerEvent(messageId = "100"))

        assertEquals(ConversationUiState.Loading, viewModel.uiState.value)
    }

    // ---- Scroll-position tracking (09-IC + 10-IC) ------------------

    @Test
    fun ws_event_while_at_bottom_sets_hasUnreadIncoming_to_false() = runTest {
        // Scenario 09-IC: when the user is at the bottom and a
        // new message arrives, no "↓ nuevo mensaje" banner shows
        // (the new bubble auto-scrolls into view).
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()
        // Default state: isAtBottom = true.
        val initial = viewModel.uiState.value as ConversationUiState.Ready
        assertTrue(initial.isAtBottom)
        assertFalse(initial.hasUnreadIncoming)

        webSocketEvents.tryEmit(providerEvent(messageId = "100"))
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(1, state.detail.messages.size)
        assertFalse(
            "at bottom + incoming → no unread banner",
            state.hasUnreadIncoming,
        )
    }

    @Test
    fun ws_event_while_scrolled_up_sets_hasUnreadIncoming_to_true() = runTest {
        // Scenario 10-IC: when the user is scrolled up reading
        // older messages and a new one arrives, surface the
        // "↓ nuevo mensaje" banner.
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()
        viewModel.onScrollPositionChanged(atBottom = false)
        assertFalse(
            (viewModel.uiState.value as ConversationUiState.Ready).isAtBottom,
        )

        webSocketEvents.tryEmit(providerEvent(messageId = "100"))
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(1, state.detail.messages.size)
        assertTrue(
            "scrolled up + incoming → unread banner",
            state.hasUnreadIncoming,
        )
    }

    @Test
    fun scrolling_back_to_bottom_clears_unread_incoming() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()
        viewModel.onScrollPositionChanged(atBottom = false)
        webSocketEvents.tryEmit(providerEvent(messageId = "100"))
        advanceUntilIdle()
        assertTrue(
            (viewModel.uiState.value as ConversationUiState.Ready).hasUnreadIncoming,
        )

        // User scrolls back to the bottom (auto-scroll fires,
        // which the screen reports via onScrollPositionChanged).
        viewModel.onScrollPositionChanged(atBottom = true)

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertFalse(
            "scroll-to-bottom clears the unread flag",
            state.hasUnreadIncoming,
        )
    }

    @Test
    fun unread_banner_tap_clears_unread_incoming() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()
        viewModel.onScrollPositionChanged(atBottom = false)
        webSocketEvents.tryEmit(providerEvent(messageId = "100"))
        advanceUntilIdle()
        assertTrue(
            (viewModel.uiState.value as ConversationUiState.Ready).hasUnreadIncoming,
        )

        viewModel.onUnreadBannerTapped()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertFalse(
            "banner tap clears the unread flag",
            state.hasUnreadIncoming,
        )
        // Scrolled-up state is preserved — the banner tap is
        // orthogonal to scroll position.
        assertFalse(state.isAtBottom)
    }

    @Test
    fun onScrollPositionChanged_with_same_value_is_a_no_op() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())

        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        viewModel.load("1")
        advanceUntilIdle()
        val before = viewModel.uiState.value as ConversationUiState.Ready

        viewModel.onScrollPositionChanged(atBottom = true) // already true

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun onScrollPositionChanged_outside_Ready_is_a_no_op() = runTest {
        coEvery { getConversationById("1") } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }
        viewModel = ConversationViewModel(getConversationById, sendMessage, sendMediaMessage, mediaReader, webSocketClient)
        // State is Loading.
        viewModel.onScrollPositionChanged(atBottom = false)
        assertEquals(ConversationUiState.Loading, viewModel.uiState.value)
    }
}