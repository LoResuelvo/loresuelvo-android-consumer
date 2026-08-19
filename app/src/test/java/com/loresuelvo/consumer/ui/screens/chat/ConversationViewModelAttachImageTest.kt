package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import com.loresuelvo.consumer.data.api.WebSocketClient
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.realtime.WsEvent
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMediaMessageUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import com.loresuelvo.consumer.data.media.MediaMetadataRetrieverReader
import com.loresuelvo.consumer.data.media.AudioRecorder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the media attach surface of [ConversationViewModel]:
 *  - [ConversationViewModel.onAttachImageFromGallery]
 *  - [ConversationViewModel.onDiscardMediaPreview]
 *  - [ConversationViewModel.onConfirmMediaSend]
 *
 * Companion file to [ConversationViewModelTest] /
 * [ConversationViewModelSendRetryTest]; lives separately so each
 * test file stays under the team's per-commit LoC budget.
 *
 * Coverage:
 *  - attach happy path (image read into `pendingMedia`),
 *  - attach read failure surfaces a typed transient error AND
 *    clears a prior transientMediaError on a successful re-attach,
 *  - discard clears `pendingMedia` + `transientMediaError`,
 *  - confirm success appends the server-persisted bubble,
 *  - confirm failure surfaces a typed failure AND preserves
 *    `pendingMedia` for retry,
 *  - confirm without `pendingMedia` is a no-op,
 *  - attach / discard / confirm are no-ops outside `Ready`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ConversationViewModelAttachImageTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getConversationById = mockk<GetConversationByIdUseCase>()
    private val sendMessage = mockk<SendMessageUseCase>(relaxed = true)
    private val sendMediaMessage = mockk<SendMediaMessageUseCase>()
    private val mediaMetadataRetriever = mockk<MediaMetadataRetrieverReader>(relaxed = true)
    private val audioRecorder = mockk<AudioRecorder>(relaxed = true)
    private val mediaReader = mockk<MediaReader>()
    private val webSocketClient = mockk<WebSocketClient>(relaxed = true)
    private lateinit var webSocketEvents: MutableSharedFlow<WsEvent>
    private lateinit var viewModel: ConversationViewModel

    private val uri: Uri =
        Uri.parse("content://media/external/images/42")
    private val imageBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    private val imageMime = "image/jpeg"
    private val imageName = "foto-baño.jpg"
    private val serverImage = MediaReference.Image(
        url = "https://cdn.loresuelvo.test/foto-baño.jpg",
        mimeType = imageMime,
        originalName = imageName,
    )

    private fun detail(id: String = "1") = ConversationDetail(
        id = id,
        status = ConversationStatus.Pending,
        counterpart = ConversationCounterpart(
            id = 20L,
            name = "Juan",
            surname = "Gómez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        messages = emptyList(),
        updatedOnEpochMillis = 0L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        webSocketEvents = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        every { webSocketClient.events } returns webSocketEvents
        every { webSocketClient.start() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onAttachImageFromGallery_reads_uri_and_populates_pendingMedia() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        viewModel.load("1")
        advanceUntilIdle()

        coEvery {
            mediaReader.read(uri)
        } returns MediaUpload.Image(imageBytes, imageMime, imageName)

        viewModel.onAttachImageFromGallery(uri)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertFalse(state.attachingMedia)
        val pending = state.pendingMedia
        assertNotNull("pendingMedia must be populated after attach", pending)
        assertEquals(uri, pending!!.localUri)
        assertEquals(imageMime, pending.mimeType)
        assertEquals(imageName, pending.originalName)
        assertEquals(imageBytes.size.toLong(), pending.sizeBytes)
        assertTrue(pending.bytes.contentEquals(imageBytes))
        assertNull(state.transientMediaError)
    }

    @Test
    fun onAttachImageFromGallery_successful_attach_clears_prior_transientMediaError() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        viewModel.load("1")
        advanceUntilIdle()

        coEvery { mediaReader.read(uri) } throws IOException("revoked")
        viewModel.onAttachImageFromGallery(uri)
        advanceUntilIdle()
        val stateAfterFailure = viewModel.uiState.value as ConversationUiState.Ready
        assertNotNull(stateAfterFailure.transientMediaError)

        coEvery { mediaReader.read(uri) } returns MediaUpload.Image(
            byteArrayOf(0x10),
            "image/png",
            "ok.png",
        )
        viewModel.onAttachImageFromGallery(uri)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertNull(
            "successful attach must clear any prior transientMediaError",
            state.transientMediaError,
        )
        assertNotNull(state.pendingMedia)
    }

    @Test
    fun onAttachImageFromGallery_with_throwing_reader_surfaces_transient_error() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        viewModel.load("1")
        advanceUntilIdle()

        val cause = IOException("permission denied")
        coEvery { mediaReader.read(uri) } throws cause

        viewModel.onAttachImageFromGallery(uri)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertFalse(state.attachingMedia)
        assertNull(state.pendingMedia)
        val failure = state.transientMediaError
        assertTrue(
            "expected Network failure, was $failure",
            failure is SendMessageOutcome.Failure.Network,
        )
        assertSame(cause, (failure as SendMessageOutcome.Failure.Network).cause)
    }

    @Test
    fun onDiscardMediaPreview_clears_pending_and_transient_fields() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        viewModel.load("1")
        advanceUntilIdle()

        coEvery {
            mediaReader.read(uri)
        } returns MediaUpload.Image(imageBytes, imageMime, imageName)
        viewModel.onAttachImageFromGallery(uri)
        advanceUntilIdle()

        viewModel.onDiscardMediaPreview()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertNull(state.pendingMedia)
        assertFalse(state.attachingMedia)
        assertFalse(state.sendingMedia)
        assertNull(state.transientMediaError)
    }

    @Test
    fun onConfirmMediaSend_posts_via_use_case_and_appends_persisted_bubble() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        viewModel.load("1")
        advanceUntilIdle()

        coEvery {
            mediaReader.read(uri)
        } returns MediaUpload.Image(imageBytes, imageMime, imageName)
        val serverMessage = ConversationMessage(
            id = "99",
            sender = ConversationSender.Consumer,
            content = "",
            createdOnEpochMillis = 1_700_000_000_000L,
            media = serverImage,
        )
        coEvery {
            sendMediaMessage(
                conversationId = "1",
                media = match {
                    it is MediaUpload.Image &&
                        it.bytes.contentEquals(imageBytes) &&
                        it.mimeType == imageMime &&
                        it.originalName == imageName
                },
            )
        } returns SendMessageOutcome.Success(serverMessage)

        viewModel.onAttachImageFromGallery(uri)
        advanceUntilIdle()
        viewModel.onConfirmMediaSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertFalse(state.sendingMedia)
        assertNull(state.pendingMedia)
        assertNull(state.transientMediaError)
        assertEquals(1, state.detail.messages.size)
        val bubble = state.detail.messages[0]
        assertEquals("99", bubble.id)
        assertEquals(serverImage, bubble.media)
        assertEquals("", bubble.content)
    }

    @Test
    fun onConfirmMediaSend_failure_preserves_pending_for_retry() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        viewModel.load("1")
        advanceUntilIdle()

        coEvery {
            mediaReader.read(uri)
        } returns MediaUpload.Image(imageBytes, imageMime, imageName)
        viewModel.onAttachImageFromGallery(uri)
        advanceUntilIdle()

        coEvery {
            sendMediaMessage(conversationId = "1", media = any())
        } returns SendMessageOutcome.Failure.Server(413, "payload too large")

        viewModel.onConfirmMediaSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertFalse(state.sendingMedia)
        assertNotNull(
            "pendingMedia must survive a failed send for retry",
            state.pendingMedia,
        )
        val failure = state.transientMediaError
        assertTrue(
            "expected Server failure, was $failure",
            failure is SendMessageOutcome.Failure.Server,
        )
        assertEquals(413, (failure as SendMessageOutcome.Failure.Server).code)
    }

    @Test
    fun onConfirmMediaSend_with_no_pending_is_a_no_op() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        viewModel.load("1")
        advanceUntilIdle()

        viewModel.onConfirmMediaSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        assertFalse(state.sendingMedia)
        assertNull(state.pendingMedia)
        coVerify(exactly = 0) { sendMediaMessage(any(), any()) }
    }

    @Test
    fun media_actions_outside_Ready_are_no_ops() = runTest {
        // Park the load so the state stays in `Loading`.
        coEvery { getConversationById("1") } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        advanceUntilIdle()
        assertEquals(ConversationUiState.Loading, viewModel.uiState.value)

        viewModel.onAttachImageFromGallery(uri)
        viewModel.onDiscardMediaPreview()
        viewModel.onConfirmMediaSend()
        advanceUntilIdle()

        assertEquals(
            "all media actions must be no-ops while Loading",
            ConversationUiState.Loading,
            viewModel.uiState.value,
        )
    }

    // ---- Audio attach path (03-MM) -------------------------------------

    private val audioBytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)
    private val audioMime = "audio/mp4"
    private val audioName = "nota-voz.m4a"
    private val audioDuration = 5_000L
    private val audioUpload = MediaUpload.Audio(
        bytes = audioBytes,
        mimeType = audioMime,
        originalName = audioName,
        durationMillis = audioDuration,
    )

    @Test
    fun onAttachMedia_with_Audio_populates_pendingMedia_with_audio_kind_and_duration() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        viewModel.load("1")
        advanceUntilIdle()

        viewModel.onAttachMedia(audioUpload, sourceUri = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        val pending = state.pendingMedia
        assertNotNull("pendingMedia must be populated for audio", pending)
        assertEquals(PendingMediaKind.AUDIO, pending!!.kind)
        assertEquals(audioMime, pending.mimeType)
        assertEquals(audioName, pending.originalName)
        assertEquals(audioBytes.size.toLong(), pending.sizeBytes)
        assertTrue(pending.bytes.contentEquals(audioBytes))
        // 03-MM: the audio preview player seeds its scrubber
        // from `durationMillis` — pinning it here so a future
        // refactor that drops the field surfaces as a unit-test
        // failure before the player UI does.
        assertEquals(
            "durationMillis must propagate from MediaUpload.Audio to PendingMedia",
            audioDuration,
            pending.durationMillis,
        )
    }

    @Test
    fun onAttachMedia_with_Image_populates_pendingMedia_with_image_kind() = runTest {
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        viewModel.load("1")
        advanceUntilIdle()

        val imageUpload = MediaUpload.Image(
            bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte()),
            mimeType = "image/jpeg",
            originalName = "img.jpg",
        )
        viewModel.onAttachMedia(imageUpload)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        val pending = state.pendingMedia
        assertNotNull(pending)
        assertEquals(PendingMediaKind.IMAGE, pending!!.kind)
        assertEquals(0L, pending.durationMillis)
    }

    @Test
    fun onAttachMedia_audio_replaces_image_in_pendingMedia() = runTest {
        // Image first → switch to audio → pendingMedia should
        // carry the audio payload, not the previous image.
        // Pinning this so the dispatcher's else-branch can't
        // accidentally append instead of overwrite.
        coEvery { getConversationById("1") } returns
            ConversationDetailOutcome.Success(detail())
        viewModel = ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            webSocketClient,
        )
        viewModel.load("1")
        advanceUntilIdle()

        viewModel.onAttachMedia(
            MediaUpload.Image(
                bytes = byteArrayOf(0xFF.toByte()),
                mimeType = "image/jpeg",
                originalName = "first.jpg",
            ),
        )
        advanceUntilIdle()
        viewModel.onAttachMedia(audioUpload)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready
        val pending = state.pendingMedia
        assertEquals(PendingMediaKind.AUDIO, pending!!.kind)
        assertEquals(audioName, pending.originalName)
        assertEquals(audioDuration, pending.durationMillis)
    }
}

/** MockK's `any()` is auto-resolved inside `coVerify { }` /
 *  `coEvery { }` blocks via the `MockKMatcherScope` receiver.
 *  No local extension is needed; this file relies on the
 *  framework-provided matcher. */