package com.loresuelvo.consumer.ui.screens.profile

import android.net.Uri
import app.cash.turbine.test
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.data.media.MediaReader
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
    private val mediaReader = mockk<MediaReader>()
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
        viewModel = CompleteProfileViewModel(useCase, sessionStore, mediaReader)
        viewModel.onStreetChange("Calle Falsa")
        viewModel.onStreetNumberChange("123")
        viewModel.onFloorChange("1")
        viewModel.onUnitChange("A")
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
    fun selecting_unsupported_profile_photo_sets_validation_error_without_registering() = runTest {
        coEvery { mediaReader.read(any()) } returns MediaUpload.Image(
            bytes = byteArrayOf(1, 2, 3),
            mimeType = "image/gif",
            originalName = "avatar.gif",
        )

        viewModel.onProfilePhotoSelected(mockk<Uri>())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error is CompleteProfileError.ProfilePhotoUnsupportedFormat)
        assertNull(viewModel.uiState.value.profilePhoto)
        coVerify(exactly = 0) { useCase(any()) }
    }

    @Test
    fun selecting_valid_profile_photo_waits_for_crop_confirmation() = runTest {
        val photo = MediaUpload.Image(
            bytes = byteArrayOf(1, 2, 3),
            mimeType = "image/png",
            originalName = "avatar.png",
        )
        coEvery { mediaReader.read(any()) } returns photo

        viewModel.onProfilePhotoSelected(mockk<Uri>())
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.profilePhoto)
        assertEquals(photo, viewModel.uiState.value.pendingProfilePhoto)
    }

    @Test
    fun valid_profile_photo_is_forwarded_in_registration_command() = runTest {
        val photo = MediaUpload.Image(
            bytes = byteArrayOf(1, 2, 3),
            mimeType = "image/png",
            originalName = "avatar.png",
        )
        coEvery { mediaReader.read(any()) } returns photo
        coEvery { useCase(any()) } returns UserRegistrationOutcome.Success(
            sessionWith().user,
        )

        viewModel.onProfilePhotoSelected(mockk<Uri>())
        advanceUntilIdle()
        viewModel.onAction(CompleteProfileAction.ProfilePhotoCropConfirmed(photo))
        viewModel.onContinueClick()
        advanceUntilIdle()

        val command = io.mockk.slot<com.loresuelvo.consumer.domain.usecase.auth.RegisterConsumerCommand>()
        coVerify { useCase(capture(command)) }
        assertEquals(photo, command.captured.profilePhoto)
        assertNull(viewModel.uiState.value.error)
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
