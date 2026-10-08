package com.loresuelvo.consumer.instrumented.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.installation.InstallationConfirmation
import com.loresuelvo.consumer.domain.installation.InstallationRegistrationResult
import com.loresuelvo.consumer.domain.installation.InstallationRepository
import com.loresuelvo.consumer.domain.installation.PushRegistrationTokenProvider
import com.loresuelvo.consumer.domain.installation.PushTokenOutcome
import com.loresuelvo.consumer.domain.installation.RegistrationOutcome
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.usecase.installation.RegisterInstallationUseCase
import com.loresuelvo.consumer.platform.notifications.InstallationRegistrationCoordinator
import com.loresuelvo.consumer.platform.notifications.RegistrationLocaleProvider
import com.loresuelvo.consumer.testdi.FakeConversationRepository
import com.loresuelvo.consumer.testdi.FakeTurnosRepository
import com.loresuelvo.consumer.ui.notifications.PushRegistrationRequests
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 33)
@RunWith(AndroidJUnit4::class)
class RegistrationRecoveryInstrumentedTest {

    private lateinit var context: Context
    private lateinit var scope: CoroutineScope
    private lateinit var sessionPrefs: SharedPreferences
    private lateinit var installationPrefs: SharedPreferences
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var repository: DynamicInstallationRepository
    private lateinit var requests: PushRegistrationRequests
    private lateinit var coordinator: InstallationRegistrationCoordinator
    private lateinit var fakeConversations: FakeConversationRepository
    private lateinit var fakeTurnos: FakeTurnosRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        sessionPrefs = context.getSharedPreferences("test_recovery_session_prefs", Context.MODE_PRIVATE)
        installationPrefs = context.getSharedPreferences("test_recovery_install_prefs", Context.MODE_PRIVATE)
        sessionPrefs.edit().clear().commit()
        installationPrefs.edit().clear().commit()

        sessions = EncryptedAuthSessionStore(sessionPrefs)
        val user = User(
            displayName = "Verified Consumer",
            firstName = "Verified",
            lastName = "Consumer",
            email = "consumer@example.test",
            address = RegisterConsumerAddress("Test street", "1"),
            backendUserId = 17,
        )
        sessions.saveSession(AuthSession(user, "test-jwt"))

        installations = EncryptedInstallationStateStore(installationPrefs)
        repository = DynamicInstallationRepository()
        requests = PushRegistrationRequests()

        val tokenProvider = object : PushRegistrationTokenProvider {
            override suspend fun token() = PushTokenOutcome.Available("instrumented-fcm-token")
        }
        val useCase = RegisterInstallationUseCase(tokenProvider, installations, repository)
        coordinator = InstallationRegistrationCoordinator(
            requests = requests,
            sessions = sessions,
            register = useCase,
            locale = RegistrationLocaleProvider { "es" },
            scope = scope,
        )
        coordinator.start()

        fakeConversations = FakeConversationRepository()
        fakeConversations.setConversationsSeed(
            listOf(
                Conversation(
                    id = "42",
                    status = ConversationStatus.Pending,
                    counterpart = ConversationCounterpart(20L, "Juan", "Prestador", "Plomería", null),
                    lastMessage = ConversationMessage("51", ConversationSender.Provider, "Hola!", 1000L),
                    updatedOnEpochMillis = 1000L,
                ),
            ),
        )

        fakeTurnos = FakeTurnosRepository()
        fakeTurnos.set(
            listOf(
                Turno(
                    id = "77",
                    serviceProposalId = "88",
                    status = TurnoStatus.Confirmed,
                    counterpart = TurnoCounterpart("20", "Juan", "Prestador", "Plomería", null),
                    description = "Reparación",
                    amountCents = 500000L,
                    scheduledOnEpochMillis = 2000L,
                ),
            ),
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun recovery_from_network_failure_registers_phone_and_maintains_access() = runBlocking {
        repository.mode = DynamicInstallationRepository.Mode.NETWORK_FAILURE
        requests.request()

        val failedOutcome = withTimeout(5000) {
            coordinator.outcome.filterNotNull().first { it !is RegistrationOutcome.Success }
        }
        assertTrue("Expected NetworkFailure, got $failedOutcome", failedOutcome is RegistrationOutcome.NetworkFailure)
        assertFalse("Store must not be confirmed during network failure", installationPrefs.getBoolean("confirmed", false))

        val conversationsOutcome = fakeConversations.getConversations()
        assertTrue("Conversations must be accessible during recovery", conversationsOutcome is ConversationsOutcome.Success)
        val turnosOutcome = fakeTurnos.getTurnos()
        assertTrue("Services/turnos must be accessible during recovery", turnosOutcome is TurnosOutcome.Success)

        repository.mode = DynamicInstallationRepository.Mode.SUCCESS
        requests.request()

        val successOutcome = withTimeout(5000) {
            coordinator.outcome.filterNotNull().first { it is RegistrationOutcome.Success }
        }
        assertTrue("Expected Success, got $successOutcome", successOutcome is RegistrationOutcome.Success)
        assertTrue("Store must be confirmed after recovery", installationPrefs.getBoolean("confirmed", false))

        val finalConversations = fakeConversations.getConversations()
        assertTrue("Conversations must remain accessible after recovery", finalConversations is ConversationsOutcome.Success)
        val finalTurnos = fakeTurnos.getTurnos()
        assertTrue("Services/turnos must remain accessible after recovery", finalTurnos is TurnosOutcome.Success)
    }

    @Test
    fun recovery_from_server_failure_registers_phone_and_maintains_access() = runBlocking {
        repository.mode = DynamicInstallationRepository.Mode.SERVER_FAILURE
        requests.request()

        val failedOutcome = withTimeout(5000) {
            coordinator.outcome.filterNotNull().first { it !is RegistrationOutcome.Success }
        }
        assertTrue("Expected ServerFailure, got $failedOutcome", failedOutcome is RegistrationOutcome.ServerFailure)
        assertFalse("Store must not be confirmed during server failure", installationPrefs.getBoolean("confirmed", false))

        val conversationsOutcome = fakeConversations.getConversations()
        assertTrue("Conversations must be accessible during recovery", conversationsOutcome is ConversationsOutcome.Success)
        val turnosOutcome = fakeTurnos.getTurnos()
        assertTrue("Services/turnos must be accessible during recovery", turnosOutcome is TurnosOutcome.Success)

        repository.mode = DynamicInstallationRepository.Mode.SUCCESS
        requests.request()

        val successOutcome = withTimeout(5000) {
            coordinator.outcome.filterNotNull().first { it is RegistrationOutcome.Success }
        }
        assertTrue("Expected Success, got $successOutcome", successOutcome is RegistrationOutcome.Success)
        assertTrue("Store must be confirmed after recovery", installationPrefs.getBoolean("confirmed", false))

        val finalConversations = fakeConversations.getConversations()
        assertTrue("Conversations must remain accessible after recovery", finalConversations is ConversationsOutcome.Success)
        val finalTurnos = fakeTurnos.getTurnos()
        assertTrue("Services/turnos must remain accessible after recovery", finalTurnos is TurnosOutcome.Success)
    }

    private class DynamicInstallationRepository : InstallationRepository {
        enum class Mode { NETWORK_FAILURE, SERVER_FAILURE, SUCCESS }
        @Volatile var mode: Mode = Mode.SUCCESS

        override suspend fun register(
            binding: InstallationBinding,
            token: String,
            locale: String,
            accessToken: String,
        ): InstallationRegistrationResult = when (mode) {
            Mode.NETWORK_FAILURE -> InstallationRegistrationResult.Failed(
                RegistrationOutcome.NetworkFailure(IOException("Connection failed")),
            )
            Mode.SERVER_FAILURE -> InstallationRegistrationResult.Failed(
                RegistrationOutcome.ServerFailure(500),
            )
            Mode.SUCCESS -> InstallationRegistrationResult.Confirmed(
                InstallationConfirmation(
                    installationId = binding.identity.id,
                    bindingId = binding.id,
                    app = "consumer",
                    locale = locale,
                    enabled = true,
                ),
            )
        }
    }
}
