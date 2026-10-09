package com.loresuelvo.consumer.instrumented.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
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
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.NotificationDismissal
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationOutcome
import com.loresuelvo.consumer.domain.usecase.auth.RestoreAuthenticatedSessionUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveServiceNotificationUseCase
import com.loresuelvo.consumer.platform.auth.AuthProvider
import com.loresuelvo.consumer.platform.notifications.AndroidMessageNotificationPublisher
import com.loresuelvo.consumer.platform.notifications.AndroidNotificationDismissal
import com.loresuelvo.consumer.platform.notifications.AndroidServiceNotificationPublisher
import com.loresuelvo.consumer.platform.notifications.ConsumerMessageReceiver
import com.loresuelvo.consumer.platform.notifications.ConsumerServiceNotificationReceiver
import com.loresuelvo.consumer.platform.notifications.VisibleConversationStore
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.navigation.SessionRouteMapper
import com.loresuelvo.consumer.ui.notifications.PushRegistrationRequests
import com.loresuelvo.consumer.ui.session.SessionViewModel
import java.io.IOException
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 33)
@RunWith(AndroidJUnit4::class)
class SessionLogoutNotificationsInstrumentedTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var binding: InstallationBinding
    private lateinit var dismissal: NotificationDismissal
    private lateinit var fakeAuthProvider: FakeLogoutAuthProvider
    private lateinit var sessionViewModel: SessionViewModel

    private lateinit var messageReceiver: ConsumerMessageReceiver
    private lateinit var serviceReceiver: ConsumerServiceNotificationReceiver
    private var messageSeq = 100
    private var serviceSeq = 200

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()

        val suffix = UUID.randomUUID().toString().take(8)
        val sessionPrefs = context.getSharedPreferences("test_logout_sessions_$suffix", Context.MODE_PRIVATE)
        val installPrefs = context.getSharedPreferences("test_logout_installations_$suffix", Context.MODE_PRIVATE)
        sessionPrefs.edit().clear().commit()
        installPrefs.edit().clear().commit()

        sessions = EncryptedAuthSessionStore(sessionPrefs)
        val user = User(
            displayName = "Logout Consumer",
            firstName = "Logout",
            lastName = "Consumer",
            email = "logout@example.test",
            address = RegisterConsumerAddress("Avenida Siempreviva", "742"),
            backendUserId = 17,
        )
        sessions.saveSession(AuthSession(user, "valid-instrumented-jwt"))

        installations = EncryptedInstallationStateStore(installPrefs)
        binding = installations.prepare(17, "instrumented-logout")
        installations.confirm(binding)

        dismissal = AndroidNotificationDismissal(context)
        fakeAuthProvider = FakeLogoutAuthProvider()

        val userRepository = object : UserRepository {
            override suspend fun getCurrentUser() = CurrentUserOutcome.Success(user)
            override suspend fun registerConsumer(data: com.loresuelvo.consumer.domain.auth.RegisterConsumerData) =
                com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome.Success(user)
        }

        sessionViewModel = SessionViewModel(
            sessionStore = sessions,
            authProvider = fakeAuthProvider,
            restoreSession = RestoreAuthenticatedSessionUseCase(userRepository, sessions),
            pushRegistrationRequests = PushRegistrationRequests(),
            notificationDismissal = dismissal,
        )

        val clock = NotificationClock { System.currentTimeMillis() }
        val messageEvents = StoredNotificationEvents(installPrefs)
        val serviceEvents = StoredNotificationEvents(installPrefs)

        val authorizeMessage = AuthorizeMessageNotificationUseCase(sessions, installations, clock)
        val messagePublisher = AndroidMessageNotificationPublisher(context, authorizeMessage)
        val messageUseCase = ReceiveMessageNotificationUseCase(
            authorize = authorizeMessage,
            visibility = VisibleConversationStore(sessions),
            availability = messagePublisher,
            events = messageEvents,
            publisher = messagePublisher,
        )
        messageReceiver = ConsumerMessageReceiver(MessageNotificationDecoder(), messageUseCase)

        val authorizeService = AuthorizeServiceNotificationUseCase(sessions, installations, clock)
        val servicePublisher = AndroidServiceNotificationPublisher(context, authorizeService)
        val serviceUseCase = ReceiveServiceNotificationUseCase(
            authorize = authorizeService,
            availability = servicePublisher,
            events = serviceEvents,
            publisher = servicePublisher,
        )
        serviceReceiver = ConsumerServiceNotificationReceiver(ServiceNotificationDecoder(), serviceUseCase)
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
        sessions.clearSession()
    }

    @Test
    fun signOut_with_connection_cancels_visible_notifications_and_clears_session_immediately() {
        val messageOutcome = messageReceiver.receive(validMessagePayload(++messageSeq))
        assertEquals(MessageNotificationOutcome.Published, messageOutcome)
        val serviceOutcome = serviceReceiver.receive(validServicePayload(++serviceSeq))
        assertEquals(ServiceNotificationOutcome.Published, serviceOutcome)

        val activeBeforeLogout = notificationManager.activeNotifications
        assertTrue("Notifications must be visible in NotificationManager before logout", activeBeforeLogout.isNotEmpty())

        fakeAuthProvider.logoutOutcome = LogoutOutcome.Success

        sessionViewModel.signOut(context)

        assertNull("Local session must be cleared immediately", sessions.getSession())
        assertFalse("ViewModel state must report unauthenticated immediately", sessionViewModel.uiState.value.authenticated)
        assertNull("ViewModel session must be null", sessionViewModel.uiState.value.session)

        awaitNoActiveNotifications("All notifications must be cancelled from NotificationManager")

        val subsequentMessage = messageReceiver.receive(validMessagePayload(++messageSeq))
        assertEquals(MessageNotificationOutcome.Invalid, subsequentMessage)

        val subsequentService = serviceReceiver.receive(validServicePayload(++serviceSeq))
        assertEquals(ServiceNotificationOutcome.Invalid, subsequentService)

        assertEquals("No subsequent notifications should be published to NotificationManager", 0, notificationManager.activeNotifications.size)

        assertEquals(Route.Welcome.path, SessionRouteMapper.routeFor(sessionViewModel.uiState.value))
    }

    @Test
    fun signOut_without_connection_cancels_visible_notifications_and_clears_session_immediately() {
        val messageOutcome = messageReceiver.receive(validMessagePayload(++messageSeq))
        assertEquals(MessageNotificationOutcome.Published, messageOutcome)

        val activeBeforeLogout = notificationManager.activeNotifications
        assertTrue("Message notification must be visible before logout", activeBeforeLogout.isNotEmpty())

        fakeAuthProvider.logoutOutcome = LogoutOutcome.Failure.Provider(IOException("No network connection"))

        sessionViewModel.signOut(context)

        assertNull("Local session must be cleared immediately even without connection", sessions.getSession())
        assertFalse("ViewModel state must report unauthenticated", sessionViewModel.uiState.value.authenticated)
        assertNull("ViewModel session must be null", sessionViewModel.uiState.value.session)

        awaitNoActiveNotifications("Notifications must be cancelled even when sign out happened offline")

        val subsequentMessage = messageReceiver.receive(validMessagePayload(++messageSeq))
        assertEquals(MessageNotificationOutcome.Invalid, subsequentMessage)
        assertEquals(0, notificationManager.activeNotifications.size)

        assertEquals(Route.Welcome.path, SessionRouteMapper.routeFor(sessionViewModel.uiState.value))
    }

    private fun awaitNoActiveNotifications(message: String, timeoutMs: Long = 3000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline && notificationManager.activeNotifications.isNotEmpty()) {
            Thread.sleep(50)
        }
        assertEquals(message, 0, notificationManager.activeNotifications.size)
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

    class FakeLogoutAuthProvider : AuthProvider {
        var logoutOutcome: LogoutOutcome = LogoutOutcome.Success
        override suspend fun login(context: Context): com.loresuelvo.consumer.domain.auth.AuthenticationOutcome =
            throw UnsupportedOperationException()
        override suspend fun signup(context: Context): com.loresuelvo.consumer.domain.auth.AuthenticationOutcome =
            throw UnsupportedOperationException()
        override suspend fun loginWithGoogle(context: Context): com.loresuelvo.consumer.domain.auth.AuthenticationOutcome =
            throw UnsupportedOperationException()
        override suspend fun logout(context: Context): LogoutOutcome = logoutOutcome
    }
}
