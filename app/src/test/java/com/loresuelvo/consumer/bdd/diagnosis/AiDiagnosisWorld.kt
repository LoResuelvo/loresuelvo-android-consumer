package com.loresuelvo.consumer.bdd.diagnosis

import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import com.loresuelvo.consumer.domain.diagnosis.Sender
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.screens.chat.ChatUiState
import com.loresuelvo.consumer.ui.screens.chat.ChatViewModel
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
 * Per-scenario world for the AI diagnostic chat BDD spec. Owns a
 * single [StandardTestDispatcher] shared by the [ChatViewModel] and
 * the observation scope, so step defs can deterministically drive
 * and inspect the UDF state without Hilt, Compose, or a backend.
 *
 * The chat screen exercises a real round-trip against a
 * [FakeDiagnosisRepository] (no Hilt). The repo's response is
 * deterministic per scenario: scenarios that need a successful
 * server reply enqueue a [Diagnosis] through
 * [seedSuccessDiagnosis]; scenarios that need a typed failure
 * (04-DIA) enqueue a [SendDiagnosisPromptOutcome.Failure].
 *
 * Cucumber instantiates this class via its zero-arg constructor on
 * a per-scenario basis (no state leaks across scenarios).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AiDiagnosisWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val fakeRepo = FakeDiagnosisRepository()
    private lateinit var sendDiagnosisPrompt: SendDiagnosisPromptUseCase
    private lateinit var viewModel: ChatViewModel
    private val observedUiStates: MutableList<ChatUiState> = mutableListOf()

    private var started: Boolean = false

    /**
     * Last prompt typed by the user through the input bar. Held on
     * the world so the "Then veo mi mensaje en el chat" assertion
     * can read what was typed without an extra step parameter.
     */
    private var lastTypedPrompt: String = ""

    /**
     * Marker for scenario 06-DIA: when the user selects the
     * "Chat con IA" entry on Home, we record that the intent was
     * issued. The matching `Then` step then asserts the route
     * exists with the expected path. Real "the screen is visible"
     * verification is handled by
     * `src/androidTest/.../acceptance/diagnosis/ChatNavigationAcceptanceTest`.
     */
    private var chatWithAiIntentIssued: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        sendDiagnosisPrompt = SendDiagnosisPromptUseCase(fakeRepo)
        viewModel = ChatViewModel(sendDiagnosisPrompt)

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }

        scheduler.advanceUntilIdle()
    }

    // ---- 01-DIA + 02-DIA flow helpers --------------------------------

    fun typePrompt(text: String) {
        lastTypedPrompt = text
        viewModel.onPromptChange(text)
        scheduler.advanceUntilIdle()
    }

    fun tapSend() {
        viewModel.onSendClick()
        scheduler.advanceUntilIdle()
    }

    fun lastUiState(): ChatUiState = observedUiStates.last()

    fun lastTypedPromptSnapshot(): String = lastTypedPrompt

    /**
     * "se inicia una conversación con el asistente" — 01-DIA. For
     * this scenario the optimistic append alone proves the
     * conversation started; subsequent scenarios will assert on a
     * server-issued `conversationId` instead.
     */
    fun assertConversationStarted() {
        val state = lastUiState()
        if (state.messages.isEmpty()) {
            error("conversation should have started, but messages was empty")
        }
    }

    /**
     * "veo mi mensaje en el chat" — 01-DIA. Asserts the most
     * recently typed prompt is rendered as a [Sender.Consumer]
     * message.
     */
    fun assertUserMessageVisible(text: String) {
        val state = lastUiState()
        val match = state.messages.lastOrNull {
            it.sender is Sender.Consumer && it.content == text
        }
        if (match == null) {
            error(
                "expected user message with content '$text' to be visible, " +
                    "but messages=${state.messages}",
            )
        }
    }

    // ---- 03-DIA in-flight typing indicator helpers -------------------

    /**
     * Drives a complete round-trip end-to-end so the conversation
     * has at least one [com.loresuelvo.consumer.domain.diagnosis.Sender.Consumer]
     * message AND the matching assistant reply. Used by 03-DIA's
     * `Given estoy en una conversación con el asistente`.
     */
    fun driveCompletedRoundTrip(
        prompt: String = "primera",
        assistantContent: String = "OK",
    ) {
        seedSuccessDiagnosis(assistantContent = assistantContent)
        typePrompt(prompt)
        tapSend()
        // `tapSend` already calls `scheduler.advanceUntilIdle()`,
        // so the fake's success response has been consumed and
        // the VM state has settled with `sending = false` and a
        // 2-message history.
    }

    /**
     * 03-DIA `When`: simulates the user sending a new prompt
     * while the backend takes too long to reply. The fake's
     * `enqueueHangingResponse()` makes the launched coroutine
     * stay parked at `awaitCancellation()`; `state.sending`
     * therefore remains `true` after `scheduler.advanceUntilIdle()`.
     */
    fun simulateHangingSend(prompt: String = "segunda") {
        fakeRepo.enqueueHangingResponse()
        typePrompt(prompt)
        tapSend()
    }

    /**
     * 03-DIA `Then veo un indicador de carga`. The
     * [com.loresuelvo.consumer.ui.screens.chat.ChatScreen] renders
     * [com.loresuelvo.consumer.ui.screens.chat.TypingIndicatorBubble]
     * iff `state.sending == true`. We assert the state contract
     * here; the visual rendering is verified by the manual
     * smoke test on device.
     */
    fun assertTypingIndicatorVisible() {
        val state = lastUiState()
        if (!state.sending) {
            error(
                "expected state.sending=true so the typing indicator would render, " +
                    "but state=$state",
            )
        }
    }

    /**
     * 03-DIA `And no puedo enviar un nuevo mensaje hasta recibir
     * una respuesta`. The `canSend` derivation in
     * [com.loresuelvo.consumer.ui.screens.chat.ChatUiState] gates
     * on `!state.sending`, so a `true` here would mean a new send
     * could slip in and stack coroutines on the server.
     */
    fun assertSendingFlagBlocksNewSends() {
        val state = lastUiState()
        if (!state.sending) {
            error("expected state.sending=true while in flight, was $state")
        }
        if (state.canSend) {
            error(
                "expected canSend=false because state.sending=true (in-flight round-trip), " +
                    "was $state",
            )
        }
    }

    // ---- 02-DIA server-roundtrip helpers ----------------------------

    /**
     * Seeds a deterministic [SendDiagnosisPromptOutcome.Success]
     * for the next `sendPrompt(...)` call. The diagnosis includes
     * the user's typed prompt (as a consumer message) and the
     * supplied assistant content. The fake consumes the seeded
     * outcome exactly once.
     */
    fun seedSuccessDiagnosis(
        assistantContent: String,
        conversationId: String = "fake-conv",
    ) {
        val prompt = lastTypedPromptSnapshot()
        val diagnosis = Diagnosis(
            conversationId = conversationId,
            messages = listOf(
                ServerSideMessage(
                    id = "user-server-1",
                    sender = Sender.Consumer,
                    content = prompt,
                ).toChatMessage(),
                ServerSideMessage(
                    id = "assistant-1",
                    sender = Sender.Assistant,
                    content = assistantContent,
                ).toChatMessage(),
            ),
        )
        fakeRepo.enqueueOutcome(SendDiagnosisPromptOutcome.Success(diagnosis))
    }

    /**
     * 02-DIA wrapper: seeds the fake's deterministic success
     * response, types the same prompt used in 01-DIA, and taps
     * send. The state lands in `messages = [optimistic-user]` with
     * `sending = true` and the launched coroutine queued in
     * `viewModelScope`. The matching `When` step's
     * [simulateAssistantResponse] advances the scheduler so the
     * coroutine consumes the seeded outcome and the state
     * transitions to `messages = [server-user, server-assistant]`.
     */
    fun startConversationWithSeededResponse() {
        val prompt = "Tengo una gotera en el baño"
        typePrompt(prompt)
        seedSuccessDiagnosis(
            assistantContent = "Entiendo. ¿La pérdida es continua?",
        )
        tapSend()
    }

    /**
     * Advances the scheduler so the launched coroutine inside
     * `ChatViewModel.onSendClick` consumes the seeded fake outcome
     * and the resulting state update lands in the observed UI
     * states list. Mirrors "the assistant processed my message".
     */
    fun simulateAssistantResponse() {
        scheduler.advanceUntilIdle()
    }

    /**
     * "veo una respuesta del asistente en el chat" — 02-DIA.
     * Asserts that the assistant bubble is present in `state.messages`
     * after the round-trip. We don't pin the exact content because
     * the Gherkin has no `{string}` parameter; the fake's seeded
     * content is asserted in unit tests instead.
     */
    fun assertAssistantMessageVisible() {
        val state = lastUiState()
        if (state.sending) {
            error("expected assistant message, but the round-trip is still in flight")
        }
        val assistant = state.messages.lastOrNull { it.sender is Sender.Assistant }
            ?: error(
                "expected at least one assistant message after the round-trip, " +
                    "but messages=${state.messages}",
            )
        // Sanity: the server-returned messages list should also
        // include the consumer's prompt — the optimistic append is
        // replaced by the server's full history, not just the
        // assistant reply.
        if (state.messages.none { it.sender is Sender.Consumer }) {
            error(
                "expected the round-trip to surface the user prompt as a " +
                    "consumer message too, but messages=${state.messages}",
            )
        }
        if (assistant.content.isBlank()) {
            error("expected the assistant message to have non-blank content")
        }
    }

    // ---- 06-DIA helpers --------------------------------------------

    /**
     * "selecciono la opción 'Chat con IA'" — 06-DIA. Records the
     * intent so the matching `Then` step can assert the navigation
     * route exists. The actual user-flow proof (the chat screen is
     * rendered) is the responsibility of the Compose acceptance
     * test.
     */
    fun recordChatWithAiIntent() {
        chatWithAiIntentIssued = true
    }

    /**
     * "veo la pantalla de conversación con el asistente" — 06-DIA.
     * Asserts the chat route is registered with the expected path and
     * that the selection step ran first. Compose-level rendering is
     * verified by the acceptance test in
     * `src/androidTest/.../acceptance/diagnosis/`.
     */
    fun assertChatScreenRouteAvailable() {
        require(chatWithAiIntentIssued) {
            "06-DIA: 'veo la pantalla' debe ir precedido de 'selecciono la opción \"Chat con IA\"'"
        }
        val path = Route.Chat.path
        if (path != CHAT_ROUTE_PATH) {
            error("expected Route.Chat.path == '$CHAT_ROUTE_PATH', was '$path'")
        }
    }

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    private data class ServerSideMessage(
        val id: String,
        val sender: Sender,
        val content: String,
    ) {
        fun toChatMessage(): com.loresuelvo.consumer.domain.diagnosis.ChatMessage =
            com.loresuelvo.consumer.domain.diagnosis.ChatMessage(
                id = id,
                sender = sender,
                content = content,
                sentAtEpochMillis = 0L,
            )
    }

    private companion object {
        const val CHAT_ROUTE_PATH = "chat"
    }
}
