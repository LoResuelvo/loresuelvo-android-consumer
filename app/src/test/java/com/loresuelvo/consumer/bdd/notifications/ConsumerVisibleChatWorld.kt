package com.loresuelvo.consumer.bdd.notifications

import android.content.SharedPreferences
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.notifications.MessageNotification
import com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.MessageNotificationPublisher
import com.loresuelvo.consumer.domain.notifications.NotificationAvailability
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.realtime.RealtimeClient
import com.loresuelvo.consumer.domain.realtime.WsEvent
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMediaMessageUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveMessageNotificationUseCase
import com.loresuelvo.consumer.platform.media.AudioRecorder
import com.loresuelvo.consumer.platform.media.MediaMetadataRetrieverReader
import com.loresuelvo.consumer.platform.media.MediaReader
import com.loresuelvo.consumer.platform.notifications.ConsumerMessageReceiver
import com.loresuelvo.consumer.platform.notifications.VisibleConversationStore
import com.loresuelvo.consumer.testdi.FakeAudioPlayer
import com.loresuelvo.consumer.ui.screens.chat.ConversationUiState
import com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConsumerVisibleChatWorld {
    private val dispatcher = StandardTestDispatcher()
    private val sessionPreferences = fakePreferences()
    private val installationPreferences = fakePreferences()
    private val visibility = VisibleConversationStore()
    private val publishedNotifications = mutableListOf<MessageNotification>()
    private val webSocketEvents = MutableSharedFlow<WsEvent>(extraBufferCapacity = 10)

    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var binding: InstallationBinding
    private lateinit var viewModel: ConversationViewModel
    private var lastReceiveOutcome: MessageNotificationOutcome? = null

    fun startReadingConversation() {
        Dispatchers.setMain(dispatcher)
        sessions = EncryptedAuthSessionStore(sessionPreferences)
        sessions.saveSession(
            AuthSession(
                User("Verified Consumer", backendUserId = 17),
                "verified-jwt",
            )
        )
        installations = EncryptedInstallationStateStore(installationPreferences)
        binding = installations.prepare(17, "visible-chat-login")
        installations.confirm(binding)

        visibility.show(42)

        val detail = ConversationDetail(
            id = "42",
            status = ConversationStatus.Pending,
            counterpart = ConversationCounterpart(
                id = 20L,
                name = "Juan",
                surname = "Prestador",
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
            messages = listOf(
                ConversationMessage("1", ConversationSender.Provider, "Hola, ¿en qué te ayudo?", 1000L),
            ),
            updatedOnEpochMillis = 1000L,
        )

        val getConversationById = mockk<GetConversationByIdUseCase>()
        coEvery { getConversationById("42") } returns ConversationDetailOutcome.Success(detail)
        val sendMessage = mockk<SendMessageUseCase>(relaxed = true)
        val sendMediaMessage = mockk<SendMediaMessageUseCase>(relaxed = true)
        val mediaReader = mockk<MediaReader>(relaxed = true)
        val mediaMetadataRetriever = mockk<MediaMetadataRetrieverReader>(relaxed = true)
        val audioRecorder = mockk<AudioRecorder>(relaxed = true)
        val audioPlayer = FakeAudioPlayer()
        val realtimeClient = object : RealtimeClient {
            override val events: SharedFlow<WsEvent> = webSocketEvents.asSharedFlow()
            override fun start() {}
            override fun stop() {}
        }

        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            audioPlayer,
            realtimeClient,
        )
        viewModel.load("42")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Conversation must be loaded and Ready", state is ConversationUiState.Ready)
    }

    fun setDraftAndReadingPosition() {
        viewModel.onPromptChange("Borrador sin enviar")
        viewModel.onScrollPositionChanged(atBottom = false)

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals("Borrador sin enviar", state.promptInput)
        assertFalse("Reading position must not be at bottom", state.isAtBottom)
    }

    fun incomingProviderMessageNotice() {
        val payload = mapOf(
            "version" to "1",
            "event_id" to "message:51:17",
            "type" to "conversation.message.created",
            "resource_type" to "conversation",
            "destination" to "conversation",
            "resource_id" to "42",
            "recipient_user_id" to "17",
            "recipient_app" to "consumer",
            "installation_id" to binding.identity.id,
            "binding_id" to binding.id,
            "title" to "Nuevo mensaje",
            "body" to "Tenés un nuevo mensaje en LoResuelvo.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )
        val authorize = AuthorizeMessageNotificationUseCase(sessions, installations, NotificationClock { 0 })
        val receive = ReceiveMessageNotificationUseCase(
            authorize = authorize,
            visibility = visibility,
            availability = NotificationAvailability { true },
            events = StoredNotificationEvents(installationPreferences),
            publisher = MessageNotificationPublisher {
                publishedNotifications += it
                true
            },
        )
        val receiver = ConsumerMessageReceiver(MessageNotificationDecoder(), receive)
        lastReceiveOutcome = receiver.receive(payload)

        val incomingMessage = ConversationMessage(
            id = "51",
            sender = ConversationSender.Provider,
            content = "Nuevo mensaje del prestador",
            createdOnEpochMillis = 2000L,
        )
        webSocketEvents.tryEmit(
            WsEvent(
                type = WsEvent.CONVERSATION_MESSAGE_CREATED,
                conversationId = 42L,
                message = incomingMessage,
            )
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    fun assertUpdatedWithoutNotificationOrPopup() {
        assertEquals(
            "Push notice for open conversation must be suppressed",
            MessageNotificationOutcome.VisibleConversation,
            lastReceiveOutcome,
        )
        assertTrue(
            "No phone notification must be published",
            publishedNotifications.isEmpty(),
        )

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertTrue(
            "Conversation must contain the incoming message",
            state.detail.messages.any { it.id == "51" },
        )
    }

    fun assertDraftAndReadingPositionPreserved() {
        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertEquals(
            "Draft message must be preserved",
            "Borrador sin enviar",
            state.promptInput,
        )
        assertFalse(
            "Reading position must remain preserved (not at bottom)",
            state.isAtBottom,
        )
    }

    fun close() {
        visibility.show(null)
        Dispatchers.resetMain()
    }

    private fun fakePreferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { prefs.getString(any(), any()) } answers { values[firstArg<String>()] as? String ?: secondArg() }
        every { prefs.getInt(any(), any()) } answers { values[firstArg<String>()] as? Int ?: secondArg() }
        every { prefs.getLong(any(), any()) } answers { values[firstArg<String>()] as? Long ?: secondArg() }
        every { prefs.getBoolean(any(), any()) } answers { values[firstArg<String>()] as? Boolean ?: secondArg() }
        every { prefs.contains(any()) } answers { values.containsKey(firstArg<String>()) }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers { values[firstArg<String>()] = secondArg<String?>(); editor }
        every { editor.putInt(any(), any()) } answers { values[firstArg<String>()] = secondArg<Int>(); editor }
        every { editor.putLong(any(), any()) } answers { values[firstArg<String>()] = secondArg<Long>(); editor }
        every { editor.putBoolean(any(), any()) } answers { values[firstArg<String>()] = secondArg<Boolean>(); editor }
        every { editor.remove(any()) } answers { values.remove(firstArg<String>()); editor }
        every { editor.clear() } answers { values.clear(); editor }
        every { editor.apply() } returns Unit
        every { editor.commit() } returns true
        return prefs
    }
}
