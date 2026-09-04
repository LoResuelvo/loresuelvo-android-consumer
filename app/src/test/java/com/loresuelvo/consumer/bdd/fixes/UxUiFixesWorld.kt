package com.loresuelvo.consumer.bdd.fixes

import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.diagnosis.usecase.LoadAiConversationUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.UploadAttachmentsAndSendUseCase
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.ui.screens.categories.CategoriesUiState
import com.loresuelvo.consumer.ui.screens.categories.CategoriesViewModel
import com.loresuelvo.consumer.ui.screens.chat.ChatUiState
import com.loresuelvo.consumer.ui.screens.chat.ChatViewModel
import com.loresuelvo.consumer.ui.screens.home.CategoriesState
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderViewModel
import io.mockk.mockk
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
 * Per-scenario world for `features/fixes/ux_ui_fixes.feature`.
 * Owns a single [StandardTestDispatcher] so step defs can
 * inspect the UDF state of both the AI chat VM (scenario 01-UXUI)
 * and the categories VM (scenario 02-UXUI) deterministically
 * without Hilt, Compose, or a backend.
 *
 * Scenarios that don't need one of the VMs simply ignore the
 * other surface — there's no cross-coupling between them. Each
 * scenario gets its own world instance (Cucumber's default), so
 * state doesn't leak across scenarios.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UxUiFixesWorld : AutoCloseable {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    // 01-UXUI dependencies (AI diagnostic chat VM)
    private val sendDiagnosisPrompt = mockk<SendDiagnosisPromptUseCase>(relaxed = true)
    private val loadAiConversation = mockk<LoadAiConversationUseCase>(relaxed = true)
    private val mediaReader = mockk<MediaReader>(relaxed = true)
    private val uploadAttachmentsAndSend =
        mockk<UploadAttachmentsAndSendUseCase>(relaxed = true)

    private lateinit var viewModel: ChatViewModel
    private val observedUiStates = mutableListOf<ChatUiState>()

    // 03-UXUI dependencies (contact-provider VM for job requests)
    private val fakeJobRequestRepo =
        com.loresuelvo.consumer.bdd.providers.contact.FakeJobRequestRepository()
    private val createJobRequestUseCase =
        com.loresuelvo.consumer.domain.usecase.jobrequest.CreateJobRequestUseCase(
            fakeJobRequestRepo,
        )
    private lateinit var contactViewModel: ContactProviderViewModel
    private val observedContactStates =
        mutableListOf<com.loresuelvo.consumer.ui.screens.professional.ContactProviderUiState>()
    private var contactStarted = false

    // 08-UXUI dependencies (messages list VM)
    private val fakeConversationRepo = FakeConversationRepository()
    private val getConversationsUseCase =
        com.loresuelvo.consumer.domain.usecase.conversation.GetConversationsUseCase(
            fakeConversationRepo,
        )
    private lateinit var messagesListViewModel:
        com.loresuelvo.consumer.ui.screens.messages.MessagesListViewModel
    private val observedMessagesListStates =
        mutableListOf<com.loresuelvo.consumer.ui.screens.messages.MessagesListUiState>()
    private var messagesStarted = false

    // 02-UXUI dependencies (all-categories VM + fake repository)
    private val fakeCategoryRepository = FakeCategoryRepository()
    private lateinit var categoriesViewModel: CategoriesViewModel
    private val observedCategoriesStates = mutableListOf<CategoriesUiState>()
    private var categoriesStarted = false
    private var chatStarted = false

    /** Boots the AI chat VM (scenario 01-UXUI). */
    fun startScenario() {
        if (chatStarted) return
        chatStarted = true
        Dispatchers.setMain(dispatcher)
        viewModel = ChatViewModel(
            sendDiagnosisPrompt = sendDiagnosisPrompt,
            loadAiConversation = loadAiConversation,
            mediaReader = mediaReader,
            uploadAttachmentsAndSend = uploadAttachmentsAndSend,
        )
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }
        scheduler.advanceUntilIdle()
    }

    /** Boots the all-categories VM (scenario 02-UXUI). */
    fun startCategoriesScenario() {
        if (categoriesStarted) return
        categoriesStarted = true
        Dispatchers.setMain(dispatcher)
        val getCategories = GetCategoriesUseCase(fakeCategoryRepository)
        categoriesViewModel = CategoriesViewModel(getCategories)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            categoriesViewModel.uiState.collect { observedCategoriesStates += it }
        }
        scheduler.advanceUntilIdle()
    }

    /** Boots the contact-provider VM (scenario 03-UXUI). */
    fun startContactScenario() {
        if (contactStarted) return
        contactStarted = true
        Dispatchers.setMain(dispatcher)
        contactViewModel = ContactProviderViewModel(
            createJobRequest = createJobRequestUseCase,
            mediaReader = mediaReader,
            uploadJobRequestImages = io.mockk.mockk<com.loresuelvo.consumer.domain.usecase.jobrequest.UploadJobRequestImagesUseCase>(relaxed = true).also {
                io.mockk.coEvery { it.invoke(any()) } returns
                    com.loresuelvo.consumer.domain.jobrequest.UploadJobRequestImagesOutcome.Success(emptyList())
            },
        )
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            contactViewModel.uiState.collect { observedContactStates += it }
        }
        scheduler.advanceUntilIdle()
    }

    /** Open the contact sheet against [provider] (03-UXUI precondition). */
    fun openContactSheet(provider: com.loresuelvo.consumer.domain.provider.Provider) {
        startContactScenario()
        contactViewModel.onOpenContact(provider)
        scheduler.advanceUntilIdle()
    }

    /** Stage [names] as if they came from the gallery picker. */
    fun attachJobRequestImages(names: List<String>) {
        val images = names.map { name ->
            com.loresuelvo.consumer.domain.conversation.MediaUpload.Image(
                bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
                mimeType = "image/jpeg",
                originalName = name,
            )
        }
        contactViewModel.onAttachImages(images)
        scheduler.advanceUntilIdle()
    }

    /** Drop the staged image at [index] via [ContactProviderViewModel.onRemoveImage]. */
    fun removeJobRequestImage(index: Int) {
        contactViewModel.onRemoveImage(index)
        scheduler.advanceUntilIdle()
    }

    // ---- 08-UXUI helpers ----------------------------------------

    /** Boots the messages list VM (scenario 08-UXUI smoke test). */
    fun startMessagesListScenario() {
        if (messagesStarted) return
        messagesStarted = true
        Dispatchers.setMain(dispatcher)
        messagesListViewModel = com.loresuelvo.consumer.ui.screens.messages.MessagesListViewModel(
            getConversations = getConversationsUseCase,
        )
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            messagesListViewModel.uiState.collect { observedMessagesListStates += it }
        }
        scheduler.advanceUntilIdle()
    }

    /** Open the messages list (08-UXUI entry point). */
    fun openMessagesList() {
        startMessagesListScenario()
    }

    /**
     * Smoke-test assertion: the messages list VM landed on
     * [MessagesListUiState.Ready]. The dev-side visual sweep on
     * the device is the authoritative check for the actual
     * rendering (status bar / nav bar / IME insets); this BDD
     * step confirms the renderable surface exists.
     */
    fun assertMessagesListRendered() {
        val state = observedMessagesListStates.lastOrNull()
            ?: error("MessagesListViewModel emitted no state — startMessagesListScenario was never called")
        if (state !is com.loresuelvo.consumer.ui.screens.messages.MessagesListUiState.Ready) {
            error(
                "expected MessagesListUiState.Ready, got ${state::class.simpleName} — " +
                    "the bottom-nav messages list did not render its surface",
            )
        }
    }

    fun lastContactUiState(): com.loresuelvo.consumer.ui.screens.professional.ContactProviderUiState =
        observedContactStates.last()

    fun assertAttachedImageNames(expectedNames: List<String>) {
        val lastState = lastContactUiState()
        val state = lastState
            as? com.loresuelvo.consumer.ui.screens.professional.ContactProviderUiState.Open
            ?: error(
                "expected Open state with attached images, got ${lastState::class.simpleName}",
            )
        val actual = state.attachedImages.map { it.originalName }
        if (actual != expectedNames) {
            error("expected attached images $expectedNames, got $actual")
        }
    }

    fun lastUiState(): ChatUiState = observedUiStates.last()

    fun lastCategoriesUiState(): CategoriesUiState = observedCategoriesStates.last()

    /** Push a search query into the categories VM. */
    fun typeSearchQuery(query: String) {
        categoriesViewModel.onSearchQueryChange(query)
        scheduler.advanceUntilIdle()
    }

    /** Seed the fake repo with a non-default outcome (failure, custom list, etc.). */
    fun seedCategoriesOutcome(
        outcome: com.loresuelvo.consumer.domain.category.CategoriesOutcome,
    ) {
        fakeCategoryRepository.enqueue(outcome)
    }

    /** Pin the consumer-side assertion: the categories list contains every expected name. */
    fun assertAllCategoriesVisible(expectedNames: List<String>) {
        val state = lastCategoriesUiState() as CategoriesUiState.Ready
        val items = (state.categories as CategoriesState.Ready).items
        if (items.size != expectedNames.size) {
            error(
                "expected ${expectedNames.size} categories, got ${items.size} " +
                    "(names=${items.map { it.name }})",
            )
        }
        if (items.map { it.name } != expectedNames) {
            error(
                "expected categories in order $expectedNames, got ${items.map { it.name }}",
            )
        }
    }

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }
}
