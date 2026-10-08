package com.loresuelvo.consumer.bdd.notifications

import android.content.Context
import android.content.SharedPreferences
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.notifications.ServiceNotificationDecoder
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.LogoutOutcome
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.MessageNotificationPublisher
import com.loresuelvo.consumer.domain.notifications.NotificationAvailability
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.NotificationDismissal
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationAvailability
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationPublisher
import com.loresuelvo.consumer.domain.usecase.auth.RestoreAuthenticatedSessionUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveServiceNotificationUseCase
import com.loresuelvo.consumer.platform.auth.AuthProvider
import com.loresuelvo.consumer.platform.notifications.ConsumerMessageReceiver
import com.loresuelvo.consumer.platform.notifications.ConsumerServiceNotificationReceiver
import com.loresuelvo.consumer.platform.notifications.VisibleConversationStore
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.navigation.SessionRouteMapper
import com.loresuelvo.consumer.ui.notifications.PushRegistrationRequests
import com.loresuelvo.consumer.ui.session.SessionViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.io.IOException

class ConsumerSessionLogoutWorld {
    private val sessionPreferences = fakePreferences()
    private val installationPreferences = fakePreferences()

    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var binding: InstallationBinding
    private lateinit var sessionViewModel: SessionViewModel
    private val authProvider = mockk<AuthProvider>()
    private val userRepository = mockk<UserRepository>()
    private val activityContext = mockk<Context>(relaxed = true)

    private var visibleNoticesCount = 0
    private var dismissalCalled = false

    private lateinit var messageReceiver: ConsumerMessageReceiver
    private lateinit var serviceReceiver: ConsumerServiceNotificationReceiver
    private var messageSequence = 100
    private var serviceSequence = 200

    fun startWithVisibleNotices() {
        visibleNoticesCount = 0
        dismissalCalled = false
        messageSequence = 100
        serviceSequence = 200

        sessions = EncryptedAuthSessionStore(sessionPreferences)
        val user = User(
            displayName = "Verified Consumer",
            firstName = "Verified",
            lastName = "Consumer",
            email = "consumer@example.test",
            address = RegisterConsumerAddress("Test street", "1"),
            backendUserId = 17,
        )
        sessions.saveSession(AuthSession(user, "valid-token"))

        installations = EncryptedInstallationStateStore(installationPreferences)
        binding = installations.prepare(17, "logout-binding")
        installations.confirm(binding)

        val notificationDismissal = NotificationDismissal {
            dismissalCalled = true
            visibleNoticesCount = 0
        }

        val clock = NotificationClock { System.currentTimeMillis() }
        val messageEvents = StoredNotificationEvents(installationPreferences)
        val serviceEvents = StoredNotificationEvents(installationPreferences)

        val messagePublisher = MessageNotificationPublisher {
            visibleNoticesCount++
            true
        }
        val authorizeMessage = AuthorizeMessageNotificationUseCase(sessions, installations, clock)
        val messageUseCase = ReceiveMessageNotificationUseCase(
            authorize = authorizeMessage,
            visibility = VisibleConversationStore(),
            availability = NotificationAvailability { true },
            events = messageEvents,
            publisher = messagePublisher,
        )
        messageReceiver = ConsumerMessageReceiver(MessageNotificationDecoder(), messageUseCase)

        val servicePublisher = ServiceNotificationPublisher {
            visibleNoticesCount++
            true
        }
        val authorizeService = AuthorizeServiceNotificationUseCase(sessions, installations, clock)
        val serviceUseCase = ReceiveServiceNotificationUseCase(
            authorize = authorizeService,
            availability = ServiceNotificationAvailability { true },
            events = serviceEvents,
            publisher = servicePublisher,
        )
        serviceReceiver = ConsumerServiceNotificationReceiver(ServiceNotificationDecoder(), serviceUseCase)

        val firstMessageOutcome = messageReceiver.receive(validMessagePayload(++messageSequence))
        assertEquals("Setup message notice should be published", MessageNotificationOutcome.Published, firstMessageOutcome)
        val firstServiceOutcome = serviceReceiver.receive(validServicePayload(++serviceSequence))
        assertEquals("Setup service notice should be published", ServiceNotificationOutcome.Published, firstServiceOutcome)
        assertEquals(2, visibleNoticesCount)

        coEvery { userRepository.getCurrentUser() } returns CurrentUserOutcome.Success(user)

        sessionViewModel = SessionViewModel(
            sessionStore = sessions,
            authProvider = authProvider,
            restoreSession = RestoreAuthenticatedSessionUseCase(userRepository, sessions),
            pushRegistrationRequests = PushRegistrationRequests(),
            notificationDismissal = notificationDismissal,
        )
    }

    fun setConnection(connectionState: String) {
        when (connectionState) {
            "con conexión" -> {
                coEvery { authProvider.logout(any()) } returns LogoutOutcome.Success
            }
            "sin conexión" -> {
                coEvery { authProvider.logout(any()) } returns LogoutOutcome.Failure.Provider(IOException("Sin conexión"))
            }
            else -> error("Unknown connection state: $connectionState")
        }
    }

    fun confirmLogout() {
        sessionViewModel.signOut(activityContext)
    }

    fun assertLocalSessionClosedImmediately() {
        assertNull("Local session store must be cleared immediately", sessions.getSession())
        assertFalse("ViewModel state must report unauthenticated immediately", sessionViewModel.uiState.value.authenticated)
        assertNull("ViewModel state session must be null", sessionViewModel.uiState.value.session)
    }

    fun assertVisibleNoticesDisappeared() {
        assertTrue("Notification dismissal must be called", dismissalCalled)
        assertEquals("Visible notifications must be cleared", 0, visibleNoticesCount)
    }

    fun assertSubsequentNoticesNotShownAndNoPrivateData() {
        val subsequentMessage = messageReceiver.receive(validMessagePayload(++messageSequence))
        assertEquals("Subsequent message notification for old session must be rejected as Invalid",
            MessageNotificationOutcome.Invalid, subsequentMessage)

        val subsequentService = serviceReceiver.receive(validServicePayload(++serviceSequence))
        assertEquals("Subsequent service notification for old session must be rejected as Invalid",
            ServiceNotificationOutcome.Invalid, subsequentService)

        assertEquals("No notifications should be visible", 0, visibleNoticesCount)

        val targetRoute = SessionRouteMapper.routeFor(sessionViewModel.uiState.value)
        assertEquals("Old notification tap or navigation must redirect to Welcome without exposing private screens",
            Route.Welcome.path, targetRoute)
    }

    fun close() {
    }

    private fun validMessagePayload(seq: Int): Map<String, String> = mapOf(
        "version" to "1",
        "event_id" to "message:$seq:17",
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

    private fun validServicePayload(seq: Int): Map<String, String> = mapOf(
        "version" to "1",
        "event_id" to "notification:service_proposal_received:$seq",
        "type" to "service_proposal_received",
        "resource_type" to "service_proposal",
        "destination" to "service_proposal",
        "resource_id" to "99",
        "recipient_user_id" to "17",
        "recipient_app" to "consumer",
        "installation_id" to binding.identity.id,
        "binding_id" to binding.id,
        "title" to "Nueva propuesta",
        "body" to "Recibiste una propuesta de servicio.",
        "expires_at" to "2099-01-01T00:00:00Z",
    )

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
