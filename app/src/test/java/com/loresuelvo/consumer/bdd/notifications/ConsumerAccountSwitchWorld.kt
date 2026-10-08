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
import com.loresuelvo.consumer.domain.notifications.MessageNotification
import com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.MessageNotificationPublisher
import com.loresuelvo.consumer.domain.notifications.NotificationAvailability
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.NotificationDismissal
import com.loresuelvo.consumer.domain.notifications.ServiceNotification
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
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class ConsumerAccountSwitchWorld {
    private val sessionPreferences = fakePreferences()
    private val installationPreferences = fakePreferences()

    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var user1Binding: InstallationBinding
    private lateinit var user2Binding: InstallationBinding
    private lateinit var sessionViewModel: SessionViewModel

    private val authProvider = mockk<AuthProvider>()
    private val userRepository = mockk<UserRepository>()
    private val activityContext = mockk<Context>(relaxed = true)

    private var visibleNoticesCount = 0
    private var dismissalCount = 0

    private lateinit var messageReceiver: ConsumerMessageReceiver
    private lateinit var serviceReceiver: ConsumerServiceNotificationReceiver
    private var messageSeq = 500
    private var serviceSeq = 600

    private var previousAccountMessageOutcome: MessageNotificationOutcome? = null
    private var currentAccountMessageOutcome: MessageNotificationOutcome? = null
    private var currentAccountServiceOutcome: ServiceNotificationOutcome? = null

    val user1 = User(
        displayName = "Usuario Anterior",
        firstName = "Usuario",
        lastName = "Anterior",
        email = "user1@example.test",
        address = RegisterConsumerAddress("Calle Vieja", "111"),
        backendUserId = 11,
    )

    val user2 = User(
        displayName = "Usuario Actual",
        firstName = "Usuario",
        lastName = "Actual",
        email = "user2@example.test",
        address = RegisterConsumerAddress("Calle Nueva", "222"),
        backendUserId = 22,
    )

    fun switchAccountOffline() {
        sessions = EncryptedAuthSessionStore(sessionPreferences)
        sessions.saveSession(AuthSession(user1, "jwt-user-1"))

        installations = EncryptedInstallationStateStore(installationPreferences)
        user1Binding = installations.prepare(11, "login-user-1")
        installations.confirm(user1Binding)

        val dismissal = NotificationDismissal {
            dismissalCount++
            visibleNoticesCount = 0
        }

        coEvery { authProvider.logout(any()) } returns LogoutOutcome.Failure.Provider(IOException("Offline"))
        coEvery { userRepository.getCurrentUser() } returns CurrentUserOutcome.Success(user1)

        sessionViewModel = SessionViewModel(
            sessionStore = sessions,
            authProvider = authProvider,
            restoreSession = RestoreAuthenticatedSessionUseCase(userRepository, sessions),
            pushRegistrationRequests = PushRegistrationRequests(),
            notificationDismissal = dismissal,
        )

        sessionViewModel.signOut(activityContext)
        assertEquals(0, visibleNoticesCount)
        assertTrue("Dismissal must have been invoked", dismissalCount > 0)
        assertFalse("Session must be closed", sessionViewModel.uiState.value.authenticated)

        sessions.saveSession(AuthSession(user2, "jwt-user-2"))
        coEvery { userRepository.getCurrentUser() } returns CurrentUserOutcome.Success(user2)
        sessionViewModel = SessionViewModel(
            sessionStore = sessions,
            authProvider = authProvider,
            restoreSession = RestoreAuthenticatedSessionUseCase(userRepository, sessions),
            pushRegistrationRequests = PushRegistrationRequests(),
            notificationDismissal = dismissal,
        )
        assertTrue("User 2 must be authenticated", sessionViewModel.uiState.value.authenticated)
        assertEquals(22, sessions.getSession()?.user?.backendUserId)
    }

    fun enableNotificationsForNewAccount() {
        user2Binding = installations.prepare(22, "login-user-2")
        installations.confirm(user2Binding)

        val confirmed = installations.confirmedInstallation()
        assertNotNull(confirmed)
        assertEquals(22, confirmed!!.userId)
        assertEquals(user2Binding.id, confirmed.bindingId)
        assertNotEquals(user1Binding.id, confirmed.bindingId)
    }

    fun restartLoResuelvo() {
        sessions = EncryptedAuthSessionStore(sessionPreferences)
        installations = EncryptedInstallationStateStore(installationPreferences)

        val session = sessions.getSession()
        assertNotNull("Session must be restored after restart", session)
        assertEquals(22, session!!.user.backendUserId)

        val confirmed = installations.confirmedInstallation()
        assertNotNull("Confirmed installation must persist after restart", confirmed)
        assertEquals(22, confirmed!!.userId)
        assertEquals(user2Binding.id, confirmed.bindingId)

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

        sessionViewModel = SessionViewModel(
            sessionStore = sessions,
            authProvider = authProvider,
            restoreSession = RestoreAuthenticatedSessionUseCase(userRepository, sessions),
            pushRegistrationRequests = PushRegistrationRequests(),
            notificationDismissal = NotificationDismissal { visibleNoticesCount = 0 },
        )
    }

    fun receivePendingNoticeFromPreviousAccount() {
        val oldPayload = mapOf(
            "version" to "1",
            "event_id" to "message:${++messageSeq}:11",
            "type" to "conversation.message.created",
            "resource_type" to "conversation",
            "destination" to "conversation",
            "resource_id" to "42",
            "recipient_user_id" to "11",
            "recipient_app" to "consumer",
            "installation_id" to user1Binding.identity.id,
            "binding_id" to user1Binding.id,
            "title" to "Nuevo mensaje",
            "body" to "Tenés un nuevo mensaje en LoResuelvo.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )
        previousAccountMessageOutcome = messageReceiver.receive(oldPayload)
    }

    fun assertNoticeNotShownAndCannotEnterPreviousAccount() {
        assertEquals("Pending notice from previous account must be rejected as Invalid",
            MessageNotificationOutcome.Invalid, previousAccountMessageOutcome)
        assertEquals("No notifications must be shown for previous account notice",
            0, visibleNoticesCount)
        assertEquals("Active session must belong strictly to current account (user 2)",
            22, sessions.getSession()?.user?.backendUserId)
    }

    fun assertCanReceiveNoticesForCurrentAccount() {
        val currentMessagePayload = mapOf(
            "version" to "1",
            "event_id" to "message:${++messageSeq}:22",
            "type" to "conversation.message.created",
            "resource_type" to "conversation",
            "destination" to "conversation",
            "resource_id" to "77",
            "recipient_user_id" to "22",
            "recipient_app" to "consumer",
            "installation_id" to user2Binding.identity.id,
            "binding_id" to user2Binding.id,
            "title" to "Nuevo mensaje",
            "body" to "Tenés un nuevo mensaje en LoResuelvo.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )
        currentAccountMessageOutcome = messageReceiver.receive(currentMessagePayload)
        assertEquals("Current account message notice must be published",
            MessageNotificationOutcome.Published, currentAccountMessageOutcome)
        assertEquals(1, visibleNoticesCount)

        val currentServicePayload = mapOf(
            "version" to "1",
            "event_id" to "notification:service_proposal_received:${++serviceSeq}",
            "type" to "service_proposal_received",
            "resource_type" to "service_proposal",
            "destination" to "service_proposal",
            "resource_id" to "88",
            "recipient_user_id" to "22",
            "recipient_app" to "consumer",
            "installation_id" to user2Binding.identity.id,
            "binding_id" to user2Binding.id,
            "title" to "Nueva propuesta",
            "body" to "Recibiste una propuesta de servicio.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )
        currentAccountServiceOutcome = serviceReceiver.receive(currentServicePayload)
        assertEquals("Current account service notice must be published",
            ServiceNotificationOutcome.Published, currentAccountServiceOutcome)
        assertEquals(2, visibleNoticesCount)
    }

    fun close() {
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
