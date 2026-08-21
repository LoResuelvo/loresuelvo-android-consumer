package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.data.api.WebSocketClient
import com.loresuelvo.consumer.data.media.AudioPlayer
import com.loresuelvo.consumer.data.media.AudioRecorder
import com.loresuelvo.consumer.data.media.MediaMetadataRetrieverReader
import com.loresuelvo.consumer.data.media.MediaReader
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelAudioPlaybackTest {

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

    private fun audioMessage(
        id: String = "audio-msg-1",
        durationMillis: Long = 10_000L,
    ) = ConversationMessage(
        id = id,
        sender = ConversationSender.Consumer,
        content = "",
        createdOnEpochMillis = 1_700_000_000_000L,
        media = MediaReference.Audio(
            id = "audio-file-id",
            url = "https://cdn.loresuelvo.test/nota-10s.webm",
            mimeType = "audio/webm",
            originalName = "nota-10s.webm",
            durationMillis = durationMillis,
        ),
    )

    private fun detailWithAudio() = ConversationDetail(
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
            audioMessage(),
        ),
        updatedOnEpochMillis = 1_700_000_000_000L,
    )

    private fun givenConversationIsLoaded() {
        coEvery {
            getConversationById("1")
        } returns ConversationDetailOutcome.Success(
            detailWithAudio(),
        )

        viewModel.load("1")
    }

    @Test
    fun onPlayAudio_starts_audio_player_for_message() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onPlayAudio("audio-msg-1")

        assertEquals(
            "https://cdn.loresuelvo.test/nota-10s.webm",
            audioPlayer.lastPlayedUrl,
        )

        assertEquals(
            0L,
            audioPlayer.lastStartPositionMillis,
        )
    }

    @Test
    fun onPlayAudio_updates_playback_state() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onPlayAudio("audio-msg-1")

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertTrue(state.audioPlayback.isPlaying)

        assertEquals(
            "audio-msg-1",
            state.audioPlayback.messageId,
        )

        assertEquals(
            0L,
            state.audioPlayback.currentPositionMillis,
        )
    }

    @Test
    fun audio_player_progress_is_reflected_in_viewmodel_state() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onPlayAudio("audio-msg-1")

        audioPlayer.advanceBy(2_000L)

        advanceUntilIdle()

        assertEquals(
            2_000L,
            audioPlayer.currentPositionMillis.value,
        )

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertEquals(
            2_000L,
            state.audioPlayback.currentPositionMillis,
        )
    }

    @Test
    fun onPlayAudio_ignores_unknown_message() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onPlayAudio("does-not-exist")

        assertEquals(
            null,
            audioPlayer.lastPlayedUrl,
        )

        assertFalse(
            audioPlayer.isPlaying.value,
        )

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertEquals(
            AudioPlaybackState(),
            state.audioPlayback,
        )
    }

    @Test
    fun onPlayAudio_ignores_non_audio_message() = runTest {
        val detail = detailWithAudio().copy(
            messages = listOf(
                ConversationMessage(
                    id = "text-msg-1",
                    sender = ConversationSender.Consumer,
                    content = "Hola Juan",
                    createdOnEpochMillis = 1_700_000_000_000L,
                    media = null,
                ),
            ),
        )

        coEvery {
            getConversationById("1")
        } returns ConversationDetailOutcome.Success(detail)

        viewModel.load("1")
        advanceUntilIdle()

        viewModel.onPlayAudio("text-msg-1")

        assertEquals(
            null,
            audioPlayer.lastPlayedUrl,
        )
    }

    @Test
    fun onPauseAudio_pauses_player_and_marks_state_as_paused() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onPlayAudio("audio-msg-1")
        advanceUntilIdle()

        assertTrue(audioPlayer.isPlaying.value)

        viewModel.onPauseAudio("audio-msg-1")
        advanceUntilIdle()

        assertFalse(audioPlayer.isPlaying.value)

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertEquals(
            "audio-msg-1",
            state.audioPlayback.messageId,
        )

        assertFalse(state.audioPlayback.isPlaying)

        assertEquals(
            0L,
            state.audioPlayback.currentPositionMillis,
        )
    }

    @Test
    fun onPauseAudio_ignores_unknown_message() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onPlayAudio("audio-msg-1")
        advanceUntilIdle()

        viewModel.onPauseAudio("does-not-exist")
        advanceUntilIdle()

        assertTrue(audioPlayer.isPlaying.value)
    }

    @Test
    fun onPauseAudio_ignores_message_that_is_not_currently_playing() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onPauseAudio("audio-msg-1")
        advanceUntilIdle()

        assertFalse(audioPlayer.isPlaying.value)

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertEquals(
            AudioPlaybackState(),
            state.audioPlayback,
        )
    }

    @Test
    fun onPlayAudio_resumes_from_paused_position() = runTest {
        givenConversationIsLoaded()
        advanceUntilIdle()

        viewModel.onPlayAudio("audio-msg-1")
        advanceUntilIdle()

        audioPlayer.advanceBy(3_000L)
        advanceUntilIdle()

        viewModel.onPauseAudio("audio-msg-1")
        advanceUntilIdle()

        viewModel.onPlayAudio("audio-msg-1")
        advanceUntilIdle()

        assertEquals(
            3_000L,
            audioPlayer.lastStartPositionMillis,
        )

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertTrue(state.audioPlayback.isPlaying)

        assertEquals(
            3_000L,
            state.audioPlayback.currentPositionMillis,
        )
    }

    @Test
    fun onPlayAudio_starts_from_zero_when_switching_to_different_audio() = runTest {
        val secondAudio = audioMessage(
            id = "audio-msg-2",
            durationMillis = 8_000L,
        )
        val detail = detailWithAudio().copy(
            messages = detailWithAudio().messages + secondAudio,
        )

        coEvery {
            getConversationById("1")
        } returns ConversationDetailOutcome.Success(detail)

        viewModel = createViewModel()
        viewModel.load("1")
        advanceUntilIdle()

        viewModel.onPlayAudio("audio-msg-1")
        advanceUntilIdle()

        audioPlayer.advanceBy(2_500L)
        advanceUntilIdle()

        viewModel.onPauseAudio("audio-msg-1")
        advanceUntilIdle()

        viewModel.onPlayAudio("audio-msg-2")
        advanceUntilIdle()

        assertEquals(
            0L,
            audioPlayer.lastStartPositionMillis,
        )

        val state =
            viewModel.uiState.value as ConversationUiState.Ready

        assertEquals(
            "audio-msg-2",
            state.audioPlayback.messageId,
        )

        assertEquals(
            0L,
            state.audioPlayback.currentPositionMillis,
        )
    }
}