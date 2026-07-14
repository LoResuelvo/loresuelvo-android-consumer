package com.loresuelvo.consumer.domain.usecase.auth

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.SessionSynchronizationOutcome
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SyncAuthenticatedSessionUseCaseTest {

    private val repository = mockk<UserRepository>()
    private val sessionStore = mockk<AuthSessionStore>(relaxed = true)
    private val useCase = SyncAuthenticatedSessionUseCase(repository, sessionStore)

    private val auth0Session = AuthSession(
        user = User(displayName = "Ana", email = "ana@example.com"),
        accessToken = "access-token",
    )

    @Test
    fun should_restore_backend_profile_for_returning_consumer() = runTest {
        val persistedUser = User(
            displayName = "Ana Perez",
            firstName = "Ana",
            lastName = "Perez",
            email = "ana@example.com",
        )
        coEvery { repository.getCurrentUser() } returns CurrentUserOutcome.Success(persistedUser)

        val outcome = useCase(auth0Session)

        assertTrue(outcome is SessionSynchronizationOutcome.Success)
        val savedSessions = mutableListOf<AuthSession>()
        verify(exactly = 2) { sessionStore.saveSession(capture(savedSessions)) }
        assertEquals(persistedUser, savedSessions.last().user)
        assertTrue(savedSessions.last().user.isProfileComplete())
    }

    @Test
    fun should_keep_auth0_identity_when_consumer_does_not_exist_yet() = runTest {
        coEvery { repository.getCurrentUser() } returns CurrentUserOutcome.NotFound

        val outcome = useCase(auth0Session)

        assertTrue(outcome is SessionSynchronizationOutcome.Success)
        val saved = slot<AuthSession>()
        verify(exactly = 1) { sessionStore.saveSession(capture(saved)) }
        assertEquals(auth0Session, saved.captured)
        assertFalse(saved.captured.user.isProfileComplete())
    }

    @Test
    fun should_clear_provisional_session_when_profile_lookup_fails() = runTest {
        coEvery { repository.getCurrentUser() } returns
            CurrentUserOutcome.Failure.Network(IOException("offline"))

        val outcome = useCase(auth0Session)

        assertTrue(outcome is SessionSynchronizationOutcome.Failure.Network)
        verify { sessionStore.saveSession(auth0Session) }
        verify { sessionStore.clearSession() }
    }
}
