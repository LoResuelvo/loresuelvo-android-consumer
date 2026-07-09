package com.loresuelvo.consumer.domain.usecase.auth

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [RegisterConsumerUseCase]. Mirrors the coverage of
 * the webapp's `registerUser` use case (see
 * `loresuelvo-webapp/application/onboarding/register-user.test.ts`):
 * 6 cases, each a single behaviour.
 */
class RegisterConsumerUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val sessionStore = mockk<AuthSessionStore>(relaxed = true)
    private val useCase = RegisterConsumerUseCase(userRepository, sessionStore)

    private fun sessionWithEmail(email: String? = "ana@example.com"): AuthSession = AuthSession(
        user = User(
            displayName = "Ana",
            firstName = "Ana",
            lastName = "Perez",
            email = email,
        ),
        accessToken = "test-token",
    )

    @Test
    fun returns_Unauthorized_when_no_active_session() = runTest {
        every { sessionStore.getSession() } returns null

        val outcome = useCase(RegisterConsumerCommand("Andres", "Colina"))

        assertTrue(outcome is UserRegistrationOutcome.Failure.Unauthorized)
        val failure = outcome as UserRegistrationOutcome.Failure.Unauthorized
        assertEquals("Sin sesión activa", failure.message)
        coVerify(exactly = 0) { userRepository.registerConsumer(any()) }
        verify(exactly = 0) { sessionStore.saveSession(any()) }
    }

    @Test
    fun returns_Server_when_session_email_is_null() = runTest {
        every { sessionStore.getSession() } returns sessionWithEmail(email = null)

        val outcome = useCase(RegisterConsumerCommand("Andres", "Colina"))

        assertTrue(outcome is UserRegistrationOutcome.Failure.Server)
        val failure = outcome as UserRegistrationOutcome.Failure.Server
        assertEquals(0, failure.code)
        assertEquals("Email ausente en la sesión", failure.message)
        coVerify(exactly = 0) { userRepository.registerConsumer(any()) }
        verify(exactly = 0) { sessionStore.saveSession(any()) }
    }

    @Test
    fun calls_repository_with_trimmed_command_and_persists_session_on_Success() = runTest {
        every { sessionStore.getSession() } returns sessionWithEmail()
        coEvery { userRepository.registerConsumer(any()) } returns UserRegistrationOutcome.Success(
            User(
                displayName = "Andres",
                firstName = "Andres",
                lastName = "Colina",
                email = "ana@example.com",
            )
        )

        val outcome = useCase(RegisterConsumerCommand("  Andres  ", "  Colina  "))

        assertTrue(outcome is UserRegistrationOutcome.Success)
        val dataSlot = slot<RegisterConsumerData>()
        coVerify { userRepository.registerConsumer(capture(dataSlot)) }
        assertEquals("ana@example.com", dataSlot.captured.email)
        assertEquals("Andres", dataSlot.captured.firstName)
        assertEquals("Colina", dataSlot.captured.lastName)

        // Session must be updated with the new firstName / lastName so
        // the navigation graph can read isProfileComplete() == true
        // without waiting for a session reload.
        val savedSlot = slot<AuthSession>()
        verify { sessionStore.saveSession(capture(savedSlot)) }
        assertEquals("Andres", savedSlot.captured.user.firstName)
        assertEquals("Colina", savedSlot.captured.user.lastName)
    }

    @Test
    fun does_not_save_session_on_Failure_Network() = runTest {
        every { sessionStore.getSession() } returns sessionWithEmail()
        coEvery { userRepository.registerConsumer(any()) } returns
            UserRegistrationOutcome.Failure.Network(IOException("dns error"))

        val outcome = useCase(RegisterConsumerCommand("A", "B"))

        assertTrue(outcome is UserRegistrationOutcome.Failure.Network)
        verify(exactly = 0) { sessionStore.saveSession(any()) }
    }

    @Test
    fun propagates_Failure_Server_from_repository_without_saving_session() = runTest {
        every { sessionStore.getSession() } returns sessionWithEmail()
        coEvery { userRepository.registerConsumer(any()) } returns
            UserRegistrationOutcome.Failure.Server(code = 409, message = "Email is already registered")

        val outcome = useCase(RegisterConsumerCommand("A", "B"))

        assertTrue(outcome is UserRegistrationOutcome.Failure.Server)
        val failure = outcome as UserRegistrationOutcome.Failure.Server
        assertEquals(409, failure.code)
        assertEquals("Email is already registered", failure.message)
        verify(exactly = 0) { sessionStore.saveSession(any()) }
    }

    @Test
    fun propagates_Failure_Unauthorized_from_repository_without_saving_session() = runTest {
        every { sessionStore.getSession() } returns sessionWithEmail()
        coEvery { userRepository.registerConsumer(any()) } returns
            UserRegistrationOutcome.Failure.Unauthorized("Token expired")

        val outcome = useCase(RegisterConsumerCommand("A", "B"))

        assertTrue(outcome is UserRegistrationOutcome.Failure.Unauthorized)
        val failure = outcome as UserRegistrationOutcome.Failure.Unauthorized
        assertEquals("Token expired", failure.message)
        verify(exactly = 0) { sessionStore.saveSession(any()) }
        // Note: this use case does NOT clear the session on 401. The
        // ViewModel owns that decision (per the AGENTS.md rule:
        // "use cases don't translate to UI actions").
    }
}
