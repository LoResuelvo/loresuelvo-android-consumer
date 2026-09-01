package com.loresuelvo.consumer.bdd.providers.contact

import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.usecase.jobrequest.CreateJobRequestUseCase
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderEvent
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderUiState
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Per-scenario world for the contact-provider BDD specs. Mirrors
 * the conventions of `SearchProvidersCucumberWorld` and
 * `AiDiagnosisWorld`: a `StandardTestDispatcher` exposes the
 * [ContactProviderViewModel]'s state deterministically, and the
 * world retains the observed state + events so the step defs
 * can assert on the full history.
 *
 * The world also owns a [FakeJobRequestRepository] so the BDD
 * can:
 *  - enqueue the next outcome (success with a configurable
 *    `conversationId`, or a typed failure);
 *  - inspect the `CreateJobRequestData` payload the VM forwarded
 *    to the repository on the last call.
 *
 * The world is **lazily initialized** on the first VM action —
 * Cucumber JVM creates a fresh `ContactProviderSteps` instance
 * per scenario, so the `startScenario` flag is reset at the
 * start of each scenario.
 */
class ContactProviderWorld : AutoCloseable {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val fakeRepo = FakeJobRequestRepository()
    private val useCase: CreateJobRequestUseCase = CreateJobRequestUseCase(fakeRepo)

    private lateinit var viewModel: ContactProviderViewModel

    private val observedUiStates = mutableListOf<ContactProviderUiState>()
    private val observedEvents = mutableListOf<ContactProviderEvent>()

    /**
     * Hard-coded provider registry. The BDD's Background uses two
     * providers (Juan Pérez and Pedro Dib) for Plomería; we keep
     * the same shape here so the contact flow can look up the
     * provider by full name in the `When` step.
     */
    private val knownProviders: Map<String, Provider> = mapOf(
        "Juan Pérez" to Provider(
            id = 1,
            name = "Juan",
            surname = "Pérez",
            categoryId = 1,
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        "Pedro Dib" to Provider(
            id = 2,
            name = "Pedro",
            surname = "Dib",
            categoryId = 1,
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
    )

    private var started = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        viewModel = ContactProviderViewModel(useCase, io.mockk.mockk<com.loresuelvo.consumer.data.media.MediaReader>(relaxed = true))

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.events.collect { observedEvents += it }
        }

        scheduler.advanceUntilIdle()
    }

    fun providerNamed(fullName: String): Provider =
        knownProviders[fullName]
            ?: error("Unknown provider: $fullName (BDD fixture has ${knownProviders.keys})")

    fun openContactFor(providerFullName: String) {
        startScenario()
        viewModel.onOpenContact(providerNamed(providerFullName))
        scheduler.advanceUntilIdle()
    }

    fun typeTitle(text: String) {
        viewModel.onTitleChange(text)
        scheduler.advanceUntilIdle()
    }

    fun typeDescription(text: String) {
        viewModel.onDescriptionChange(text)
        scheduler.advanceUntilIdle()
    }

    /**
     * Convenience used by Scenario 02-SRP: pre-loads a successful
     * outcome so the BDD step can submit the form and observe the
     * navigation event without explicit outcome plumbing.
     */
    fun enqueueSuccess(conversationId: String = "fake-conv-1") {
        fakeRepo.enqueueSuccess(conversationId)
    }

    fun submit() {
        viewModel.onSubmit()
        scheduler.advanceUntilIdle()
    }

    fun cancel() {
        viewModel.onCancel()
        scheduler.advanceUntilIdle()
    }

    fun lastUiState(): ContactProviderUiState = observedUiStates.last()

    fun observedStates(): List<ContactProviderUiState> = observedUiStates.toList()

    fun observedEvents(): List<ContactProviderEvent> = observedEvents.toList()

    fun lastSubmittedData() = fakeRepo.lastData

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }
}
