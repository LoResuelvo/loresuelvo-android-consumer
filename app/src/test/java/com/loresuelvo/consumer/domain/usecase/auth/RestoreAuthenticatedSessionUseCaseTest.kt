package com.loresuelvo.consumer.domain.usecase.auth

import com.loresuelvo.consumer.domain.auth.*
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RestoreAuthenticatedSessionUseCaseTest {
    @Test
    fun only_not_found_removes_cached_profile_and_server_failure_preserves_it() = runTest {
        val session = AuthSession(User("Ana", "Ana", "Perez", address = RegisterConsumerAddress("Street", "1")), "token")
        val flow = MutableStateFlow<AuthSession?>(session)
        val store = mockk<AuthSessionStore>()
        every { store.sessionFlow } returns flow
        every { store.saveSession(any()) } answers { flow.value = firstArg() }
        val repository = mockk<UserRepository>()
        val restore = RestoreAuthenticatedSessionUseCase(repository, store)
        coEvery { repository.getCurrentUser() } returns CurrentUserOutcome.Failure.Server(503, "Unavailable")
        restore(session)
        assertEquals(session, flow.value)
        coEvery { repository.getCurrentUser() } returns CurrentUserOutcome.NotFound
        restore(session)
        assertNull(flow.value?.user?.address)
    }

    @Test
    fun delayed_response_does_not_restore_logged_out_session() = runTest {
        val session = AuthSession(User("Ana"), "token")
        val flow = MutableStateFlow<AuthSession?>(session)
        val store = mockk<AuthSessionStore>()
        every { store.sessionFlow } returns flow
        val repository = mockk<UserRepository>()
        val response = CompletableDeferred<CurrentUserOutcome>()
        val started = CompletableDeferred<Unit>()
        coEvery { repository.getCurrentUser() } coAnswers { started.complete(Unit); response.await() }
        val job = launch { RestoreAuthenticatedSessionUseCase(repository, store)(session) }
        started.await()
        flow.value = null
        response.complete(CurrentUserOutcome.Success(session.user))
        job.join()
        verify(exactly = 0) { store.saveSession(any()) }
        assertNull(flow.value)
    }
}
