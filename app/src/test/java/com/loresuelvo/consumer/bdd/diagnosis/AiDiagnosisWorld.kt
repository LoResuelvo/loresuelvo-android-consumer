package com.loresuelvo.consumer.bdd.diagnosis

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
 * The chat screen is identity-continuous: there is no separate
 * `init { }` block that fires a network request, so the VM is
 * constructed synchronously. The network round-trip lands in commit
 * 02-DIA; this world will then host a `FakeDiagnosisRepository`.
 *
 * Cucumber instantiates this class via its zero-arg constructor on a
 * per-scenario basis (no state leaks across scenarios).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AiDiagnosisWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

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

        viewModel = ChatViewModel()

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }

        scheduler.advanceUntilIdle()
    }

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

    /**
     * "selecciono la opción 'Chat con IA'" — 06-DIA. Records the
     * intent so the matching `Then` step can assert the navigation
     * route exists. The actual user-flow proof (the chat screen is
     * rendered) is the responsibility of the Compose acceptance test.
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

    private companion object {
        const val CHAT_ROUTE_PATH = "chat"
    }
}
