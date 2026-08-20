package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.data.api.WebSocketClient
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.realtime.WsEvent
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMediaMessageUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import com.loresuelvo.consumer.ui.screens.chat.ConversationUiState
import com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel
import com.loresuelvo.consumer.data.media.MediaMetadataRetrieverReader
import com.loresuelvo.consumer.data.media.AudioRecorder
import io.mockk.every
import io.mockk.mockk
import io.mockk.coEvery
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import android.net.Uri

/**
 * Per-scenario world for the US "Send photos / audio in the chat"
 * BDD specs. Self-contained: owns its OWN
 * [ConversationViewModel], its OWN [FakeConversationRepository]
 * (with media-aware responses), its OWN [FakeMediaReader], and
 * its OWN [WebSocketClient] mock.
 *
 * Decoupled from `SendMessagesWorld` on purpose: the user has
 * flagged that media scenarios will grow into video and
 * eventually general file attachments, and we want each media
 * scenario's world to evolve independently of the text-message
 * world's concerns. Sharing one world would couple two flows
 * that have nothing in common beyond the chat surface.
 *
 * For 01-MM the world exposes:
 *  - `startScenario` — sets the dispatcher, builds the VM, and
 *    starts collecting the UI state stream.
 *  - `enqueueConversation` — seeds the fake repo with a single
 *    pending conversation with the named provider counterpart.
 *  - `openConversation` — drives `ConversationViewModel.load` so
 *    the screen surfaces the seeded detail.
 *  - `chooseFromGallery(filename)` — simulates the picker
 *    returning a content URI for the named file: invokes
 *    `vm.onAttachImageFromGallery` with a deterministic fake URI.
 *  - `confirmSend` / `discardPreview` — drives the matching VM
 *    handlers.
 *  - `lastConversationUiState` — the most recent observed
 *    [ConversationUiState] (the BDD asserts against this).
 *
 * The world is `@OptIn(ExperimentalCoroutinesApi::class)` because
 * the test dispatcher is `StandardTestDispatcher` — the same
 * pattern the existing `SendMessagesWorld` uses for the text
 * path.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SendMediaWorld : AutoCloseable {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val fakeRepo = FakeConversationRepository()
    private val wsEvents: MutableSharedFlow<WsEvent> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val uriCache = mutableMapOf<String, Uri>()
    private val fakeWebSocketClient: WebSocketClient = mockk(relaxed = true) {
        every { events } returns wsEvents
        every { start() } returns Unit
    }

    private val getConversationById = GetConversationByIdUseCase(fakeRepo)
    private val sendMessage = SendMessageUseCase(fakeRepo)
    private val sendMedia = SendMediaMessageUseCase(fakeRepo)
    private val mediaReader = mockk<com.loresuelvo.consumer.data.media.MediaReader>(relaxed = true)
    private val audioRecorder = mockk<com.loresuelvo.consumer.data.media.AudioRecorder>(relaxed = true)
    private val mediaMetadataRetriever = mockk<MediaMetadataRetrieverReader>(relaxed = true)

    private lateinit var viewModel: ConversationViewModel
    private val observedConversationStates = mutableListOf<ConversationUiState>()
    private var started = false

    private val knownCounterparts: Map<String, ConversationCounterpart> = mapOf(
        "Juan Pérez" to ConversationCounterpart(
            id = 20L,
            name = "Juan",
            surname = "Pérez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        "Florencia Vega" to ConversationCounterpart(
            id = 56L,
            name = "Florencia",
            surname = "Vega",
            categoryName = "Electricidad",
            profilePhotoUrl = null,
        ),
    )

    fun startScenario() {
        if (started) return
        started = true
        Dispatchers.setMain(dispatcher)
        viewModel = ConversationViewModel(
            getConversationById = getConversationById,
            sendMessage = sendMessage,
            sendMediaMessage = sendMedia,
            mediaReader = mediaReader,
            mediaMetadataRetriever = mediaMetadataRetriever,
            audioRecorder = audioRecorder,
            webSocketClient = fakeWebSocketClient,
        )
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedConversationStates += it }
        }
        scheduler.advanceUntilIdle()
    }

    /**
     * Seeds the fake [ConversationRepository] with a single
     * pending conversation for the named counterpart. Defaults to
     * `conversationId = "1"` so the BDD step ("tengo una
     * conversación abierta con el prestador 'Juan Pérez'") can
     * stay terse. Future scenarios will pass explicit ids.
     */
    fun enqueueConversation(
        counterpartName: String,
        conversationId: String = "1",
    ) {
        val counterpart = knownCounterparts[counterpartName]
            ?: error(
                "Unknown counterpart: $counterpartName " +
                    "(BDD has ${knownCounterparts.keys})",
            )
        fakeRepo.setListSeed(
            listOf(
                Conversation(
                    id = conversationId,
                    status = ConversationStatus.Pending,
                    counterpart = counterpart,
                    lastMessage = null,
                    updatedOnEpochMillis = 0L,
                ),
            ),
        )
        fakeRepo.setDetailSeed(
            ConversationDetail(
                id = conversationId,
                status = ConversationStatus.Pending,
                counterpart = counterpart,
                messages = emptyList(),
                updatedOnEpochMillis = 0L,
            ),
        )
    }

    /**
     * Drives [ConversationViewModel.load] so the screen surfaces
     * the seeded detail in the new VM's state stream.
     */
    fun openConversation(conversationId: String = "1") {
        viewModel.load(conversationId)
        scheduler.advanceUntilIdle()
    }

    /**
     * Simulates the gallery picker returning a content URI for
     * [filename]. The world skips the picker + the launcher
     * because the BDD layer asserts data behaviour, not UI
     * rendering (the Compose acceptance test covers that).
     * Instead it stages a deterministic in-memory JPEG straight
     * through [ConversationViewModel.onAttachMedia], the
     * canonical non-Uri entry point.
     *
     * In the real UI flow the user would tap "Galería" inside
     * [MediaAttachSheet] which launches the
     * `PickVisualMedia` activity; the resulting Uri is fed to
     * [ConversationViewModel.onAttachImageFromGallery] which
     * internally calls `onAttachMedia(media, sourceUri = uri)`.
     */
    fun chooseFromGallery(filename: String = "foto-baño.jpg") {
        val media = MediaUpload.Image(
            bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
            mimeType = "image/jpeg",
            originalName = filename,
        )
        viewModel.onAttachMedia(media, sourceUri = null)
        scheduler.advanceUntilIdle()
    }

    /** Taps the preview's Send action. */
    fun confirmSend() {
        viewModel.onConfirmMediaSend()
        scheduler.advanceUntilIdle()
    }

    /** Taps the preview's Discard action. */
    fun discardPreview() {
        viewModel.onDiscardMediaPreview()
        scheduler.advanceUntilIdle()
    }

    fun lastConversationUiState(): ConversationUiState =
        observedConversationStates.last()

    /**
     * Snapshot of the `(conversationId, media)` pairs that hit
     * the fake repo's `sendMediaMessage` — useful when the BDD
     * needs to assert WHICH file was sent (not just that the
     * state mutated). Returns a triple so the assertion can
     * pin the original name + mime + byte payload.
     */
    fun observedSendMediaCalls(): List<MediaUpload.Image> =
        fakeRepo.sendMediaCallsSnapshot()

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    private fun fakeUri(value: String): Uri = 
        uriCache.getOrPut(value) { mockk(relaxed = true) }
        
    fun startAudioRecording() {
        every {
            audioRecorder.start()
        } returns Result.success(Unit)

        viewModel.onStartAudioRecording()
        scheduler.advanceUntilIdle()
    }

    fun recordAudioFor(seconds: Int) {
        require(seconds > 0)

        val audioUri = fakeUri("content://test/audio/nota-${seconds}s.webm")
        val audioBytes = ByteArray(10)

        every {
            audioRecorder.stop()
        } returns Result.success(audioUri)

        coEvery {
            mediaReader.read(audioUri)
        } returns MediaUpload.Audio(
            bytes = audioBytes,
            mimeType = "audio/mp4",
            originalName = "nota-${seconds}s.webm",
            durationMillis = seconds * 1_000L,
        )

        coEvery {
            mediaMetadataRetriever.extractDurationMillis(audioUri)
        } returns seconds * 1_000L

        viewModel.onStopAudioRecording()
        scheduler.advanceUntilIdle()
    }

    // ---- Scenario 04-MM ---------------------------------------------

    fun seedConversationWithSentImage(counterpartName: String) {
        val counterpart = knownCounterparts[counterpartName]
            ?: error("Unknown counterpart: $counterpartName")

        fakeRepo.setDetailSeed(
            ConversationDetail(
                id = "1",
                status = ConversationStatus.Pending,
                counterpart = counterpart,
                messages = listOf(
                    ConversationMessage(
                        id = "media-msg-1",
                        sender = ConversationSender.Consumer,
                        content = "",
                        createdOnEpochMillis = 1_700_000_000_000L,
                        media = MediaReference.Image(
                            id = "img-file-id",
                            url = "https://cdn.loresuelvo.test/gotera-baño.jpg",
                            mimeType = "image/jpeg",
                            originalName = "gotera-baño.jpg",
                        ),
                    ),
                ),
                updatedOnEpochMillis = 1_700_000_000_000L,
            ),
        )
    }

    fun assertSentImageBubbleIsVisible() {
        val state = lastConversationUiState()

        assertTrue(
            "expected ConversationUiState.Ready, was $state",
            state is ConversationUiState.Ready,
        )

        val ready = state as ConversationUiState.Ready

        val imageMessage = ready.detail.messages.firstOrNull {
            it.id == "media-msg-1" &&
                it.sender is ConversationSender.Consumer &&
                it.media is MediaReference.Image
        }

        assertTrue(
            "expected sent image message to be present in the conversation",
            imageMessage != null,
        )
    }

    fun assertSentImageThumbnailIsVisible() {
        val state = lastConversationUiState()

        assertTrue(
            "expected ConversationUiState.Ready, was $state",
            state is ConversationUiState.Ready,
        )

        val ready = state as ConversationUiState.Ready

        val image = ready.detail.messages
            .firstOrNull { it.id == "media-msg-1" }
            ?.media as? MediaReference.Image

        val actualImage = image
            ?: error("expected sent image message to contain an image")

        assertTrue(
            "expected sent image to have a URL",
            actualImage.url.isNotBlank(),
        )
    }

    private fun assertTrue(message: String, condition: Boolean) {
        if (!condition) throw AssertionError(message)
    }
    
    /**
     * Fake [ConversationRepository] for the media BDD. Always
     * returns Success on `sendMediaMessage` with a fresh
     * server-issued [ConversationMessage] carrying a
     * [MediaReference.Image] derived from the upload's mime +
     * name. Records every call so the BDD can assert which file
     * was sent.
     *
     * Text-path responses (`sendMessage`) deliberately fail with
     * `Server(500)` because the media scenarios don't exercise
     * them — if a future test does, it must seed a custom
     * outcome first.
     */
    private class FakeConversationRepository : ConversationRepository {
        private var listSeed: List<Conversation> = emptyList()
        private var detailSeed: ConversationDetail? = null
        private val sendMediaCalls = mutableListOf<MediaUpload.Image>()
        private val sendMediaCounter = AtomicReference(0)

        fun setListSeed(conversations: List<Conversation>) {
            listSeed = conversations
        }

        fun setDetailSeed(detail: ConversationDetail) {
            detailSeed = detail
        }

        fun sendMediaCallsSnapshot(): List<MediaUpload.Image> =
            sendMediaCalls.toList()

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
        ): SendMessageOutcome =
            SendMessageOutcome.Failure.Server(
                code = 500,
                message = "FakeConversationRepository: sendMessage not exercised",
            )

        override suspend fun sendMediaMessage(
            conversationId: String,
            media: MediaUpload,
        ): SendMessageOutcome {
            val image = media as? MediaUpload.Image
                ?: return SendMessageOutcome.Failure.Server(
                    code = 500,
                    message = "FakeConversationRepository: only Image is wired",
                )
            sendMediaCalls += image
            val nextId = sendMediaCounter.updateAndGet { it + 1 }
            return SendMessageOutcome.Success(
                ConversationMessage(
                    id = "media-msg-$nextId",
                    sender = ConversationSender.Consumer,
                    content = "",
                    createdOnEpochMillis = 1_700_000_000_000L,
                    media = MediaReference.Image(
                        id = "img-file-id",
                        url = "https://cdn.loresuelvo.test/${image.originalName}",
                        mimeType = image.mimeType,
                        originalName = image.originalName,
                    ),
                ),
            )
        }
    }
}
