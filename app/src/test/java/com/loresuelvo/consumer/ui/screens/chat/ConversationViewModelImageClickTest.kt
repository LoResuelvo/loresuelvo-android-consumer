package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.data.api.WebSocketClient
import com.loresuelvo.consumer.platform.media.AudioPlayer
import com.loresuelvo.consumer.platform.media.AudioRecorder
import com.loresuelvo.consumer.platform.media.MediaMetadataRetrieverReader
import com.loresuelvo.consumer.platform.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMediaMessageUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import com.loresuelvo.consumer.domain.realtime.WsEvent
import com.loresuelvo.consumer.testdi.FakeAudioPlayer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelImageClickTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getConversationById =
        mockk<GetConversationByIdUseCase>()

    private val sendMessage =
        mockk<SendMessageUseCase>()

    private val sendMediaMessage =
        mockk<SendMediaMessageUseCase>(relaxed = true)

    private val mediaReader =
        mockk<MediaReader>()

    private val mediaMetadataRetriever =
        mockk<MediaMetadataRetrieverReader>()

    private val audioRecorder =
        mockk<AudioRecorder>()

    private val webSocketClient =
        mockk<WebSocketClient>(relaxed = true)

    private lateinit var webSocketEvents: MutableSharedFlow<WsEvent>

    private lateinit var audioPlayer: FakeAudioPlayer

    private lateinit var viewModel: ConversationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        audioPlayer = FakeAudioPlayer()

        webSocketEvents = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

        every {
            webSocketClient.events
        } returns webSocketEvents

        every {
            webSocketClient.start()
        } returns Unit

        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ConversationViewModel {
        return ConversationViewModel(
            getConversationById,
            sendMessage,
            sendMediaMessage,
            mediaReader,
            mediaMetadataRetriever,
            audioRecorder,
            audioPlayer,
            webSocketClient,
        )
    }

    private fun receivedImageMessage(
        id: String = "image-msg-1",
    ) = ConversationMessage(
        id = id,
        sender = ConversationSender.Provider,
        content = "",
        createdOnEpochMillis = 1_700_000_000_000L,
        media = MediaReference.Image(
            id = "img-file-id",
            url = "https://cdn.loresuelvo.test/gotera-baño.jpg",
            mimeType = "image/jpeg",
            originalName = "gotera-baño.jpg",
        ),
    )

    private fun detailWithImage() = ConversationDetail(
        id = "1",
        status = ConversationStatus.Pending,
        counterpart = ConversationCounterpart(
            id = 20L,
            name = "Juan",
            surname = "Pérez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        messages = listOf(
            receivedImageMessage(),
        ),
        updatedOnEpochMillis = 1_700_000_000_000L,
    )

    private fun receivedVideoMessage(
        id: String = "video-msg-1",
    ) = ConversationMessage(
        id = id,
        sender = ConversationSender.Provider,
        content = "Mirá la pérdida",
        createdOnEpochMillis = 1_700_000_000_000L,
        media = MediaReference.Video(
            id = "video-file-id",
            url = "https://cdn.loresuelvo.test/evidence.mp4",
            mimeType = "video/mp4",
            originalName = "evidence.mp4",
            durationMillis = 20_000L,
            width = 1280,
            height = 720,
            videoCodec = "h264",
            audioCodec = "aac",
        ),
    )

    private fun detailWithVideo() = detailWithImage().copy(
        messages = listOf(receivedVideoMessage()),
    )

    private fun givenConversationIsLoaded() {
        coEvery {
            getConversationById("1")
        } returns ConversationDetailOutcome.Success(
            detailWithImage(),
        )

        viewModel.load("1")
    }

    @Test
    fun onImageClick_sets_fullscreen_image_in_state() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onImageClick("image-msg-1")
        advanceUntilIdle()

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertNotNull(
            "expected fullscreenImage to be set after tapping the image bubble",
            state.fullscreenImage,
        )

        assertEquals(
            "gotera-baño.jpg",
            state.fullscreenImage?.originalName,
        )
    }

    @Test
    fun onFullscreenImageDismiss_clears_fullscreen_image() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onImageClick("image-msg-1")
        advanceUntilIdle()

        viewModel.onFullscreenImageDismiss()
        advanceUntilIdle()

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertNull(state.fullscreenImage)
    }

    @Test
    fun onImageClick_ignores_unknown_message() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onImageClick("does-not-exist")
        advanceUntilIdle()

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertNull(state.fullscreenImage)
    }

    @Test
    fun onImageClick_ignores_non_image_message() = runTest {
        val detail = detailWithImage().copy(
            messages = listOf(
                ConversationMessage(
                    id = "text-msg-1",
                    sender = ConversationSender.Provider,
                    content = "Hola",
                    createdOnEpochMillis = 1_700_000_000_000L,
                    media = null,
                ),
            ),
        )

        coEvery {
            getConversationById("1")
        } returns ConversationDetailOutcome.Success(detail)

        viewModel = createViewModel()
        viewModel.load("1")
        advanceUntilIdle()

        viewModel.onImageClick("text-msg-1")
        advanceUntilIdle()

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertNull(state.fullscreenImage)
    }

    @Test
    fun onVideoClick_sets_fullscreen_video_in_state() = runTest {
        coEvery {
            getConversationById("1")
        } returns ConversationDetailOutcome.Success(detailWithVideo())

        viewModel.load("1")
        advanceUntilIdle()

        viewModel.onVideoClick("video-msg-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready

        assertNotNull(
            "expected fullscreenVideo to be set after tapping the video bubble",
            state.fullscreenVideo,
        )
        assertEquals("evidence.mp4", state.fullscreenVideo?.originalName)
    }

    @Test
    fun onFullscreenVideoDismiss_clears_fullscreen_video() = runTest {
        coEvery {
            getConversationById("1")
        } returns ConversationDetailOutcome.Success(detailWithVideo())

        viewModel.load("1")
        advanceUntilIdle()

        viewModel.onVideoClick("video-msg-1")
        viewModel.onFullscreenVideoDismiss()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ConversationUiState.Ready

        assertNull(state.fullscreenVideo)
    }
}
