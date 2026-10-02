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

        viewModel = ContactProviderViewModel(
            createJobRequest = useCase,
            mediaReader = io.mockk.mockk<com.loresuelvo.consumer.platform.media.MediaReader>(relaxed = true),
            uploadJobRequestImages = io.mockk.mockk<com.loresuelvo.consumer.domain.usecase.jobrequest.UploadJobRequestImagesUseCase>(relaxed = true).also {
                io.mockk.coEvery { it.invoke(any()) } returns
                    com.loresuelvo.consumer.domain.jobrequest.UploadJobRequestImagesOutcome.Success(emptyList())
            },
        )

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
