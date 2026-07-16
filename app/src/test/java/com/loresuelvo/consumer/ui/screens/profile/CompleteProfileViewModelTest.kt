package com.loresuelvo.consumer.ui.screens.profile

import app.cash.turbine.test
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.usecase.auth.RegisterConsumerCommand
import com.loresuelvo.consumer.domain.usecase.auth.RegisterConsumerUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [CompleteProfileViewModel]. Covers the UDF contract
 * documented in AGENTS.md:
 *
 * - Local validation: blank firstName / lastName produce typed
 *   [CompleteProfileError] without invoking the use case.
 * - Async state: `loading = true` during the call, `false` on
 *   completion; the error is reflected in `state.error`.
 * - One-shot events: `Success` emits [CompleteProfileEvent.NavigateToHome]
 *   via a [kotlinx.coroutines.channels.Channel], not a flag in the
 *   UiState.
 * - Side effects: [UserRegistrationOutcome.Failure.Unauthorized]
 *   triggers `AuthSessionStore.clearSession()` so the navigation
 *   graph can fall back to Welcome.
 * - Double-tap protection: rapid clicks do not call the use case
 *   more than once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CompleteProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase = mockk<RegisterConsumerUseCase>()
    private val sessionStore = mockk<AuthSessionStore>(relaxed = true)
    private lateinit var viewModel: CompleteProfileViewModel

    private fun sessionWith(
        firstName: String? = "Ana",
        lastName: String? = "Perez",
        email: String? = "ana@example.com",
    ): AuthSession = AuthSession(
        user = User(
            displayName = "Ana",
            firstName = firstName,
            lastName = lastName,
            email = email,
        ),
        accessToken = "test-token",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionStore.sessionFlow } returns MutableStateFlow(sessionWith())
        viewModel = CompleteProfileViewModel(useCase, sessionStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_is_prefilled_from_session() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Ana", state.firstName)
        assertEquals("Perez", state.lastName)
        assertFalse(state.loading)
        assertNull(state.error)
    }

    @Test
    fun onContinueClick_blank_firstName_sets_MissingFirstName_without_calling_use_case() = runTest {
        viewModel.onFirstNameChange("")
        viewModel.onLastNameChange("Colina")
        viewModel.onContinueClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.error is CompleteProfileError.MissingFirstName)
        assertFalse(state.loading)
        coVerify(exactly = 0) { useCase(any()) }
    }

    @Test
    fun onContinueClick_blank_lastName_sets_MissingLastName_without_calling_use_case() = runTest {
        viewModel.onFirstNameChange("Andres")
        viewModel.onLastNameChange("")
        viewModel.onContinueClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.error is CompleteProfileError.MissingLastName)
        assertFalse(state.loading)
        coVerify(exactly = 0) { useCase(any()) }
    }

    @Test
    fun onContinueClick_success_emits_NavigateToHome_and_clears_loading() = runTest {
        viewModel.onFirstNameChange("Andres")
        viewModel.onLastNameChange("Colina")
        coEvery { useCase(any()) } returns UserRegistrationOutcome.Success(
            User(
                displayName = "Andres",
                firstName = "Andres",
                lastName = "Colina",
                email = "ana@example.com",
            )
        )

        viewModel.events.test {
            viewModel.onContinueClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is CompleteProfileEvent.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertNull(state.error)
    }

    @Test
    fun onContinueClick_failure_Network_sets_error_and_clears_loading() = runTest {
        viewModel.onFirstNameChange("Andres")
        viewModel.onLastNameChange("Colina")
        coEvery { useCase(any()) } returns
            UserRegistrationOutcome.Failure.Network(IOException("dns"))

        viewModel.onContinueClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        val error = state.error
        assertTrue("error must be Network, was $error", error is CompleteProfileError.Network)
    }

    @Test
    fun onContinueClick_failure_Server_sets_error_with_code_and_message() = runTest {
        viewModel.onFirstNameChange("Andres")
        viewModel.onLastNameChange("Colina")
        coEvery { useCase(any()) } returns
            UserRegistrationOutcome.Failure.Server(code = 409, message = "Email is already registered")

        viewModel.onContinueClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        val error = state.error
        assertTrue(error is CompleteProfileError.Server)
        val server = error as CompleteProfileError.Server
        assertEquals(409, server.code)
        assertEquals("Email is already registered", server.message)
    }

    @Test
    fun onContinueClick_failure_Unauthorized_clears_session_and_sets_error() = runTest {
        viewModel.onFirstNameChange("Andres")
        viewModel.onLastNameChange("Colina")
        coEvery { useCase(any()) } returns
            UserRegistrationOutcome.Failure.Unauthorized("Token expired")

        viewModel.onContinueClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        val error = state.error
        assertTrue(error is CompleteProfileError.Unauthorized)
        assertEquals("Token expired", (error as CompleteProfileError.Unauthorized).message)
        coVerify { sessionStore.clearSession() }
    }

    @Test
    fun double_tap_on_Continue_calls_use_case_only_once() = runTest {
        viewModel.onFirstNameChange("Andres")
        viewModel.onLastNameChange("Colina")
        coEvery { useCase(any()) } coAnswers {
            // Slow use case: yield twice so concurrent taps can pile up.
            kotlinx.coroutines.yield()
            kotlinx.coroutines.yield()
            UserRegistrationOutcome.Success(
                User(
                    displayName = "Andres",
                    firstName = "Andres",
                    lastName = "Colina",
                    email = "ana@example.com",
                )
            )
        }

        viewModel.onContinueClick()
        viewModel.onContinueClick()
        viewModel.onContinueClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { useCase(any()) }
    }
}
