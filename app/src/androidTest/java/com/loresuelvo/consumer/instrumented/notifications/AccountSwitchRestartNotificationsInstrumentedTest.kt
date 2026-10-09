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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 33)
@RunWith(AndroidJUnit4::class)
class AccountSwitchRestartNotificationsInstrumentedTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var user1Binding: InstallationBinding
    private lateinit var user2Binding: InstallationBinding
    private lateinit var fakeAuthProvider: FakeSwitchAuthProvider
    private lateinit var sessionViewModel: SessionViewModel

    private lateinit var messageReceiver: ConsumerMessageReceiver
    private lateinit var serviceReceiver: ConsumerServiceNotificationReceiver
    private var messageSeq = 700
    private var serviceSeq = 800

    private val user1 = User(
        displayName = "Usuario Anterior",
        firstName = "Usuario",
        lastName = "Anterior",
        email = "user1@example.test",
        address = RegisterConsumerAddress("Avenida Anterior", "111"),
        backendUserId = 11,
    )

    val user2 = User(
        displayName = "Usuario Actual",
        firstName = "Usuario",
        lastName = "Actual",
        email = "user2@example.test",
        address = RegisterConsumerAddress("Avenida Actual", "222"),
        backendUserId = 22,
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
        fakeAuthProvider = FakeSwitchAuthProvider()
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
        if (::sessions.isInitialized) {
            sessions.clearSession()
        }
    }

    @Test
    fun account_switch_after_restart_discards_old_account_notices_and_receives_current_account_notices() {
        val suffix = UUID.randomUUID().toString().take(8)
        val sessionPrefs = context.getSharedPreferences("test_switch_sessions_$suffix", Context.MODE_PRIVATE)
        val installPrefs = context.getSharedPreferences("test_switch_installations_$suffix", Context.MODE_PRIVATE)
        sessionPrefs.edit().clear().commit()
        installPrefs.edit().clear().commit()

        // 1. Iniciar con cuenta 1
        sessions = EncryptedAuthSessionStore(sessionPrefs)
        sessions.saveSession(AuthSession(user1, "token-user-1"))

        installations = EncryptedInstallationStateStore(installPrefs)
        user1Binding = installations.prepare(11, "login-user-1")
        installations.confirm(user1Binding)

        val dismissal = AndroidNotificationDismissal(context)
        val userRepository = object : UserRepository {
            override suspend fun getCurrentUser() = CurrentUserOutcome.Success(user1)
            override suspend fun registerConsumer(data: com.loresuelvo.consumer.domain.auth.RegisterConsumerData) =
                com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome.Success(user1)
        }

        sessionViewModel = SessionViewModel(
            sessionStore = sessions,
            authProvider = fakeAuthProvider,
            restoreSession = RestoreAuthenticatedSessionUseCase(userRepository, sessions),
            pushRegistrationRequests = PushRegistrationRequests(),
            notificationDismissal = dismissal,
        )

        // 2. Cerrar sesión sin conexión
        fakeAuthProvider.logoutOutcome = LogoutOutcome.Failure.Provider(IOException("Offline"))
        sessionViewModel.signOut(context)
        assertNull("Sesión local debe quedar cerrada inmediatamente", sessions.getSession())
        assertFalse(sessionViewModel.uiState.value.authenticated)
        assertEquals(0, notificationManager.activeNotifications.size)

        // 3. Ingresar con cuenta 2
        sessions.saveSession(AuthSession(user2, "token-user-2"))
        user2Binding = installations.prepare(22, "login-user-2")
        installations.confirm(user2Binding)

        val confirmedUser2 = installations.confirmedInstallation()
        assertNotNull(confirmedUser2)
        assertEquals(22, confirmedUser2!!.userId)
        assertEquals(user2Binding.id, confirmedUser2.bindingId)
        assertNotEquals(user1Binding.id, confirmedUser2.bindingId)

        // 4. Reinicio de LoResuelvo (recrear stores a partir de preferencias persistidas en disco)
        sessions = EncryptedAuthSessionStore(sessionPrefs)
        installations = EncryptedInstallationStateStore(installPrefs)

        val restoredSession = sessions.getSession()
        assertNotNull("La sesión debe persistir tras reinicio", restoredSession)
        assertEquals("La sesión persistida debe ser la de Usuario 2", 22, restoredSession!!.user.backendUserId)

        val restoredInstallation = installations.confirmedInstallation()
        assertNotNull("El registro persistido debe ser el de Usuario 2", restoredInstallation)
        assertEquals(22, restoredInstallation!!.userId)
        assertEquals(user2Binding.id, restoredInstallation.bindingId)

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

        // 5. Llega aviso de la cuenta anterior (Usuario 1)
        val oldMessagePayload = mapOf(
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
        val oldMessageOutcome = messageReceiver.receive(oldMessagePayload)
        assertEquals("El aviso de la cuenta anterior debe ser rechazado como Invalid",
            MessageNotificationOutcome.Invalid, oldMessageOutcome)

        val oldServicePayload = mapOf(
            "version" to "1",
            "event_id" to "notification:service_proposal_received:${++serviceSeq}",
            "type" to "service_proposal_received",
            "resource_type" to "service_proposal",
            "destination" to "service_proposal",
            "resource_id" to "99",
            "recipient_user_id" to "11",
            "recipient_app" to "consumer",
            "installation_id" to user1Binding.identity.id,
            "binding_id" to user1Binding.id,
            "title" to "Nueva propuesta",
            "body" to "Recibiste una propuesta de servicio.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )
        val oldServiceOutcome = serviceReceiver.receive(oldServicePayload)
        assertEquals("El aviso de servicio de la cuenta anterior debe ser rechazado como Invalid",
            ServiceNotificationOutcome.Invalid, oldServiceOutcome)

        // No se muestra ninguna notificación en el teléfono
        assertEquals("No debe haber notificaciones en NotificationManager para la cuenta vieja",
            0, notificationManager.activeNotifications.size)

        // No permite entrar a la cuenta anterior: la sesión activa pertenece estrictamente a Usuario 2
        assertEquals(22, sessions.getSession()?.user?.backendUserId)

        // 6. Llega aviso legítimo de la cuenta actual (Usuario 2)
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
        val currentMessageOutcome = messageReceiver.receive(currentMessagePayload)
        assertEquals("El aviso de la cuenta actual debe ser publicado",
            MessageNotificationOutcome.Published, currentMessageOutcome)
        assertEquals("Debe haber 1 notificación activa en NotificationManager",
            1, notificationManager.activeNotifications.size)

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
        val currentServiceOutcome = serviceReceiver.receive(currentServicePayload)
        assertEquals("El aviso de servicio de la cuenta actual debe ser publicado",
            ServiceNotificationOutcome.Published, currentServiceOutcome)
        assertEquals("Debe haber 2 notificaciones activas en NotificationManager",
            2, notificationManager.activeNotifications.size)
    }

    class FakeSwitchAuthProvider : AuthProvider {
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
