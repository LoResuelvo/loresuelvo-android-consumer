package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.assistant.AiConversationRepository
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.assistant.GetAiConversationsUseCase
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAcceptedServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import com.loresuelvo.consumer.ui.screens.assistant.AssistantUiState
import com.loresuelvo.consumer.ui.screens.assistant.AssistantViewModel
import com.loresuelvo.consumer.ui.screens.home.AiConversationsState
import com.loresuelvo.consumer.ui.screens.home.CategoriesState
import com.loresuelvo.consumer.ui.screens.home.HomeUiState
import com.loresuelvo.consumer.ui.screens.home.HomeViewModel
import com.loresuelvo.consumer.ui.screens.home.ServiceProposalsState
import com.loresuelvo.consumer.ui.screens.home.TurnosState
import com.loresuelvo.consumer.ui.screens.messages.MessagesListUiState
import com.loresuelvo.consumer.ui.screens.messages.MessagesListViewModel
import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class HomeAndMessagesRefreshWorld : AutoCloseable {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    private lateinit var categoriesRepository: MutableCategoriesRepository
    private lateinit var serviceProposalsRepository: MutableServiceProposalsRepository
    private lateinit var turnosRepository: MutableTurnosRepository
    private lateinit var aiRepository: ControlledAiConversationRepository
    private lateinit var conversationsRepository: ControlledConversationRepository

    lateinit var homeViewModel: HomeViewModel
        private set
    lateinit var messagesViewModel: MessagesListViewModel
        private set

    var currentRoute: String = ""
        private set
    var routeHistory: List<String> = emptyList()
        private set

    fun startAuthenticatedSession() {
        Dispatchers.setMain(dispatcher)
        categoriesRepository = MutableCategoriesRepository(
            CategoriesOutcome.Success(listOf(Category(1, "Plomería"))),
        )
        serviceProposalsRepository = MutableServiceProposalsRepository(
            ServiceProposalsOutcome.Success(emptyList()),
        )
        turnosRepository = MutableTurnosRepository(TurnosOutcome.Success(emptyList()))
        aiRepository = ControlledAiConversationRepository(
            AiConversationListOutcome.Success(emptyList()),
        )
        conversationsRepository = ControlledConversationRepository(
            ConversationsOutcome.Success(emptyList()),
        )
        homeViewModel = createHomeViewModel()
        settle()
    }

    fun setAiConversations(conversations: List<AiConversationSummary>) {
        aiRepository.nextOutcome = AiConversationListOutcome.Success(conversations)
    }

    fun setAiFailure(failure: AiConversationListOutcome.Failure) {
        aiRepository.nextOutcome = failure
    }

    fun refreshRecentAiConversations() {
        homeViewModel.loadRecentAiConversations()
        settle()
    }

    fun recentAiConversations(): List<AiConversationSummary> =
        ((homeViewModel.uiState.value as HomeUiState.Ready)
            .recentAiConversations as AiConversationsState.Ready).items

    fun openAssistantFromHome() {
        currentRoute = "Asistente IA"
        routeHistory = routeHistory + currentRoute
    }

    fun assistantSessions(): List<AiConversationSummary> {
        val assistant = AssistantViewModel(GetAiConversationsUseCase(aiRepository))
        settle()
        return (assistant.uiState.value as AssistantUiState.Ready).conversations
    }

    fun startNewDiagnosis() {
        currentRoute = "Conversación IA"
        routeHistory = routeHistory + currentRoute
    }

    fun setTurnos(outcome: TurnosOutcome) {
        turnosRepository.nextOutcome = outcome
    }

    fun setServiceProposals(outcome: ServiceProposalsOutcome) {
        serviceProposalsRepository.nextOutcome = outcome
    }

    fun refreshHome() {
        homeViewModel.refresh()
        settle()
    }

    fun refreshTurnos() {
        homeViewModel.loadTurnos()
        settle()
    }

    fun enqueueAiResponses(
        oldResponse: AiConversationListOutcome,
        latestResponse: AiConversationListOutcome,
    ): Pair<CompletableDeferred<AiConversationListOutcome>, CompletableDeferred<AiConversationListOutcome>> {
        val old = CompletableDeferred<AiConversationListOutcome>()
        val latest = CompletableDeferred<AiConversationListOutcome>()
        aiRepository.enqueue(old)
        aiRepository.enqueue(latest)
        homeViewModel.loadRecentAiConversations()
        homeViewModel.loadRecentAiConversations()
        latest.complete(latestResponse)
        old.complete(oldResponse)
        settle()
        return old to latest
    }

    fun homeState(): HomeUiState = homeViewModel.uiState.value

    fun setConversations(conversations: List<Conversation>) {
        conversationsRepository.nextOutcome = ConversationsOutcome.Success(conversations)
    }

    fun openMessages() {
        messagesViewModel = MessagesListViewModel(
            GetConversationsUseCase(conversationsRepository),
        )
        settle()
        currentRoute = "Mensajes"
        routeHistory = routeHistory + currentRoute
    }

    fun filterMessages(query: String) {
        messagesViewModel.onSearchQueryChange(query)
    }

    fun messageState(): MessagesListUiState = messagesViewModel.uiState.value

    fun reloadMessages() {
        messagesViewModel.load()
        settle()
    }

    fun beginLoadingMessages(): CompletableDeferred<ConversationsOutcome> {
        val response = CompletableDeferred<ConversationsOutcome>()
        conversationsRepository.enqueue(response)
        messagesViewModel = MessagesListViewModel(
            GetConversationsUseCase(conversationsRepository),
        )
        scheduler.runCurrent()
        return response
    }

    fun exerciseMessageStatesAndNavigation() {
        val loadingResponse = beginLoadingMessages()
        check(messageState() is MessagesListUiState.Loading)
        loadingResponse.complete(ConversationsOutcome.Success(emptyList()))
        settle()
        check(messageState() is MessagesListUiState.Ready)

        conversationsRepository.nextOutcome = ConversationsOutcome.Failure.Server(503, "unavailable")
        reloadMessages()
        check(messageState() is MessagesListUiState.Error)

        conversationsRepository.nextOutcome = ConversationsOutcome.Success(
            listOf(conversation(id = "recovered", providerName = "Juan")),
        )
        reloadMessages()
        check(messageState() is MessagesListUiState.Ready)

        currentRoute = "Home"
        routeHistory = routeHistory + currentRoute
        currentRoute = "Mensajes"
        routeHistory = routeHistory + currentRoute
        currentRoute = "Conversación"
        routeHistory = routeHistory + currentRoute
        currentRoute = "Mensajes"
        routeHistory = routeHistory + currentRoute
    }

    fun expectedDividerCount(): Int {
        val state = messageState() as MessagesListUiState.Ready
        return (state.conversations.size - 1).coerceAtLeast(0)
    }

    override fun close() {
        Dispatchers.resetMain()
    }

    private fun createHomeViewModel() = HomeViewModel(
        getCategories = GetCategoriesUseCase(categoriesRepository),
        getPendingServiceProposals = GetPendingServiceProposalsUseCase(serviceProposalsRepository),
        getAcceptedServiceProposals = GetAcceptedServiceProposalsUseCase(serviceProposalsRepository),
        getTurnos = GetTurnosUseCase(turnosRepository),
        getAiConversations = GetAiConversationsUseCase(aiRepository),
    )

    private fun settle() {
        scheduler.advanceUntilIdle()
    }

    fun aiConversation(
        id: String,
        title: String,
        updatedOn: Long,
        preview: String,
    ) = AiConversationSummary(id, title, updatedOn, preview)

    fun conversation(
        id: String,
        providerName: String,
        providerSurname: String = "Gómez",
        preview: String = "Necesito ayuda",
        updatedOn: Long = 1_700_000_000_000L,
    ) = Conversation(
        id = id,
        status = ConversationStatus.Other("accepted"),
        counterpart = ConversationCounterpart(
            id = id.hashCode().toLong(),
            name = providerName,
            surname = providerSurname,
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        lastMessage = ConversationMessage(
            id = "$id-message",
            sender = ConversationSender.Provider,
            content = preview,
            createdOnEpochMillis = updatedOn,
        ),
        updatedOnEpochMillis = updatedOn,
    )

    private class MutableCategoriesRepository(
        var nextOutcome: CategoriesOutcome,
    ) : CategoryRepository {
        override suspend fun getCategories(): CategoriesOutcome = nextOutcome
    }

    private class MutableServiceProposalsRepository(
        var nextOutcome: ServiceProposalsOutcome,
    ) : ServiceProposalRepository {
        override suspend fun getServiceProposals(): ServiceProposalsOutcome = nextOutcome
    }

    private class MutableTurnosRepository(
        var nextOutcome: TurnosOutcome,
    ) : TurnosRepository {
        override suspend fun getTurnos(): TurnosOutcome = nextOutcome
    }

    private class ControlledAiConversationRepository(
        var nextOutcome: AiConversationListOutcome,
    ) : AiConversationRepository {
        private val deferredResponses = ArrayDeque<CompletableDeferred<AiConversationListOutcome>>()

        fun enqueue(response: CompletableDeferred<AiConversationListOutcome>) {
            deferredResponses.addLast(response)
        }

        override suspend fun getConversations(): AiConversationListOutcome =
            if (deferredResponses.isEmpty()) nextOutcome else deferredResponses.removeFirst().await()
    }

    private class ControlledConversationRepository(
        var nextOutcome: ConversationsOutcome,
    ) : ConversationRepository {
        private val deferredResponses = ArrayDeque<CompletableDeferred<ConversationsOutcome>>()

        fun enqueue(response: CompletableDeferred<ConversationsOutcome>) {
            deferredResponses.addLast(response)
        }

        override suspend fun getConversations(): ConversationsOutcome =
            if (deferredResponses.isEmpty()) nextOutcome else deferredResponses.removeFirst().await()

        override suspend fun getConversationById(
            conversationId: String,
        ): ConversationDetailOutcome = throw UnsupportedOperationException()

        override suspend fun sendMessage(
            conversationId: String,
            content: String,
        ): SendMessageOutcome = throw UnsupportedOperationException()

        override suspend fun sendMediaMessage(
            conversationId: String,
            media: List<MediaUpload>,
        ): SendMessageOutcome = throw UnsupportedOperationException()
    }
}
