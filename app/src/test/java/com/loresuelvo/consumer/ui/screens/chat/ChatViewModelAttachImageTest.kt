package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.diagnosis.usecase.LoadAiConversationUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
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
        viewModel = ChatViewModel(useCase, loadUseCase, mediaReader)
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
}
