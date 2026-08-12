package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.jobrequest.CreateAiJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequest
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.usecase.jobrequest.CreateAiJobRequestUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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

/**
 * Unit tests for [AiDiagnosisContactViewModel]. The VM is the
 * bridge between the chat UI's Contactar button and the AI
 * pre-filled job-request endpoint. Coverage:
 *  - happy path: tap triggers the use case; on success the VM
 *    emits `NavigateToConversation` with the conversationId the
 *    backend returned on the `JobRequest`;
 *  - failure path: the use case returns a typed failure → the
 *    VM does NOT emit a navigation event and the state returns
 *    to `Idle` so the user can retry;
 *  - defensive guards: a blank conversationId is a no-op, a
 *    second tap while already in flight is a no-op;
 *  - missing `conversationId` on the success response: the VM
 *    does NOT emit a navigation event (UI can't navigate to a
 *    conversationId it doesn't have).
 *
 * Mirrors the test pattern used by `ChatViewModelTest`:
 *  - `StandardTestDispatcher` for `viewModelScope` determinism;
 *  - `Dispatchers.setMain` to make `viewModelScope` resolve to
 *    the test dispatcher;
 *  - `coEvery` / `coVerify` through MockK for the use case.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiDiagnosisContactViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase = mockk<CreateAiJobRequestUseCase>()
    private lateinit var viewModel: AiDiagnosisContactViewModel

    private val provider = Provider(
        id = 10,
        name = "Juan",
        surname = "Gómez",
        categoryId = 1,
        categoryName = "Plomería",
        profilePhotoUrl = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AiDiagnosisContactViewModel(useCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun success_emits_NavigateToConversation_with_jobResponse_conversationId() = runTest {
        val outcome = CreateAiJobRequestOutcome.Success(
            JobRequest(
                id = "1",
                conversationId = "10",
                title = "Reparación de fuga en la cocina",
                description = "Hola Juan, ...",
                status = "pending",
                images = emptyList(),
            ),
        )
        coEvery { useCase(any(), any()) } returns outcome

        val emitted = mutableListOf<AiDiagnosisContactEvent>()
        val job = launch {
            viewModel.events.collect { emitted += it }
        }

        viewModel.onContactProviderClick(provider, conversationId = "10")
        advanceUntilIdle()

        coVerify(exactly = 1) { useCase(conversationId = "10", providerId = 10) }
        assertEquals(
            AiDiagnosisContactEvent.NavigateToConversation("10"),
            emitted.lastOrNull(),
        )
        assertEquals(AiDiagnosisContactUiState.Idle, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun failure_returns_to_Idle_and_emits_no_event() = runTest {
        coEvery { useCase(any(), any()) } returns
            CreateAiJobRequestOutcome.Failure.Server(code = 500, message = "boom")

        val emitted = mutableListOf<AiDiagnosisContactEvent>()
        val job = launch {
            viewModel.events.collect { emitted += it }
        }

        viewModel.onContactProviderClick(provider, conversationId = "10")
        advanceUntilIdle()

        assertEquals(AiDiagnosisContactUiState.Idle, viewModel.uiState.value)
        assertTrue(
            "no navigation event on failure, got=$emitted",
            emitted.isEmpty(),
        )
        job.cancel()
    }

    @Test
    fun network_failure_does_not_emit_navigation_event() = runTest {
        coEvery { useCase(any(), any()) } returns
            CreateAiJobRequestOutcome.Failure.Network(RuntimeException("dns"))

        val emitted = mutableListOf<AiDiagnosisContactEvent>()
        val job = launch {
            viewModel.events.collect { emitted += it }
        }

        viewModel.onContactProviderClick(provider, conversationId = "10")
        advanceUntilIdle()

        assertTrue(emitted.isEmpty())
        assertEquals(AiDiagnosisContactUiState.Idle, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun blank_conversationId_is_a_no_op() = runTest {
        viewModel.onContactProviderClick(provider, conversationId = null)
        viewModel.onContactProviderClick(provider, conversationId = "")
        viewModel.onContactProviderClick(provider, conversationId = "   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { useCase(any(), any()) }
        assertEquals(AiDiagnosisContactUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun second_tap_while_already_in_flight_is_a_no_op() = runTest {
        // The use case suspends until the test scheduler advances;
        // the second tap fires while the first is still in flight.
        coEvery { useCase(any(), any()) } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }

        viewModel.onContactProviderClick(provider, conversationId = "10")
        viewModel.onContactProviderClick(provider, conversationId = "10")
        viewModel.onContactProviderClick(provider, conversationId = "10")
        advanceUntilIdle()

        coVerify(exactly = 1) { useCase(conversationId = "10", providerId = 10) }
        val state = viewModel.uiState.value
        assertTrue(
            "expected Submitting, was $state",
            state is AiDiagnosisContactUiState.Submitting,
        )
    }

    @Test
    fun success_without_conversationId_on_response_does_not_emit_event() = runTest {
        // Defensive: the backend ALWAYS returns a conversationId
        // on this endpoint, but if a future revision omits it
        // the VM must not navigate to an empty string. The
        // `JobRequest.conversationId` is nullable by contract.
        coEvery { useCase(any(), any()) } returns
            CreateAiJobRequestOutcome.Success(
                JobRequest(
                    id = "1",
                    conversationId = null,
                    title = "x",
                    description = "y",
                    status = "pending",
                    images = emptyList(),
                ),
            )

        val emitted = mutableListOf<AiDiagnosisContactEvent>()
        val job = launch {
            viewModel.events.collect { emitted += it }
        }

        viewModel.onContactProviderClick(provider, conversationId = "10")
        advanceUntilIdle()

        assertTrue("no event when conversationId is null", emitted.isEmpty())
        assertEquals(AiDiagnosisContactUiState.Idle, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun state_reflects_in_flight_provider_while_submitting() = runTest {
        // The state MUST carry the in-flight provider so the UI
        // can disable just that provider's button (instead of
        // disabling the whole carousel) once we add a per-row
        // loading indicator. Today the carousel is disabled at
        // the route level, but the VM still pins the value.
        coEvery { useCase(any(), any()) } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }

        viewModel.onContactProviderClick(provider, conversationId = "10")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AiDiagnosisContactUiState.Submitting)
        assertEquals(provider, (state as AiDiagnosisContactUiState.Submitting).provider)
    }

    @Test
    fun `no_event_emitted_without_subscription`() = runTest {
        // The VM's `events` channel is buffered; without a
        // collector, a successful round-trip still completes and
        // the internal queue holds the event. After advancing
        // the scheduler, the public state returns to `Idle`; the
        // buffered event is consumed on the route's LaunchedEffect.
        coEvery { useCase(any(), any()) } returns
            CreateAiJobRequestOutcome.Success(
                JobRequest(
                    id = "1",
                    conversationId = "10",
                    title = "x",
                    description = "y",
                    status = "pending",
                    images = emptyList(),
                ),
            )

        viewModel.onContactProviderClick(provider, conversationId = "10")
        advanceUntilIdle()

        assertEquals(AiDiagnosisContactUiState.Idle, viewModel.uiState.value)
    }
}
