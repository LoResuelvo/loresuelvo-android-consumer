package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.diagnosis.usecase.LoadAiConversationUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
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
