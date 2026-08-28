package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.diagnosis.usecase.LoadAiConversationUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Sender
import java.io.IOException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests pinning the image-attach surface of [ChatViewModel]
 * for the AI pre-diagnosis flow (scenario 01-AIP). Companion to
 * [ChatViewModelTest]; lives separately so each test class
 * stays under the team's per-commit LoC budget.
 *
 * Coverage of 01-AIP:
 *  - initial state has empty `pendingAttachments`,
 *  - `onAttachImageFromGallery` reads the [Uri] via [MediaReader]
 *    and appends a [PendingMedia] of kind [PendingMediaKind.IMAGE]
 *    with the read bytes + name + mime,
 *  - `onAttachMedia` (the canonical non-`Uri` entry point used by
 *    the BDD world) appends without invoking [MediaReader] and
 *    does NOT fire the send round-trip.
 *
 * The [Uri] is built via MockK to keep the test on the plain JUnit
 * runner (no Robolectric boot) — the production code only hands the
 * [Uri] to the mocked [MediaReader], so the Android surface area
 * stays bounded to the test fixture.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelAttachImageTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase = mockk<SendDiagnosisPromptUseCase>(relaxed = true)
    private val loadUseCase = mockk<LoadAiConversationUseCase>(relaxed = true)
    private val mediaReader = mockk<MediaReader>()
    private val uploadAttachmentsAndSend = mockk<com.loresuelvo.consumer.domain.diagnosis.usecase.UploadAttachmentsAndSendUseCase>(relaxed = true)
    private lateinit var viewModel: ChatViewModel

    private val galleryUri: Uri = mockk<Uri>(relaxed = true).also {
        every { it.toString() } returns "content://media/external/images/42"
    }
    private val imageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val imageName = "gotera-baño.jpg"
    private val imageMime = "image/jpeg"

    private fun attachment(name: String) = com.loresuelvo.consumer.ui.screens.chat.PendingMedia(
        localUri = null,
        mimeType = "image/jpeg",
        originalName = name,
        sizeBytes = imageBytes.size.toLong(),
        bytes = imageBytes,
        kind = com.loresuelvo.consumer.ui.screens.chat.PendingMediaKind.IMAGE,
        durationMillis = 0L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChatViewModel(useCase, loadUseCase, mediaReader, uploadAttachmentsAndSend)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_pendingAttachments_is_empty_list() = runTest {
        advanceUntilIdle()
        assertEquals(emptyList<PendingMedia>(), viewModel.uiState.value.pendingAttachments)
    }

    @Test
    fun canSend_is_true_when_only_attachments_staged_no_prompt() = runTest {
        // WSP-style send: stage an image without typing a prompt.
        viewModel.onAttachMedia(
            MediaUpload.Image(imageBytes, imageMime, imageName),
            sourceUri = null,
        )
        advanceUntilIdle()
        assertTrue(
            "canSend must be true with staged attachments even when the " +
                "prompt input is empty",
            viewModel.uiState.value.canSend,
        )
    }

    @Test
    fun canSend_is_false_with_no_prompt_and_no_attachments() = runTest {
        advanceUntilIdle()
        assertFalse(
            "canSend must remain false with nothing to send",
            viewModel.uiState.value.canSend,
        )
    }

    @Test
    fun onSendClick_with_attachments_only_sends_image_only_round_trip() = runTest {
        // WSP-style send path: prompt empty + attachment present ⇒
        // the orchestrator must still be invoked with the
        // attachment and a blank prompt.
        val serverMessage = ChatMessage(
            id = "srv-1",
            sender = Sender.Assistant,
            content = "Diagnóstico IA",
            sentAtEpochMillis = 1_700_000_000_000L,
        )
        val serverDiagnosis = Diagnosis(
            conversationId = "conv-1",
            messages = emptyList(),
        )
        coEvery {
            uploadAttachmentsAndSend(
                prompt = any(),
                conversationId = any(),
                attachments = any(),
            )
        } returns SendDiagnosisPromptOutcome.Success(serverDiagnosis)

        viewModel.onAttachMedia(
            MediaUpload.Image(imageBytes, imageMime, imageName),
            sourceUri = null,
        )
        advanceUntilIdle()
        viewModel.onSendClick()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            uploadAttachmentsAndSend(
                prompt = "",
                conversationId = any(),
                attachments = match { list -> list.single().originalName == imageName },
            )
        }
        val state = viewModel.uiState.value
        assertFalse(state.sending)
        assertTrue(state.pendingAttachments.isEmpty())
    }

    @Test
    fun onAttachImageFromGallery_with_valid_uri_appends_image_to_pendingAttachments() = runTest {
        coEvery { mediaReader.read(galleryUri) } returns
            MediaUpload.Image(imageBytes, imageMime, imageName)

        viewModel.onAttachImageFromGallery(galleryUri)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.pendingAttachments.size)
        val pending = state.pendingAttachments.first()
        assertEquals(PendingMediaKind.IMAGE, pending.kind)
        assertEquals(imageName, pending.originalName)
        assertEquals(imageMime, pending.mimeType)
        assertTrue(pending.bytes.contentEquals(imageBytes))
        // The attach path is strictly local; the send round-trip
        // only fires when the user taps "Diagnosticar" (06-AIP).
        assertFalse(state.sending)
        assertNull(state.transientError)
        coVerify(exactly = 1) { mediaReader.read(galleryUri) }
        coVerify(exactly = 0) { useCase(any(), any()) }
    }

    @Test
    fun onAttachImageFromCamera_with_valid_uri_appends_image_to_pendingAttachments() = runTest {
        // Mirrors the gallery test but goes through the camera
        // entry point (02-AIP). The VM collapses both URIs into
        // the same read pipeline; what changes is the route's
        // launcher wiring.
        val cameraUri: Uri = mockk<Uri>(relaxed = true).also {
            every { it.toString() } returns "content://cache/camera/42"
        }
        val cameraBytes = byteArrayOf(0xCA.toByte(), 0xFE.toByte())
        val cameraName = "fuga-cocina.jpg"
        coEvery { mediaReader.read(cameraUri) } returns
            MediaUpload.Image(cameraBytes, imageMime, cameraName)

        viewModel.onAttachImageFromCamera(cameraUri)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.pendingAttachments.size)
        val pending = state.pendingAttachments.first()
        assertEquals(cameraName, pending.originalName)
        assertTrue(pending.bytes.contentEquals(cameraBytes))
        assertFalse(state.sending)
        coVerify(exactly = 1) { mediaReader.read(cameraUri) }
        coVerify(exactly = 0) { useCase(any(), any()) }
    }

    @Test
    fun onAttachMedia_with_Image_appends_to_pendingAttachments_without_calling_mediaReader() =
        runTest {
            viewModel.onAttachMedia(
                MediaUpload.Image(imageBytes, imageMime, imageName),
                sourceUri = null,
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.pendingAttachments.size)
            assertEquals(imageName, state.pendingAttachments.first().originalName)
            // Canonical non-Uri entry point bypasses the picker
            // entirely; MediaReader must NOT be touched.
            coVerify(exactly = 0) { mediaReader.read(any()) }
            coVerify(exactly = 0) { useCase(any(), any()) }
        }

    @Test
    fun onRemoveAttachment_filters_attachment_by_index() = runTest {
        // Stage two images so the test exercises an out-of-order
        // removal (the second, not the first).
        viewModel.onAttachMedia(
            MediaUpload.Image(imageBytes, imageMime, "first.jpg"),
            sourceUri = null,
        )
        viewModel.onAttachMedia(
            MediaUpload.Image(imageBytes, imageMime, "second.jpg"),
            sourceUri = null,
        )
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.pendingAttachments.size)

        viewModel.onRemoveAttachment(index = 1)
        advanceUntilIdle()

        val remaining = viewModel.uiState.value.pendingAttachments
        assertEquals(1, remaining.size)
        assertEquals("first.jpg", remaining.single().originalName)
    }

    @Test
    fun onRemoveAttachment_with_out_of_bounds_index_is_a_no_op() = runTest {
        viewModel.onAttachMedia(
            MediaUpload.Image(imageBytes, imageMime, imageName),
            sourceUri = null,
        )
        advanceUntilIdle()

        // Stale recompositions must not crash the chat.
        viewModel.onRemoveAttachment(index = 5)
        viewModel.onRemoveAttachment(index = -1)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.pendingAttachments.size)
    }

    @Test
    fun onClearAttachments_empties_the_pending_list() = runTest {
        viewModel.onAttachMedia(
            MediaUpload.Image(imageBytes, imageMime, "first.jpg"),
            sourceUri = null,
        )
        viewModel.onAttachMedia(
            MediaUpload.Image(imageBytes, imageMime, "second.jpg"),
            sourceUri = null,
        )
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.pendingAttachments.size)

        viewModel.onClearAttachments()
        advanceUntilIdle()

        assertEquals(emptyList<PendingMedia>(), viewModel.uiState.value.pendingAttachments)
    }

    @Test
    fun onClearAttachments_is_a_no_op_when_already_empty() = runTest {
        // Stale recompositions must not crash the chat.
        viewModel.onClearAttachments()
        advanceUntilIdle()
        assertEquals(emptyList<PendingMedia>(), viewModel.uiState.value.pendingAttachments)
    }

    @Test
    fun upload_failure_preserves_pending_attachments() = runTest {
        val cause = IOException("network failure")

        coEvery {
            uploadAttachmentsAndSend(
                prompt = "Tengo una gotera en el baño",
                conversationId = any(),
                attachments = any(),
            )
        } returns SendDiagnosisPromptOutcome.Failure.Network(cause)

        viewModel.onAttachMedia(
            MediaUpload.Image(
                imageBytes,
                imageMime,
                imageName,
            ),
            sourceUri = null,
        )

        viewModel.onPromptChange("Tengo una gotera en el baño")
        viewModel.onSendClick()

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.sending)
        assertEquals(ChatError.Network, state.transientError)

        assertEquals(
            "failed upload must preserve the pending attachment",
            1,
            state.pendingAttachments.size,
        )
        assertEquals(
            imageName,
            state.pendingAttachments.single().originalName,
        )
    }

    @Test
    fun post_upload_send_failure_moves_attachments_to_sentAttachments() =
        runTest {
            // 10-AIP: when the orchestrator carries
            // `partiallyUploadedAttachments` on the failure, the
            // VM must move them off `pendingAttachments` (those
            // bytes no longer need a re-upload) into a separate
            // `sentAttachments` snapshot the UI renders below the
            // optimistic bubble. Distinguishes from
            // `upload_failure_preserves_pending_attachments` where
            // the bytes never made it to the storage backend.
            val first = attachment("first.jpg")
            val second = attachment("second.jpg")
            coEvery {
                uploadAttachmentsAndSend(
                    prompt = any(),
                    conversationId = any(),
                    attachments = any(),
                )
            } returns SendDiagnosisPromptOutcome.Failure.Server(
                code = 500,
                message = "ia service down",
                partiallyUploadedAttachments = listOf(first, second),
            )

            viewModel.onAttachMedia(
                com.loresuelvo.consumer.domain.conversation.MediaUpload.Image(
                    bytes = first.bytes,
                    mimeType = first.mimeType,
                    originalName = first.originalName,
                ),
                sourceUri = null,
            )
            viewModel.onAttachMedia(
                com.loresuelvo.consumer.domain.conversation.MediaUpload.Image(
                    bytes = second.bytes,
                    mimeType = second.mimeType,
                    originalName = second.originalName,
                ),
                sourceUri = null,
            )
            viewModel.onPromptChange("Tengo una gotera en el baño")
            viewModel.onSendClick()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(
                "pendingAttachments must be empty after post-upload send failure",
                0,
                state.pendingAttachments.size,
            )
            assertEquals(
                "uploaded attachments must be preserved in sentAttachments",
                2,
                state.sentAttachments.size,
            )
            assertEquals(listOf("first.jpg", "second.jpg"), state.sentAttachments.map { it.originalName })
            assertTrue(
                "transientError must surface the IA failure",
                state.transientError is ChatError.ServiceUnavailable,
            )
        }

    @Test
    fun retry_with_pending_attachments_retries_upload_and_clears_pending() =
        runTest {
            val cause = IOException("network failure")

            coEvery {
                uploadAttachmentsAndSend(
                    prompt = "Tengo una gotera en el baño",
                    conversationId = any(),
                    attachments = any(),
                )
            } returnsMany listOf(
                SendDiagnosisPromptOutcome.Failure.Network(cause),
                SendDiagnosisPromptOutcome.Success(
                    Diagnosis(
                        conversationId = "conv-1",
                        messages = emptyList(),
                    ),
                ),
            )

            viewModel.onAttachMedia(
                MediaUpload.Image(
                    imageBytes,
                    imageMime,
                    imageName,
                ),
                sourceUri = null,
            )

            viewModel.onPromptChange("Tengo una gotera en el baño")
            viewModel.onSendClick()

            advanceUntilIdle()

            assertEquals(
                "the failed upload must leave the attachment staged",
                1,
                viewModel.uiState.value.pendingAttachments.size,
            )

            viewModel.onRetryClick()
            advanceUntilIdle()

            coVerify(exactly = 2) {
                uploadAttachmentsAndSend(
                    prompt = "Tengo una gotera en el baño",
                    conversationId = any(),
                    attachments = any(),
                )
            }

            val state = viewModel.uiState.value

            assertFalse(state.sending)
            assertTrue(state.pendingAttachments.isEmpty())
            assertNull(state.transientError)
            assertEquals("conv-1", state.conversationId)
        }

}
