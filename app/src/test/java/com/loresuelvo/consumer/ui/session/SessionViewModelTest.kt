package com.loresuelvo.consumer.ui.session

import android.content.Context
import com.loresuelvo.consumer.domain.auth.AuthProvider
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.LogoutOutcome
import com.loresuelvo.consumer.domain.auth.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SessionViewModel] covering the local-first
 * sign-out policy.
 *
 * Behaviour pinned:
 *  - `signOut(context)` MUST call [AuthSessionStore.clearSession]
 *    synchronously (before returning), regardless of whether the
 *    subsequent Auth0 SSO call succeeds, fails, or hangs.
 *  - It MUST also dispatch [AuthProvider.logout] in the background
 *    so the SDK session token is invalidated when the network is
 *    reachable; the Auth0 result is fire-and-forget — the
 *    consumer is already back at Welcome and can retry the next
 *    time they log in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val authProvider = mockk<AuthProvider>()
    private val sessionStore = mockk<AuthSessionStore>(relaxed = true)
    private val context = mockk<Context>()
    private val session = AuthSession(User("Ana", "Ana", "Perez"), "token")
    private val sessionFlow = MutableStateFlow<AuthSession?>(session)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { sessionStore.sessionFlow } returns sessionFlow
        // Wire `clearSession()` to actually mutate the backing flow
        // so the VM's `init { sessionFlow.collect { ... }` observer
        // emits the null and the resulting `_uiState.session` goes
        // to null. `mockk(relaxed = true)` would otherwise no-op the
        // call and leave the test asserting against the stale value.
        every { sessionStore.clearSession() } answers {
            sessionFlow.value = null
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signOut_clears_local_session_synchronously_when_auth0_logout_succeeds() = runTest {
        coEvery { authProvider.logout(context) } returns LogoutOutcome.Success
        val viewModel = SessionViewModel(sessionStore, authProvider)

        viewModel.signOut(context)
        advanceUntilIdle()

        // Local session is gone BEFORE the Auth0 call returns so
        // the smart-router observes the change before
        // `signOut` returns to the click handler.
        verify { sessionStore.clearSession() }
        coVerify { authProvider.logout(context) }
        assertNull(viewModel.uiState.value.session)
    }

    @Test
    fun signOut_clears_local_session_when_auth0_logout_fails() = runTest {
        // The previous policy kept the local session when Auth0
        // failed; the new one clears it regardless because the
        // consumer has already pressed "Cerrar sesión" and the
        // smart-router must land them on Welcome. The Auth0 failure
        // is logged for diagnostics but does not block local
        // sign-out.
        coEvery { authProvider.logout(context) } returns
            LogoutOutcome.Failure.Provider(IllegalStateException("Auth0 unavailable"))
        val viewModel = SessionViewModel(sessionStore, authProvider)

        viewModel.signOut(context)
        advanceUntilIdle()

        verify { sessionStore.clearSession() }
        coVerify { authProvider.logout(context) }
        assertNull(viewModel.uiState.value.session)
    }

    @Test
    fun signOut_clears_local_session_when_auth0_logout_is_cancelled() = runTest {
        coEvery { authProvider.logout(context) } returns LogoutOutcome.Cancelled
        val viewModel = SessionViewModel(sessionStore, authProvider)

        viewModel.signOut(context)
        advanceUntilIdle()

        verify { sessionStore.clearSession() }
        coVerify { authProvider.logout(context) }
        assertNull(viewModel.uiState.value.session)
    }
}
