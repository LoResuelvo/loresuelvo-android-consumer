package com.loresuelvo.consumer.bdd.notifications

import android.content.SharedPreferences
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
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
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.installation.RegisterInstallationUseCase
import com.loresuelvo.consumer.platform.notifications.InstallationRegistrationCoordinator
import com.loresuelvo.consumer.platform.notifications.RegistrationLocaleProvider
import com.loresuelvo.consumer.ui.notifications.PushRegistrationRequests
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConsumerRecoveryNotificationsWorld {
    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val sessionStore = InMemorySessionStore()
    private val requests = PushRegistrationRequests()
    private lateinit var preferences: SharedPreferences
    private lateinit var store: EncryptedInstallationStateStore
    private lateinit var repository: FakeInstallationRepository
    private lateinit var coordinator: InstallationRegistrationCoordinator
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var turnosRepository: TurnosRepository

    fun startWithVerifiedSession() {
        preferences = fakePreferences()
        store = EncryptedInstallationStateStore(preferences)
        repository = FakeInstallationRepository()

        val verifiedUser = User(
            displayName = "Verified Consumer",
            firstName = "Verified",
            lastName = "Consumer",
            email = "consumer@example.test",
            address = RegisterConsumerAddress("Test street", "1"),
            backendUserId = 17,
        )
        sessionStore.saveSession(AuthSession(verifiedUser, "verified-jwt"))

        val tokenProvider = object : PushRegistrationTokenProvider {
            override suspend fun token() = PushTokenOutcome.Available("test-fcm-token")
        }
        val useCase = RegisterInstallationUseCase(tokenProvider, store, repository)
        coordinator = InstallationRegistrationCoordinator(
            requests = requests,
            sessions = sessionStore,
            register = useCase,
            locale = RegistrationLocaleProvider { "es" },
            scope = scope,
        )
        coordinator.start()

        val conversation = Conversation(
            id = "42",
            status = ConversationStatus.Pending,
            counterpart = ConversationCounterpart(20L, "Juan", "Prestador", "Plomería", null),
            lastMessage = ConversationMessage("51", ConversationSender.Provider, "Hola!", 1000L),
            updatedOnEpochMillis = 1000L,
        )
        conversationRepository = mockk {
            coEvery { getConversations() } returns ConversationsOutcome.Success(listOf(conversation))
        }

        val turno = Turno(
            id = "77",
            serviceProposalId = "88",
            status = TurnoStatus.Confirmed,
            counterpart = TurnoCounterpart("20", "Juan", "Prestador", "Plomería", null),
            description = "Reparación",
            amountCents = 500000L,
            scheduledOnEpochMillis = 2000L,
        )
        turnosRepository = mockk {
            coEvery { getTurnos() } returns TurnosOutcome.Success(listOf(turno))
        }
    }

    fun interruptionWhenEnabling(interruption: String) {
        when (interruption) {
            "falla la conexión" -> repository.mode = FakeInstallationRepository.Mode.NETWORK_FAILURE
            "no llega la confirmación del registro" -> repository.mode = FakeInstallationRepository.Mode.SERVER_FAILURE
            else -> error("Unknown interruption: $interruption")
        }

        requests.request()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse("Store must not be confirmed during interruption", preferences.getBoolean("confirmed", false))
        val outcome = coordinator.outcome.value
        assertFalse("Registration outcome must not be success during interruption", outcome is RegistrationOutcome.Success)
    }

    fun resumeWithConnectionAvailable() {
        repository.mode = FakeInstallationRepository.Mode.SUCCESS
        requests.request()
        dispatcher.scheduler.advanceUntilIdle()
    }

    fun assertPhoneRegistered() {
        val outcome = coordinator.outcome.value
        assertTrue("Outcome must be RegistrationOutcome.Success after recovery, was: $outcome", outcome is RegistrationOutcome.Success)
        assertTrue("Store must be confirmed after recovery", preferences.getBoolean("confirmed", false))
    }

    fun assertCanConsultMessagesAndServices() = runBlocking {
        val conversations = conversationRepository.getConversations()
        assertTrue("Conversations query must succeed during/after recovery", conversations is ConversationsOutcome.Success)
        val turnos = turnosRepository.getTurnos()
        assertTrue("Turnos query must succeed during/after recovery", turnos is TurnosOutcome.Success)
    }

    fun close() {
        scope.cancel()
    }

    private fun fakePreferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { prefs.getString(any(), any()) } answers { values[firstArg<String>()] as? String ?: secondArg() }
        every { prefs.getInt(any(), any()) } answers { values[firstArg<String>()] as? Int ?: secondArg() }
        every { prefs.getBoolean(any(), any()) } answers { values[firstArg<String>()] as? Boolean ?: secondArg() }
        every { prefs.contains(any()) } answers { values.containsKey(firstArg<String>()) }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers { values[firstArg<String>()] = secondArg<String?>(); editor }
        every { editor.putInt(any(), any()) } answers { values[firstArg<String>()] = secondArg<Int>(); editor }
        every { editor.putBoolean(any(), any()) } answers { values[firstArg<String>()] = secondArg<Boolean>(); editor }
        every { editor.remove(any()) } answers { values.remove(firstArg<String>()); editor }
        every { editor.clear() } answers { values.clear(); editor }
        every { editor.apply() } returns Unit
        every { editor.commit() } returns true
        return prefs
    }

    private class InMemorySessionStore : AuthSessionStore {
        override val sessionFlow = MutableStateFlow<AuthSession?>(null)
        private var persisted: AuthSession? = null

        override fun getSession() = persisted
        override fun persistSession(session: AuthSession) {
            persisted = session
        }
        override fun saveSession(session: AuthSession) {
            persistSession(session)
            sessionFlow.value = session
        }
        override fun clearSession() {
            persisted = null
            sessionFlow.value = null
        }
    }

    private class FakeInstallationRepository : InstallationRepository {
        enum class Mode { NETWORK_FAILURE, SERVER_FAILURE, SUCCESS }
        var mode: Mode = Mode.SUCCESS

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
