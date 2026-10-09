package com.loresuelvo.consumer.instrumented.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.realtime.RealtimeClient
import com.loresuelvo.consumer.domain.realtime.WsEvent
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMediaMessageUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveMessageNotificationUseCase
import com.loresuelvo.consumer.platform.media.AudioPlayer
import com.loresuelvo.consumer.platform.media.AudioRecorder
import com.loresuelvo.consumer.platform.media.MediaMetadataRetrieverReader
import com.loresuelvo.consumer.platform.media.MediaReader
import com.loresuelvo.consumer.platform.notifications.AndroidMessageNotificationPublisher
import com.loresuelvo.consumer.platform.notifications.ConsumerMessageReceiver
import com.loresuelvo.consumer.platform.notifications.VisibleConversationStore
import com.loresuelvo.consumer.testdi.FakeConversationRepository
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.notifications.ConversationNotificationVisibility
import com.loresuelvo.consumer.ui.screens.chat.ConversationUiState
import com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@SdkSuppress(minSdkVersion = 33)
@RunWith(AndroidJUnit4::class)
class VisibleChatNotificationInstrumentedTest {

    @get:Rule(order = 0)
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var context: Context
    private lateinit var manager: NotificationManager
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var binding: InstallationBinding
    private lateinit var visibility: VisibleConversationStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()

        val sessionPrefs = context.getSharedPreferences("test_visible_chat_sessions", Context.MODE_PRIVATE)
        val installPrefs = context.getSharedPreferences("test_visible_chat_installations", Context.MODE_PRIVATE)
        sessionPrefs.edit().clear().commit()
        installPrefs.edit().clear().commit()

        sessions = EncryptedAuthSessionStore(sessionPrefs)
        visibility = VisibleConversationStore(sessions)
        sessions.saveSession(
            AuthSession(
                User(
                    displayName = "Test Consumer",
                    firstName = "Test",
                    lastName = "Consumer",
                    email = "consumer@example.test",
                    address = RegisterConsumerAddress("Street", "1"),
                    backendUserId = 17,
                ),
                "test-jwt",
            )
        )
        installations = EncryptedInstallationStateStore(installPrefs)
        binding = installations.prepare(17, "visible-chat-login")
        installations.confirm(binding)
    }

    @After
    fun tearDown() {
        manager.cancelAll()
        visibility.show(null)
    }

    @Test
    fun visible_conversation_suppresses_push_notification_and_does_not_post_to_notification_manager() {
        visibility.show(42)

        val authorize = AuthorizeMessageNotificationUseCase(sessions, installations, NotificationClock { 0 })
        val publisher = AndroidMessageNotificationPublisher(context, authorize)
        val eventsPrefs = context.getSharedPreferences("test_visible_chat_events", Context.MODE_PRIVATE)
        eventsPrefs.edit().clear().commit()
        val events = StoredNotificationEvents(eventsPrefs)

        val receiveUseCase = ReceiveMessageNotificationUseCase(
            authorize = authorize,
            visibility = visibility,
            availability = publisher,
            events = events,
            publisher = publisher,
        )
        val receiver = ConsumerMessageReceiver(MessageNotificationDecoder(), receiveUseCase)

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

        val outcome = receiver.receive(payload)
        assertEquals(MessageNotificationOutcome.VisibleConversation, outcome)

        val active = manager.activeNotifications.filter { it.tag?.contains("message:51:17") == true }
        assertTrue("No notification should be published for visible conversation", active.isEmpty())
    }

    @Test
    fun different_conversation_notification_publishes_to_notification_manager_even_if_another_is_visible() {
        visibility.show(42)

        val authorize = AuthorizeMessageNotificationUseCase(sessions, installations, NotificationClock { 0 })
        val publisher = AndroidMessageNotificationPublisher(context, authorize)
        val eventsPrefs = context.getSharedPreferences("test_visible_chat_diff_events", Context.MODE_PRIVATE)
        eventsPrefs.edit().clear().commit()
        val events = StoredNotificationEvents(eventsPrefs)

        val receiveUseCase = ReceiveMessageNotificationUseCase(
            authorize = authorize,
            visibility = visibility,
            availability = publisher,
            events = events,
            publisher = publisher,
        )
        val receiver = ConsumerMessageReceiver(MessageNotificationDecoder(), receiveUseCase)

        val payload = mapOf(
            "version" to "1",
            "event_id" to "message:99:17",
            "type" to "conversation.message.created",
            "resource_type" to "conversation",
            "destination" to "conversation",
            "resource_id" to "99",
            "recipient_user_id" to "17",
            "recipient_app" to "consumer",
            "installation_id" to binding.identity.id,
            "binding_id" to binding.id,
            "title" to "Nuevo mensaje",
            "body" to "Tenés un nuevo mensaje en LoResuelvo.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )

        val outcome = receiver.receive(payload)
        assertEquals(MessageNotificationOutcome.Published, outcome)

        val active = manager.activeNotifications.filter { it.tag?.contains("message:99:17") == true }
        assertEquals("Notification must be published for different conversation", 1, active.size)
    }

    @Test
    fun incoming_message_while_reading_open_conversation_preserves_draft_and_reading_position() {
        val repo = FakeConversationRepository()
        repo.setDetailSeed(
            ConversationDetail(
                id = "42",
                status = ConversationStatus.Pending,
                counterpart = ConversationCounterpart(20L, "Juan", "Prestador", "Plomería", null),
                messages = listOf(
                    ConversationMessage("1", ConversationSender.Provider, "Hola!", 1000L),
                ),
                updatedOnEpochMillis = 1000L,
            )
        )
        val getConversationById = GetConversationByIdUseCase(repo)
        val sendMessage = SendMessageUseCase(repo)
        val sendMediaMessage = SendMediaMessageUseCase(repo)

        val dummyMediaReader = object : MediaReader {
            override suspend fun read(uri: Uri): MediaUpload =
                MediaUpload.Image(byteArrayOf(), "image/jpeg", "test.jpg")
        }
        val dummyMetadataReader = object : MediaMetadataRetrieverReader {
            override suspend fun extractDurationMillis(uri: Uri): Long? = null
        }
        val dummyAudioRecorder = object : AudioRecorder {
            override fun start(): Result<Unit> = Result.success(Unit)
            override fun stop(): Result<Uri> = Result.success(Uri.EMPTY)
            override fun cancel() {}
        }
        val dummyAudioPlayer = object : AudioPlayer {
            override val isPlaying: StateFlow<Boolean> = MutableStateFlow(false)
            override val currentPositionMillis: StateFlow<Long> = MutableStateFlow(0L)
            override fun play(url: String, startPositionMillis: Long) {}
            override fun pause() {}
            override fun stop() {}
        }
        val realtimeEvents = MutableSharedFlow<WsEvent>(extraBufferCapacity = 10)
        val realtimeClient = object : RealtimeClient {
            override val events: SharedFlow<WsEvent> = realtimeEvents.asSharedFlow()
            override fun start() {}
            override fun stop() {}
        }

        lateinit var viewModel: ConversationViewModel
        compose.runOnUiThread {
            viewModel = ConversationViewModel(
                getConversationById,
                sendMessage,
                sendMediaMessage,
                dummyMediaReader,
                dummyMetadataReader,
                dummyAudioRecorder,
                dummyAudioPlayer,
                realtimeClient,
            )
            viewModel.load("42")
        }
        compose.waitForIdle()

        compose.runOnUiThread {
            viewModel.onPromptChange("Borrador sin enviar")
            viewModel.onScrollPositionChanged(atBottom = false)
        }
        compose.waitForIdle()

        realtimeEvents.tryEmit(
            WsEvent.ConversationMessageCreated(
                conversationId = 42L,
                message = ConversationMessage(
                    id = "51",
                    sender = ConversationSender.Provider,
                    content = "Nuevo mensaje del prestador",
                    createdOnEpochMillis = 2000L,
                ),
            )
        )
        compose.waitForIdle()

        compose.runOnUiThread {
            val state = viewModel.uiState.value
            assertTrue("ViewModel state must be Ready", state is ConversationUiState.Ready)
            val ready = state as ConversationUiState.Ready
            assertEquals("Draft prompt must be preserved", "Borrador sin enviar", ready.promptInput)
            assertFalse("Reading position must not jump to bottom", ready.isAtBottom)
            assertTrue("hasUnreadIncoming must be true when message arrives while reading", ready.hasUnreadIncoming)
            assertTrue("Incoming message must be appended", ready.detail.messages.any { it.id == "51" })
        }
    }

    @Test
    fun conversation_notification_visibility_composable_binds_and_unbinds_visible_chat() {
        var currentRoute by mutableStateOf<String?>(Route.Conversation("").path)
        var currentConvId by mutableStateOf<String?>("42")
        var isAuthenticated by mutableStateOf(true)

        compose.setContent {
            ConversationNotificationVisibility(
                visibility = visibility,
                route = currentRoute,
                conversationId = currentConvId,
                authenticated = isAuthenticated,
            )
        }
        compose.waitForIdle()
        assertEquals(42, visibility.visibleConversation())

        currentRoute = Route.Messages.path
        currentConvId = null
        compose.waitForIdle()
        assertNull(visibility.visibleConversation())

        currentRoute = Route.Conversation("").path
        currentConvId = "42"
        isAuthenticated = false
        compose.waitForIdle()
        assertNull(visibility.visibleConversation())
    }
}
