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
