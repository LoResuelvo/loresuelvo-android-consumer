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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun should_clear_local_session_after_auth0_logout_succeeds() = runTest {
        coEvery { authProvider.logout(context) } returns LogoutOutcome.Success
        val viewModel = SessionViewModel(sessionStore, authProvider)

        viewModel.signOut(context)
        advanceUntilIdle()

        coVerify { authProvider.logout(context) }
        verify { sessionStore.clearSession() }
        assertFalse(viewModel.uiState.value.signingOut)
    }

    @Test
    fun should_keep_local_session_when_auth0_logout_fails() = runTest {
        coEvery { authProvider.logout(context) } returns
            LogoutOutcome.Failure.Provider(IllegalStateException("Auth0 unavailable"))
        val viewModel = SessionViewModel(sessionStore, authProvider)

        viewModel.signOut(context)
        advanceUntilIdle()

        verify(exactly = 0) { sessionStore.clearSession() }
        assertFalse(viewModel.uiState.value.signingOut)
        assertTrue(viewModel.uiState.value.error is SessionError.Logout)
    }
}
