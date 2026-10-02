package com.loresuelvo.consumer.bdd.diagnosis

import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisAssessment
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.diagnosis.usecase.LoadAiConversationUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import com.loresuelvo.consumer.domain.diagnosis.Sender
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.usecase.assistant.GetAiConversationsUseCase
import com.loresuelvo.consumer.domain.usecase.jobrequest.CreateAiJobRequestUseCase
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.screens.assistant.AssistantUiState
import com.loresuelvo.consumer.ui.screens.assistant.AssistantViewModel
import com.loresuelvo.consumer.ui.screens.chat.AiDiagnosisContactEvent
import com.loresuelvo.consumer.ui.screens.chat.AiDiagnosisContactViewModel
import com.loresuelvo.consumer.ui.screens.chat.ChatUiState
import com.loresuelvo.consumer.ui.screens.chat.ChatViewModel
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.ConfirmedFile
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.PresignUploadResult
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import com.loresuelvo.consumer.domain.diagnosis.usecase.UploadAttachmentsAndSendUseCase
import com.loresuelvo.consumer.ui.screens.chat.errorLiteral
import com.loresuelvo.consumer.ui.screens.chat.ChatError
import io.mockk.coEvery
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
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AiDiagnosisWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)
    private val presignAttemptCalls = mutableListOf<PresignUploadRequest>()

    private val fakeRepo = FakeDiagnosisRepository()
    private val fakeAiJobRequestRepo = FakeAiJobRequestRepository()
    private val fakeAiConversationRepo = FakeAiConversationRepository()
    private val mediaReader = mockk<com.loresuelvo.consumer.platform.media.MediaReader>(relaxed = true)
    private val fileRepository = mockk<FileRepository>(relaxed = true)
    private lateinit var sendDiagnosisPrompt: SendDiagnosisPromptUseCase
    private lateinit var uploadAttachmentsAndSend: UploadAttachmentsAndSendUseCase
    private lateinit var createAiJobRequest: CreateAiJobRequestUseCase
    private lateinit var loadAiConversation: LoadAiConversationUseCase
    private lateinit var getConversations: GetAiConversationsUseCase
    private lateinit var viewModel: ChatViewModel
    private lateinit var aiContactViewModel: AiDiagnosisContactViewModel
    private lateinit var assistantViewModel: AssistantViewModel
    private val observedUiStates: MutableList<ChatUiState> = mutableListOf()
    private val observedAssistantStates: MutableList<AssistantUiState> = mutableListOf()
    private val observedAiContactEvents: MutableList<AiDiagnosisContactEvent> =
        mutableListOf()

    private var started: Boolean = false

    /**
     * Last prompt typed by the user through the input bar. Held on
     * the world so the "Then veo mi mensaje en el chat" assertion
     * can read what was typed without an extra step parameter.
     */
    private var lastTypedPrompt: String = ""

    private var chatWithAiIntentIssued: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        sendDiagnosisPrompt = SendDiagnosisPromptUseCase(fakeRepo)
        loadAiConversation = LoadAiConversationUseCase(fakeRepo)
        createAiJobRequest = CreateAiJobRequestUseCase(fakeAiJobRequestRepo)
        getConversations = GetAiConversationsUseCase(fakeAiConversationRepo)
        uploadAttachmentsAndSend = UploadAttachmentsAndSendUseCase(
            fileRepository = fileRepository,
            sendDiagnosisPrompt = sendDiagnosisPrompt,
        )
        viewModel = ChatViewModel(
            sendDiagnosisPrompt = sendDiagnosisPrompt,
            loadAiConversation = loadAiConversation,
            mediaReader = mediaReader,
            uploadAttachmentsAndSend = uploadAttachmentsAndSend,
        )
        aiContactViewModel = AiDiagnosisContactViewModel(createAiJobRequest)
        assistantViewModel = AssistantViewModel(getConversations)

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            aiContactViewModel.events.collect { observedAiContactEvents += it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            assistantViewModel.uiState.collect { observedAssistantStates += it }
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

    fun presignAttemptCallsSnapshot(): List<PresignUploadRequest> =
    presignAttemptCalls.toList()

    fun lastTypedPromptSnapshot(): String = lastTypedPrompt

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

    // ---- 04-DIA failure + retry helpers ---------------------------

    /**
     * 04-DIA `When`: the user types a new prompt and the backend
     * fails. The fake returns [SendDiagnosisPromptOutcome.Failure.Server]
     * on the next [sendPrompt], the VM flips
     * `transientError = ChatError.ServiceUnavailable` and the
     * optimistic bubble stays visible.
     */
    fun simulateFailingSend(prompt: String = "segunda") {
        fakeRepo.enqueueFailure(
            com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome.Failure.Server(
                code = 500,
                message = "boom",
            ),
        )
        typePrompt(prompt)
        tapSend()
    }

    fun assertAssistantMessageShows(expected: String) {
        val state = lastUiState()

        // 04-DIA path: the error card is visible.
        state.transientError?.let { error ->
            val actual = error.errorLiteral()
            if (actual == expected) return
            error(
                "expected the assistant error to read '$expected', " +
                    "got '$actual' for ${error::class.simpleName}, " +
                    "preliminaryWarningVisible=${state.preliminaryWarningVisible}",
            )
        }

        // 05-DIA path: the warning banner is visible.
        if (state.preliminaryWarningVisible) {
            // The banner text in the resource is fixed and
            // i18n is verified by the locale-aware strings.xml
            // snapshots; here we assert the flag is on so the
            // banner renders the right resource.
            if (expected == PRELIMINARY_WARNING_LITERAL) return
        }

        error(
            "expected assistant-visible message '$expected' but " +
                "transientError=${state.transientError} " +
                "and preliminaryWarningVisible=${state.preliminaryWarningVisible}",
        )
    }

    /**
     * 04-DIA `And puedo volver a intentarlo`. The retry CTA calls
     * [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel.onRetryClick],
     * which clears `transientError` and refires the round-trip
     * with `lastAttemptedPrompt`.
     */
    fun assertRetryClearsError() {
        val state = lastUiState()
        if (state.transientError != null) {
            error(
                "expected transientError to be cleared after retry, " +
                    "was ${state.transientError}",
            )
        }
        // After 04-DIA's retry hook a fresh round-trip is in
        // flight with the original prompt; state.sending is true
        // again until the fake's next outcome lands.
        if (!state.sending) {
            error("expected sending=true after retry, was $state")
        }
    }

    fun simulateRetry() {
        viewModel.onRetryClick()
        scheduler.advanceUntilIdle()
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
     * `src/androidTest/.../instrumented/diagnosis/`.
     */
    fun assertChatScreenRouteAvailable() {
        require(chatWithAiIntentIssued) {
            "06-DIA: 'veo la pantalla' debe ir precedido de 'selecciono la opción \"Chat con IA\"'"
        }

        val path = Route.Chat.buildPath()

        if (path != CHAT_ROUTE_PATH) {
            error("expected Route.Chat.buildPath() == '$CHAT_ROUTE_PATH', was '$path'")
        }
    }

    // ---- 09-DIA / 10-DIA diagnosis-concluded helpers ----------------

    /**
     * 09-DIA `Given`: drive a complete round-trip where the
     * backend returns an assessment and a list of recommended
     * providers for the supplied [categoryName]. Used by both
     * 09-DIA and 10-DIA.
     */
    fun seedConcludedDiagnosis(categoryName: String) {
        val prompt = "Tengo una gotera en el baño"
        typePrompt(prompt)
        val providers = listOf(
            Provider(
                id = 11,
                name = "Ana",
                surname = "Pérez",
                categoryId = 1,
                categoryName = categoryName,
                profilePhotoUrl = "https://example.com/ana.webp",
            ),
            Provider(
                id = 12,
                name = "Luis",
                surname = "Gómez",
                categoryId = 1,
                categoryName = categoryName,
                profilePhotoUrl = null,
            ),
        )
        val diagnosis = Diagnosis(
            conversationId = "fake-conv",
            messages = listOf(
                ServerSideMessage(
                    id = "user-server-1",
                    sender = Sender.Consumer,
                    content = prompt,
                ).toChatMessage(),
                ServerSideMessage(
                    id = "assistant-1",
                    sender = Sender.Assistant,
                    content = "Tengo una gotera en el baño",
                ).toChatMessage(),
            ),
            assessment = DiagnosisAssessment(
                outcome = DiagnosisAssessment.OUTCOME_PROFESSIONAL_REQUIRED,
                problemCategory = com.loresuelvo.consumer.domain.category.Category(
                    id = 1,
                    name = categoryName,
                ),
            ),
            recommendedProviders = providers,
        )
        fakeRepo.enqueueOutcome(SendDiagnosisPromptOutcome.Success(diagnosis))
        tapSend()
    }

    fun seedDiagnosisSuccessForAttachment() {
        val diagnosis = Diagnosis(
            conversationId = "fake-conv",
            messages = listOf(
                ServerSideMessage(
                    id = "user-server-1",
                    sender = Sender.Consumer,
                    content = lastTypedPromptSnapshot(),
                ).toChatMessage(),
                ServerSideMessage(
                    id = "assistant-1",
                    sender = Sender.Assistant,
                    content = "Detectamos una posible pérdida de agua.",
                ).toChatMessage(),
            ),
        )

        fakeRepo.enqueueOutcome(
            SendDiagnosisPromptOutcome.Success(diagnosis)
        )
    }

    fun seedFileRepositoryNetworkFailure() {
        coEvery { fileRepository.presign(any()) } answers {
            val request = firstArg<PresignUploadRequest>()
            presignAttemptCalls += request

            PresignUploadOutcome.Failure.Network(
                IOException("simulated network failure"),
            )
        }
    }

    fun assertAttachmentUploadErrorVisible() {
        val state = lastUiState()

        if (state.transientError != ChatError.Network) {
            error(
                "expected ChatError.Network after attachment upload failure, " +
                    "got ${state.transientError}",
            )
        }
    }

    fun assertDiagnosisPromptWasNotSent() {
        val ids = lastImageFileIdsSnapshot()

        if (ids.isNotEmpty()) {
            error(
                "expected diagnosis prompt not to be sent after upload failure, " +
                    "but image_file_ids=$ids",
            )
        }
    }
    
    fun assertAttachmentUploadRetried(filename: String) {
        val count = presignAttemptCallsSnapshot().count {
            it.originalName == filename
        }

        if (count < 2) {
            error(
                "expected '$filename' to be uploaded again, " +
                    "but presign was attempted $count time(s)",
            )
        }
    }

    fun assertDiagnosisPromptWasSent() {
        val ids = lastImageFileIdsSnapshot()

        if (ids.isEmpty()) {
            error(
                "expected diagnosis prompt to be sent with image_file_ids",
            )
        }
    }

    fun seedSendDiagnosisPromptServerFailure() {
        fakeRepo.enqueueOutcome(
            SendDiagnosisPromptOutcome.Failure.Server(
                code = 500,
                message = "ia service down",
            ),
        )
    }

    /**
     * Snapshot of the `image_file_ids[]` the orchestrator
     * delivered to the last `sendDiagnosisPrompt` call. Used by
     * 11-AIP to assert "se vuelve a procesar el mensaje
     * enviado" — the retry path must replay the same file IDs
     * without re-running the presign pipeline.
     */
    fun lastImageFileIdsSnapshotAfterRetry(): List<String> =
        fakeRepo.lastImageFileIdsSnapshot()

    /**
     * 11-AIP `Y no se vuelve a subir la imagen`: pin that the
     * file repository received NO additional presign calls after
     * the retry. We compare against the count snapshotted before
     * the retry fired; the orchestrator must not re-run the
     * upload pipeline when the cached file IDs are reused.
     */
    fun assertPresignCountUnchanged(initialCount: Int) {
        val actual = presignCallsSnapshot().size
        if (actual != initialCount) {
            error(
                "expected no additional presign calls after retry (was $initialCount, " +
                    "now $actual); the cached file IDs must bypass the upload pipeline",
            )
        }
    }

    fun assertAttachmentPersistsAfterFailure() {
        val state = lastUiState()
        if (state.sentAttachments.isEmpty()) {
            error(
                "expected the uploaded attachment to persist in sentAttachments " +
                    "after the prompt endpoint rejected, but sentAttachments was empty. " +
                    "state=$state",
            )
        }
    }

    fun assertTransientErrorIsServiceUnavailable() {
        val state = lastUiState()
        if (state.transientError !is ChatError.ServiceUnavailable) {
            error(
                "expected ChatError.ServiceUnavailable after IA service failure, " +
                    "got ${state.transientError}",
            )
        }
    }

    /**
     * 11-AIP `Y veo una respuesta del asistente en la
     * conversación`. Mirrors 02-DIA's assertion: after a
     * successful round-trip the assistant bubble must be
     * present in `state.messages` with non-blank content.
     */
    fun assertAssistantReplyAfterRetry() {
        val state = lastUiState()
        if (state.sending) {
            error(
                "expected the retry round-trip to settle, but state.sending was " +
                    "still true. state=$state",
            )
        }
        val assistant = state.messages.lastOrNull {
            it.sender is com.loresuelvo.consumer.domain.diagnosis.Sender.Assistant
        } ?: error(
            "expected at least one assistant message after the retry round-trip, " +
                "but messages=${state.messages}",
        )
        if (assistant.content.isBlank()) {
            error(
                "expected the assistant message to have non-blank content, " +
                    "got '${assistant.content}'",
            )
        }
    }

    fun assertAssessmentVisible() {
        val state = lastUiState()
        val assessment = state.assessment
            ?: error(
                "expected assessment to be visible after the AI concluded the diagnosis, " +
                    "but state.assessment was null. state=$state",
            )
        if (!assessment.isProfessionalRequired) {
            error(
                "expected assessment.outcome to be 'professional_required' " +
                    "after the AI concluded, but got '${assessment.outcome}'",
            )
        }
    }

    /**
     * 07-AIP `veo la explicación del problema detectado por la IA`.
     * Pins that the AI produced at least one assistant message
     * with non-blank content so the consumer has a textual
     * diagnosis to read in the chat bubble.
     */
    fun assertAssistantExplanationVisible() {
        val state = lastUiState()
        val assistant = state.messages.lastOrNull { it.sender is com.loresuelvo.consumer.domain.diagnosis.Sender.Assistant }
            ?: error(
                "expected at least one Assistant message after the diagnosis " +
                    "concluded, but state.messages=${state.messages}",
            )
        if (assistant.content.isBlank()) {
            error(
                "expected the assistant message to have non-blank content, " +
                    "got '${assistant.content}'",
            )
        }
    }

    /**
     * 07-AIP `veo la categoría detectada "Plomería"`. Pins the
     * `problemCategory.name` of the `professional_required`
     * assessment so the UI wire contract is locked.
     */
    fun assertAssessmentCategoryVisible(categoryName: String) {
        val state = lastUiState()
        val assessment = state.assessment
            ?: error(
                "expected an assessment to be visible after the diagnosis " +
                    "concluded, but state.assessment was null. state=$state",
            )
        val category = assessment.problemCategory
            ?: error(
                "expected assessment.problemCategory to be set, " +
                    "but it was null. assessment=$assessment",
            )
        if (category.name != categoryName) {
            error(
                "expected assessment.problemCategory.name to be '$categoryName', " +
                    "got '${category.name}'",
            )
        }
    }

    fun assertRecommendedProvidersVisible(categoryName: String) {
        val state = lastUiState()
        val providers = state.recommendedProviders
            ?: error(
                "expected recommended providers to be visible, " +
                    "but state.recommendedProviders was null. state=$state",
            )
        if (providers.isEmpty()) {
            error("expected at least one recommended provider, was empty")
        }
        providers.forEach { provider ->
            if (provider.categoryName != categoryName) {
                error(
                    "expected every provider to belong to '$categoryName', " +
                        "but '${provider.name} ${provider.surname}' belongs to " +
                        "'${provider.categoryName}'",
                )
            }
            val fullName = "${provider.name} ${provider.surname}".trim()
            if (fullName.isBlank()) {
                error("expected provider to have a non-blank full name, was '$fullName'")
            }
        }
    }

    // ---- 01-AIP / 02-AIP image attach helpers -----------------------

    fun chooseFromGallery(filename: String = "gotera-baño.jpg") {
        stageImageAttachment(filename)
    }

    fun chooseFromCamera(filename: String = "fuga-cocina.jpg") {
        stageImageAttachment(filename)
    }

    private fun stageImageAttachment(filename: String) {
        val media = MediaUpload.Image(
            bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
            mimeType = "image/jpeg",
            originalName = filename,
        )
        viewModel.onAttachMedia(media, sourceUri = null)
        scheduler.advanceUntilIdle()
    }

    fun seedFileRepositorySuccess() {
        coEvery { fileRepository.presign(any()) } answers {
            val request = firstArg<PresignUploadRequest>()

            presignAttemptCalls += request
            presignCalls += request

            val id = "upload-${presignCalls.size}"

            PresignUploadOutcome.Success(
                PresignUploadResult(
                    fileId = "file-${presignCalls.size}",
                    key = "key-$id",
                    uploadUrl = "https://upload.test/$id",
                    headers = mapOf("Content-Type" to "image/jpeg"),
                ),
            )
        }
        coEvery { fileRepository.uploadBytes(any(), any(), any()) } returns
            UploadBytesOutcome.Success
        coEvery { fileRepository.confirm(any(), any()) } answers {
            val fileId = firstArg<String>()
            confirmCalls += fileId
            ConfirmUploadOutcome.Success(
                ConfirmedFile(
                    id = fileId,
                    mimeType = "image/jpeg",
                    originalName = "stage-$fileId",
                    codec = "",
                    durationSeconds = 0,
                ),
            )
        }
    }

    /** Captured [PresignUploadRequest]s, in call order. */
    fun presignCallsSnapshot(): List<PresignUploadRequest> = presignCalls.toList()

    /** Captured `confirm(fileId, ...)` calls, in call order. */
    fun confirmCallsSnapshot(): List<String> = confirmCalls.toList()

    /** Snapshot of the `image_file_ids[]` the orchestrator
     *  passed to the last `sendDiagnosisPrompt` call. */
    fun lastImageFileIdsSnapshot(): List<String> =
        fakeRepo.lastImageFileIdsSnapshot()

    private val presignCalls = mutableListOf<PresignUploadRequest>()
    private val confirmCalls = mutableListOf<String>()

    fun assertPendingAttachmentStaged() {
        val state = lastUiState()
        val attachments = state.pendingAttachments
        if (attachments.isEmpty()) {
            error(
                "expected at least one pending attachment after the user picked " +
                    "an image, but pendingAttachments was empty",
            )
        }
        if (attachments.any { it.originalName.isBlank() }) {
            error(
                "expected every staged attachment to have a non-blank originalName, " +
                    "got ${attachments.map { it.originalName }}",
            )
        }
        if (state.sending) {
            error(
                "pending attachments must NOT trigger the send round-trip; " +
                    "state.sending was true. state=$state",
            )
        }
    }

    fun stageImages(filenames: List<String>) {
        filenames.forEach { stageImageAttachment(it) }
    }

    fun stageNImages(count: Int) {
        val names = (1..count).map { "imagen-$it.jpg" }
        stageImages(names)
    }

    fun removeAttachmentByFilename(filename: String) {
        val state = lastUiState()
        val index = state.pendingAttachments.indexOfFirst {
            it.originalName == filename
        }
        if (index == -1) {
            error(
                "expected to find a staged attachment named '$filename', got " +
                    "${state.pendingAttachments.map { it.originalName }}",
            )
        }
        viewModel.onRemoveAttachment(index)
        scheduler.advanceUntilIdle()
    }

    /**
     * 05-AIP: drop every staged attachment through the VM's
     * "clear all" entry point.
     */
    fun clearAllAttachments() {
        viewModel.onClearAttachments()
        scheduler.advanceUntilIdle()
    }

    /**
     * 04-AIP `Entonces tengo N imágenes pendientes de envío`:
     * the staged list has exactly [expected] elements.
     */
    fun assertPendingAttachmentCount(expected: Int) {
        val actual = lastUiState().pendingAttachments.size
        if (actual != expected) {
            error(
                "expected $expected pending attachment(s), got $actual " +
                    "(names=${lastUiState().pendingAttachments.map { it.originalName }})",
            )
        }
    }

    /**
     * 04-AIP `Y las imágenes pendientes son "x" y "y"`: the
     * staged list contains exactly [expectedFilenames] in
     * order. Pinned alongside `assertPendingAttachmentCount`
     * so a stray extra attachment surfaces cleanly.
     */
    fun assertPendingAttachmentsAre(expectedFilenames: List<String>) {
        val actual = lastUiState().pendingAttachments.map { it.originalName }
        if (actual != expectedFilenames) {
            error(
                "expected staged attachments in order ${expectedFilenames}, " +
                    "got $actual",
            )
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

    // ---- 11-DIA AI contact flow helpers ----------------------------

    /**
     * 11-DIA `When`: the user taps "Contactar" on the FIRST
     * recommended provider tile inside the carousel. The VM
     * dispatches the AI pre-filled job request via the real
     * use case against the fake repo; the fake records the call
     * for the matching `Then` assertion.
     */
    fun tapContactOnFirstRecommendedProvider() {
        val provider = lastUiState().recommendedProviders?.firstOrNull()
            ?: error(
                "11-DIA precondition failed: no recommended providers in state. " +
                    "state=${lastUiState()}",
            )
        val conversationId = lastUiState().conversationId
            ?: error(
                "11-DIA precondition failed: no conversationId in state. " +
                    "state=${lastUiState()}",
            )
        aiContactViewModel.onContactProviderClick(provider, conversationId)
        scheduler.advanceUntilIdle()
    }

    /**
     * 11-DIA `Then`: the AI pre-filled `POST /chatbot/conversations/{id}/job-requests`
     * round-trip was sent with the selected [provider]'s id and
     * the conversation id the chat state carries.
     */
    fun assertAiJobRequestInvokedFor(provider: Provider) {
        val recorded = fakeAiJobRequestRepo.lastRecordedCall()
            ?: error(
                "expected CreateAiJobRequestUseCase to be invoked with provider=" +
                    "${provider.id}, but no call was recorded on the fake repo",
            )
        if (recorded.providerId != provider.id) {
            error(
                "expected AI job-request to be sent for providerId=${provider.id}" +
                    " (${provider.name} ${provider.surname}), but the fake repo " +
                    "recorded providerId=${recorded.providerId}",
            )
        }
    }

    /**
     * Helper for the 11-DIA `Then` step: returns the first
     * recommended provider the chat state currently exposes so
     * the step can pin the wire call against the same provider
     * the `When` tapped.
     */
    fun firstRecommendedProviderSnapshot(): Provider =
        lastUiState().recommendedProviders?.firstOrNull()
            ?: error(
                "11-DIA assertion failed: no recommended providers in state. " +
                    "state=${lastUiState()}",
            )

    fun assertNavigatesToConversation() {
        val event = observedAiContactEvents.lastOrNull()
            ?: error(
                "expected AiDiagnosisContactEvent.NavigateToConversation, " +
                    "but no event was emitted. observed=$observedAiContactEvents",
            )
        if (event !is AiDiagnosisContactEvent.NavigateToConversation) {
            error(
                "expected AiDiagnosisContactEvent.NavigateToConversation, " +
                    "got ${event::class.simpleName}",
            )
        }
    }

    /**
     * Helper for the 12-DIA `Given`: seed the AI conversation
     * list AND re-trigger the Assistant VM so the world settles
     * on the matching `Ready` state. The VM auto-loads on
     * `init`, so a simple `enqueueSuccess` after that would not
     * reach the VM — the `retry()` call forces a fresh round-trip
     * that consumes the seeded list.
     */
    fun seedAiConversations(conversations: List<AiConversationSummary>) {
        fakeAiConversationRepo.enqueueSuccess(conversations)
        assistantViewModel.retry()
        scheduler.advanceUntilIdle()
    }

    private fun lastAssistantUiState(): AssistantUiState =
        observedAssistantStates.lastOrNull()
            ?: error(
                "expected the Assistant VM to have emitted at least one state, " +
                    "but observed=empty",
            )

    /**
     * 12-DIA `Then`: the Assistant VM landed on `Ready` with a
     * list of size [expectedCount]. Pins the wire contract
     * (`GET /chatbot/conversations` returned the list we seeded).
     */
    fun assertAssistantHasConversationCount(expectedCount: Int) {
        val state = lastAssistantUiState()
        if (state !is AssistantUiState.Ready) {
            error(
                "expected AssistantUiState.Ready with $expectedCount conversations, " +
                    "got ${state::class.simpleName}. state=$state",
            )
        }
        if (state.conversations.size != expectedCount) {
            error(
                "expected $expectedCount conversations in the list, " +
                    "got ${state.conversations.size}. state=$state",
            )
        }
    }

    /**
     * 12-DIA `And`: the conversation list contains a row whose
     * title matches [expectedTitle]. Used to pin the "cada sesión
     * muestra el título" assertion.
     */
    fun assertAssistantConversationTitlePresent(expectedTitle: String) {
        val state = lastAssistantUiState()
        if (state !is AssistantUiState.Ready) {
            error(
                "expected AssistantUiState.Ready, got ${state::class.simpleName}",
            )
        }
        if (state.conversations.none { it.title == expectedTitle }) {
            error(
                "expected the list to contain a conversation titled " +
                    "'$expectedTitle', got titles=" +
                    state.conversations.map { it.title },
            )
        }
    }

    /**
     * 12-DIA `And`: the conversation list contains a row whose
     * `lastMessageAtEpochMillis` is non-zero (the backend's
     * `updated_on` ISO timestamp parsed). Pinned because the
     * feature file wording is "cada sesión muestra el título y
     * la fecha del último mensaje".
     */
    fun assertAssistantConversationsHaveTimestamp() {
        val state = lastAssistantUiState()
        if (state !is AssistantUiState.Ready) {
            error(
                "expected AssistantUiState.Ready, got ${state::class.simpleName}",
            )
        }
        state.conversations.forEach { conversation ->
            if (conversation.lastMessageAtEpochMillis <= 0L) {
                error(
                    "expected conversation '${conversation.id}' to have a " +
                        "non-zero `lastMessageAtEpochMillis`, got " +
                        "${conversation.lastMessageAtEpochMillis}",
                )
            }
        }
    }

    private companion object {
        // Mirrors `app/src/main/res/values/strings.xml#chat_preliminary_warning`
        const val PRELIMINARY_WARNING_LITERAL: String =
            "Las respuestas brindadas son una orientación preliminar y no constituyen un diagnóstico técnico definitivo"

        const val CHAT_ROUTE_PATH = "chat"
    }
}
