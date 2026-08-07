package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequest
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationsUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import com.loresuelvo.consumer.domain.usecase.jobrequest.CreateJobRequestUseCase
import com.loresuelvo.consumer.domain.usecase.provider.GetProvidersByCategoryUseCase
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import com.loresuelvo.consumer.ui.professional.ProfessionalsViewModel
import com.loresuelvo.consumer.ui.screens.chat.ConversationUiState
import com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel
import com.loresuelvo.consumer.ui.screens.messages.MessagesListUiState
import com.loresuelvo.consumer.ui.screens.messages.MessagesListViewModel
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
import java.util.concurrent.atomic.AtomicReference

/**
 * Per-scenario world for the US-17 "Start a conversation with a
 * provider" BDD specs. Wires the [ProfessionalsViewModel] (for the
 * search results list) AND the [ContactProviderViewModel] (for the
 * contact form flow against the same provider) against in-memory
 * fakes so the scenarios can drive the user journey and assert on
 * the observed state.
 *
 * The world is self-contained — it does NOT share state with the
 * search-providers BDD world's `SearchProvidersCucumberWorld` or the
 * contact-provider BDD world's `ContactProviderWorld`. The shared
 * `Given` steps (login, providers, etc.) live in the search glue
 * package and operate on the search world's VM; this world drives
 * SEPARATE VM instances so the messaging BDD scenarios observe
 * their own state transitions.
 *
 * As scenarios 03-IC onwards are landed, the world will gain
 * `MessagesViewModel` (or equivalent) helpers in the same pattern.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SendMessagesWorld : AutoCloseable {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val fakeProviderRepo = FakeProviderRepository()
    private val fakeCategoryRepo = FakeCategoryRepository()
    private lateinit var viewModel: ProfessionalsViewModel

    // Contact form flow (scenario 02-IC): owns its own VM and uses
    // a fake JobRequestRepository so the `When` step can pre-load a
    // success outcome and the submit transitions to the navigation
    // event. The data layer's `CreateJobRequestUseCase` is shared
    // with production — the BDD only substitutes the repository it
    // depends on.
    private val fakeJobRequestRepo = FakeJobRequestRepository()
    private val createJobRequestUseCase = CreateJobRequestUseCase(fakeJobRequestRepo)
    private lateinit var contactProviderViewModel: ContactProviderViewModel

    // Conversations list (scenario 03-IC): owns its own VM against
    // a fake ConversationRepository. The VM auto-loads in its
    // `init { }` block (mirrors production) so the world seeds the
    // fake repo FIRST (in the `Given` step) and then re-fires the
    // load (in the `When` step) to observe the seeded conversation.
    private val fakeConversationRepo = FakeConversationRepository()
    private val getConversationsUseCase = GetConversationsUseCase(fakeConversationRepo)
    private val getConversationByIdUseCase = GetConversationByIdUseCase(fakeConversationRepo)
    private val sendMessageUseCase = SendMessageUseCase(fakeConversationRepo)
    private lateinit var messagesListViewModel: MessagesListViewModel

    // Conversation detail (scenario 05-IC): the same fake repo
    // backs both the list and the detail VM. The detail VM is
    // constructed but does NOT auto-load — the `Given` step must
    // seed the detail + call `openConversation(id)` so the
    // production-equivalent load path runs.
    private lateinit var conversationViewModel: ConversationViewModel

    private val observedUiStates = mutableListOf<ProfessionalsUiState>()
    private val observedContactUiStates = mutableListOf<ContactProviderUiState>()
    private val observedContactEvents = mutableListOf<ContactProviderEvent>()
    private val observedMessagesListStates = mutableListOf<MessagesListUiState>()
    private val observedConversationStates = mutableListOf<ConversationUiState>()

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

    private val knownCategories: Map<String, Category> = mapOf(
        "Plomería" to Category(id = 1, name = "Plomería"),
        "Electricidad" to Category(id = 2, name = "Electricidad"),
        "Gas" to Category(id = 3, name = "Gas"),
    )

    private var started = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        viewModel = ProfessionalsViewModel(
            getProviders = GetProvidersByCategoryUseCase(fakeProviderRepo),
            getCategories = GetCategoriesUseCase(fakeCategoryRepo),
        )
        contactProviderViewModel = ContactProviderViewModel(createJobRequestUseCase)
        messagesListViewModel = MessagesListViewModel(getConversationsUseCase)
        conversationViewModel = ConversationViewModel(
            getConversationById = getConversationByIdUseCase,
            sendMessage = sendMessageUseCase,
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            contactProviderViewModel.uiState.collect { observedContactUiStates += it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            contactProviderViewModel.events.collect { observedContactEvents += it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            messagesListViewModel.uiState.collect { observedMessagesListStates += it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            conversationViewModel.uiState.collect { observedConversationStates += it }
        }

        scheduler.advanceUntilIdle()
    }

    /**
     * Loads the providers that belong to [categoryName] and drives
     * the VM through [ProfessionalsViewModel.loadProviders]. The
     * state transitions to `Ready` with the matching list.
     */
    fun loadProvidersForCategory(categoryName: String) {
        val category = knownCategories[categoryName]
            ?: error("Unknown category: $categoryName (BDD has ${knownCategories.keys})")
        fakeProviderRepo.setSeed(
            knownProviders.values.filter { it.categoryId == category.id },
        )
        viewModel.loadProviders(category.id, category.name)
        scheduler.advanceUntilIdle()
    }

    fun providerNamed(fullName: String): Provider =
        knownProviders[fullName]
            ?: error("Unknown provider: $fullName (BDD has ${knownProviders.keys})")

    // ---- Contact form flow (scenario 02-IC) -------------------

    /**
     * Opens the contact form for [providerName]. Mirrors the
     * `ContactProviderWorld.openContactFor` API so the messaging
     * BDD step stays terse.
     */
    fun openContactFor(providerName: String) {
        contactProviderViewModel.onOpenContact(providerNamed(providerName))
        scheduler.advanceUntilIdle()
    }

    fun typeTitle(text: String) {
        contactProviderViewModel.onTitleChange(text)
        scheduler.advanceUntilIdle()
    }

    fun typeDescription(text: String) {
        contactProviderViewModel.onDescriptionChange(text)
        scheduler.advanceUntilIdle()
    }

    /**
     * Pre-loads the fake [JobRequestRepository] with a successful
     * outcome so the next `submitContact()` transitions to
     * `Closed` + emits `NavigateToConversation`. Mirrors the
     * `ContactProviderWorld.enqueueSuccess` API.
     */
    fun preLoadSuccess(conversationId: String = "fake-conv-1") {
        fakeJobRequestRepo.enqueueSuccess(conversationId)
    }

    fun submitContact() {
        contactProviderViewModel.onSubmit()
        scheduler.advanceUntilIdle()
    }

    fun lastUiState(): ProfessionalsUiState = observedUiStates.last()

    fun observedContactEvents(): List<ContactProviderEvent> = observedContactEvents.toList()

    // ---- Conversations list (scenario 03-IC) ---------------

    /**
     * Seeds the fake [ConversationRepository] with a single
     * conversation so the next [accessMessagesSection] call
     * observes the seeded row in the list, AND seeds the
     * detail endpoint so [openConversation] lands on a
     * populated thread. Earlier scenarios (03-IC, 04-IC) only
     * read the list state — the detail seed is a no-op for them.
     */
    fun enqueueConversation(
        conversationId: String = "1",
        counterpartName: String = "Juan",
        counterpartSurname: String = "Pérez",
        categoryName: String = "Plomería",
        status: ConversationStatus = ConversationStatus.Pending,
        lastMessageContent: String? = "Hola Juan, necesito una mano",
        lastMessageSender: ConversationSender = ConversationSender.Consumer,
        updatedOnEpochMillis: Long = 0L,
    ) {
        val counterpart = ConversationCounterpart(
            id = 20L,
            name = counterpartName,
            surname = counterpartSurname,
            categoryName = categoryName,
            profilePhotoUrl = null,
        )
        val seededMessage = lastMessageContent?.let { content ->
            ConversationMessage(
                id = "$conversationId-msg-1",
                sender = lastMessageSender,
                content = content,
                createdOnEpochMillis = updatedOnEpochMillis,
            )
        }
        fakeConversationRepo.setListSeed(
            listOf(
                Conversation(
                    id = conversationId,
                    status = status,
                    counterpart = counterpart,
                    lastMessage = seededMessage,
                    updatedOnEpochMillis = updatedOnEpochMillis,
                ),
            ),
        )
        // The detail endpoint returns the full thread. For the
        // BDD "conversation has at least one message" baseline we
        // mirror the seeded `lastMessage` into the `messages[]`
        // list; `null` ⇒ an empty thread (a brand-new conversation
        // the consumer just opened).
        fakeConversationRepo.setDetailSeed(
            ConversationDetail(
                id = conversationId,
                status = status,
                counterpart = counterpart,
                messages = listOfNotNull(seededMessage),
                updatedOnEpochMillis = updatedOnEpochMillis,
            ),
        )
    }

    /**
     * Re-fires [MessagesListViewModel.load]. The BDD's `When`
     * step ("I access the messages section") maps to this — the
     * VM's `init { load() }` already fired during
     * [startScenario] against an empty seed; the re-fetch after
     * seeding surfaces the conversation the user "already sent".
     */
    fun accessMessagesSection() {
        messagesListViewModel.load()
        scheduler.advanceUntilIdle()
    }

    fun lastMessagesListUiState(): MessagesListUiState =
        observedMessagesListStates.last()

    // ---- Conversation detail (scenario 05-IC) -------------

    /**
     * Drives [ConversationViewModel.load] for the seeded
     * conversation. Mirrors the host's `LaunchedEffect` in
     * `ConversationRoute` so the BDD exercises the same code
     * path the production UI does.
     */
    fun openConversation(conversationId: String) {
        conversationViewModel.load(conversationId)
        scheduler.advanceUntilIdle()
    }

    /** Updates the composer field. */
    fun typeMessage(text: String) {
        conversationViewModel.onPromptChange(text)
        scheduler.advanceUntilIdle()
    }

    /** Taps the send button. Advances the scheduler so the
     *  round-trip completes synchronously against the fake repo. */
    fun tapSend() {
        conversationViewModel.onSendClick()
        scheduler.advanceUntilIdle()
    }

    fun lastConversationUiState(): ConversationUiState =
        observedConversationStates.last()

    /** Snapshots the `(conversationId, content)` pairs that hit
     *  the fake repo's `sendMessage` — useful when the BDD needs
     *  to assert WHICH message was sent (not just that the state
     *  mutated). */
    fun observedSendCalls(): List<Pair<String, String>> =
        fakeConversationRepo.sendCallsSnapshot()

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    private class FakeProviderRepository : ProviderRepository {
        private val providers = mutableListOf<Provider>()

        fun setSeed(providers: List<Provider>) {
            this.providers.clear()
            this.providers.addAll(providers)
        }

        override suspend fun getProvidersByCategory(categoryId: Int): ProvidersOutcome =
            ProvidersOutcome.Success(providers.filter { it.categoryId == categoryId })
    }

    private class FakeCategoryRepository : CategoryRepository {
        override suspend fun getCategories(): CategoriesOutcome =
            CategoriesOutcome.Success(emptyList())
    }

    private class FakeJobRequestRepository : JobRequestRepository {
        private val nextOutcome = AtomicReference<CreateJobRequestOutcome?>(null)

        fun enqueueSuccess(conversationId: String = "fake-conv-1") {
            nextOutcome.set(
                CreateJobRequestOutcome.Success(
                    JobRequest(
                        id = "fake-job-1",
                        conversationId = conversationId,
                        title = "irrelevant",
                        description = "irrelevant",
                        status = "pending",
                        images = emptyList(),
                    ),
                ),
            )
        }

        override suspend fun createJobRequest(data: CreateJobRequestData): CreateJobRequestOutcome {
            val queued = nextOutcome.getAndSet(null)
            if (queued != null) return queued
            return CreateJobRequestOutcome.Success(
                JobRequest(
                    id = "fake-job-1",
                    conversationId = "fake-conv-1",
                    title = data.title,
                    description = data.description,
                    status = "pending",
                    images = emptyList(),
                ),
            )
        }
    }

    /**
     * Fake [ConversationRepository] for the send-messages BDD
     * scenarios. Holds a seeded list of conversations AND a
     * seeded single-conversation detail so both the list screen
     * and the detail screen have something to render. Records
     * every `sendMessage` call so the BDD can assert what the
     * user actually sent (and not just that the state mutated).
     *
     * Send responses are always `Success` with a fresh server-
     * issued message — the BDD scenarios for 05-IC and 06-IC
     * don't exercise typed failures at the repo level (those
     * are pinned by `ApiConversationRepositoryIntegrationTest`).
     */
    private class FakeConversationRepository : ConversationRepository {
        private var listSeed: List<Conversation> = emptyList()
        private var detailSeed: ConversationDetail? = null
        private val sendCalls = mutableListOf<Pair<String, String>>()

        fun setListSeed(conversations: List<Conversation>) {
            listSeed = conversations
        }

        fun setDetailSeed(detail: ConversationDetail) {
            detailSeed = detail
        }

        fun sendCallsSnapshot(): List<Pair<String, String>> =
            sendCalls.toList()

        override suspend fun getConversations(): ConversationsOutcome =
            ConversationsOutcome.Success(listSeed)

        override suspend fun getConversationById(
            conversationId: String,
        ): ConversationDetailOutcome {
            val detail = detailSeed
                ?: return ConversationDetailOutcome.Failure.Server(
                    code = 0,
                    message = "FakeConversationRepository: no detail seeded",
                )
            if (detail.id != conversationId) {
                return ConversationDetailOutcome.Failure.Server(
                    code = 404,
                    message = "Conversation $conversationId not found",
                )
            }
            return ConversationDetailOutcome.Success(detail)
        }

        override suspend fun sendMessage(
            conversationId: String,
            content: String,
        ): SendMessageOutcome {
            sendCalls += conversationId to content
            return SendMessageOutcome.Success(
                ConversationMessage(
                    id = "server-msg-${sendCalls.size}",
                    sender = ConversationSender.Consumer,
                    content = content,
                    createdOnEpochMillis = 1_700_000_000_000L,
                ),
            )
        }
    }
}
