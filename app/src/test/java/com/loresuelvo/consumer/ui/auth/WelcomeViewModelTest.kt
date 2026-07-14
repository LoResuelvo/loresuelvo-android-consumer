package com.loresuelvo.consumer.ui.auth

import android.content.Context
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthenticationOutcome
import com.loresuelvo.consumer.domain.auth.AuthProvider
import com.loresuelvo.consumer.domain.auth.SessionSynchronizationOutcome
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.usecase.auth.SyncAuthenticatedSessionUseCase
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [WelcomeViewModel]'s category-loading behaviour.
 * The screen shows exactly one of loading / ready / error, and an
 * empty backend response collapses to the error state (product
 * decision). Failures never crash the VM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authProvider = mockk<AuthProvider>(relaxed = true)
    private val getCategories = mockk<GetCategoriesUseCase>()
    private val syncSession = mockk<SyncAuthenticatedSessionUseCase>()
    private val context = mockk<Context>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        WelcomeViewModel(authProvider, syncSession, getCategories)

    private val authenticatedSession = AuthSession(
        user = User(displayName = "Ana", email = "ana@example.com"),
        accessToken = "access-token",
    )

    @Test
    fun login_uses_login_flow_and_synchronizes_backend_profile() = runTest {
        coEvery { getCategories() } returns CategoriesOutcome.Success(emptyList())
        coEvery { authProvider.login(context) } returns
            AuthenticationOutcome.Success(authenticatedSession)
        coEvery { syncSession(authenticatedSession) } returns
            SessionSynchronizationOutcome.Success(authenticatedSession)

        val viewModel = createViewModel()
        viewModel.login(context)
        advanceUntilIdle()

        coVerify(exactly = 1) { authProvider.login(context) }
        coVerify(exactly = 0) { authProvider.signup(any()) }
        coVerify(exactly = 1) { syncSession(authenticatedSession) }
        assertEquals(false, viewModel.uiState.value.loading)
    }

    @Test
    fun signup_uses_signup_flow_and_synchronizes_backend_profile() = runTest {
        coEvery { getCategories() } returns CategoriesOutcome.Success(emptyList())
        coEvery { authProvider.signup(context) } returns
            AuthenticationOutcome.Success(authenticatedSession)
        coEvery { syncSession(authenticatedSession) } returns
            SessionSynchronizationOutcome.Success(authenticatedSession)

        val viewModel = createViewModel()
        viewModel.signup(context)
        advanceUntilIdle()

        coVerify(exactly = 1) { authProvider.signup(context) }
        coVerify(exactly = 0) { authProvider.login(any()) }
        coVerify(exactly = 1) { syncSession(authenticatedSession) }
    }

    @Test
    fun google_action_uses_google_authentication_flow() = runTest {
        coEvery { getCategories() } returns CategoriesOutcome.Success(emptyList())
        coEvery { authProvider.loginWithGoogle(context) } returns
            AuthenticationOutcome.Success(authenticatedSession)
        coEvery { syncSession(authenticatedSession) } returns
            SessionSynchronizationOutcome.Success(authenticatedSession)

        val viewModel = createViewModel()
        viewModel.loginWithGoogle(context)
        advanceUntilIdle()

        coVerify(exactly = 1) { authProvider.loginWithGoogle(context) }
        coVerify(exactly = 1) { syncSession(authenticatedSession) }
    }

    @Test
    fun loads_categories_into_Ready_state_on_success() = runTest {
        val categories = listOf(Category(1, "Plomería"), Category(2, "Electricidad"))
        coEvery { getCategories() } returns CategoriesOutcome.Success(categories)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value.categories
        assertTrue("expected Ready, was $state", state is WelcomeCategoriesUiState.Ready)
        assertEquals(categories, (state as WelcomeCategoriesUiState.Ready).categories)
    }

    @Test
    fun empty_backend_response_collapses_to_Error_state() = runTest {
        coEvery { getCategories() } returns CategoriesOutcome.Success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(
            WelcomeCategoriesUiState.Error,
            viewModel.uiState.value.categories,
        )
    }

    @Test
    fun network_failure_sets_Error_state() = runTest {
        coEvery { getCategories() } returns
            CategoriesOutcome.Failure.Network(IOException("dns"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(
            WelcomeCategoriesUiState.Error,
            viewModel.uiState.value.categories,
        )
    }

    @Test
    fun server_failure_sets_Error_state() = runTest {
        coEvery { getCategories() } returns
            CategoriesOutcome.Failure.Server(500, "boom")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(
            WelcomeCategoriesUiState.Error,
            viewModel.uiState.value.categories,
        )
    }

    @Test
    fun initial_state_is_Loading_before_fetch_completes() = runTest {
        coEvery { getCategories() } returns CategoriesOutcome.Success(
            listOf(Category(1, "Plomería")),
        )

        val viewModel = createViewModel()
        // Do not advance the dispatcher: the init coroutine is still
        // suspended, so the state must still be the default Loading.
        assertEquals(
            WelcomeCategoriesUiState.Loading,
            viewModel.uiState.value.categories,
        )
    }
}
