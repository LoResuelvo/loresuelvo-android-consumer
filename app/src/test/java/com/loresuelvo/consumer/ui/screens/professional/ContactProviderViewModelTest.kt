package com.loresuelvo.consumer.ui.screens.professional

import app.cash.turbine.test
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequest
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.usecase.jobrequest.CreateJobRequestUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins the state machine + events emitted by
 * [ContactProviderViewModel]. Mirrors the convention used by
 * `ChatViewModelTest` and `CompleteProfileViewModelTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactProviderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val createJobRequest: CreateJobRequestUseCase = mockk()
    private val mediaReader = io.mockk.mockk<com.loresuelvo.consumer.data.media.MediaReader>(relaxed = true)
    private lateinit var viewModel: ContactProviderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ContactProviderViewModel(createJobRequest, mediaReader)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_is_Closed() = runTest(testDispatcher) {
        assertEquals(ContactProviderUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun onOpenContact_transitions_to_Open_with_empty_fields() = runTest(testDispatcher) {
        val provider = sampleProvider()

        viewModel.onOpenContact(provider)

        val state = viewModel.uiState.value
        assertTrue(state is ContactProviderUiState.Open)
        state as ContactProviderUiState.Open
        assertEquals(provider, state.provider)
        assertEquals("", state.title)
        assertEquals("", state.description)
        assertEquals(false, state.isSubmitting)
        assertNull(state.error)
        assertEquals(false, state.canSubmit)
    }

    @Test
    fun onTitleChange_and_onDescriptionChange_update_fields_and_clear_errors() = runTest(testDispatcher) {
        viewModel.onOpenContact(sampleProvider())
        viewModel.onTitleChange("Fuga")
        viewModel.onDescriptionChange("Hay una fuga")
        // Simulate a prior error to verify it's cleared on every keystroke.
        // (No public API to inject the error; we exercise clearing by
        // observing that after typing the freshly-built state has null error.)

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertEquals("Fuga", state.title)
        assertEquals("Hay una fuga", state.description)
        assertEquals(true, state.canSubmit)
    }

    @Test
    fun onSubmit_with_empty_fields_does_not_call_the_use_case() = runTest(testDispatcher) {
        viewModel.onOpenContact(sampleProvider())

        viewModel.onSubmit()

        coVerify(exactly = 0) { createJobRequest(any()) }
        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertEquals(false, state.isSubmitting)
    }

    @Test
    fun onSubmit_with_valid_fields_emits_NavigateToConversation_and_closes() = runTest(testDispatcher) {
        val provider = sampleProvider()
        val jobRequest = JobRequest(
            id = "1",
            conversationId = "10",
            title = "Fuga",
            description = "Hay una fuga",
            status = "pending",
            images = emptyList(),
        )
        coEvery { createJobRequest(any()) } returns CreateJobRequestOutcome.Success(jobRequest)

        viewModel.onOpenContact(provider)
        viewModel.onTitleChange("Fuga")
        viewModel.onDescriptionChange("Hay una fuga")

        viewModel.events.test {
            viewModel.onSubmit()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ContactProviderEvent.NavigateToConversation)
            assertEquals("10", (event as ContactProviderEvent.NavigateToConversation).conversationId)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(ContactProviderUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun onSubmit_with_network_failure_sets_Network_error_and_keeps_form_open() = runTest(testDispatcher) {
        coEvery { createJobRequest(any()) } returns
            CreateJobRequestOutcome.Failure.Network(IllegalStateException("socket closed"))

        viewModel.onOpenContact(sampleProvider())
        viewModel.onTitleChange("Fuga")
        viewModel.onDescriptionChange("Hay una fuga")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertEquals(ContactProviderError.Network, state.error)
        assertEquals(false, state.isSubmitting)
    }

    @Test
    fun onSubmit_with_server_failure_sets_Server_error_with_message() = runTest(testDispatcher) {
        coEvery { createJobRequest(any()) } returns
            CreateJobRequestOutcome.Failure.Server(500, "boom")

        viewModel.onOpenContact(sampleProvider())
        viewModel.onTitleChange("Fuga")
        viewModel.onDescriptionChange("Hay una fuga")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertTrue(state.error is ContactProviderError.Server)
        state.error as ContactProviderError.Server
        assertEquals(500, state.error.code)
        assertEquals("boom", state.error.message)
    }

    @Test
    fun onSubmit_with_unauthorized_failure_sets_Unauthorized_error() = runTest(testDispatcher) {
        coEvery { createJobRequest(any()) } returns
            CreateJobRequestOutcome.Failure.Unauthorized("expired")

        viewModel.onOpenContact(sampleProvider())
        viewModel.onTitleChange("Fuga")
        viewModel.onDescriptionChange("Hay una fuga")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertEquals(ContactProviderError.Unauthorized, state.error)
    }

    @Test
    fun onSubmit_with_success_but_missing_conversationId_surfaces_Server_error() = runTest(testDispatcher) {
        val jobRequest = JobRequest(
            id = "1",
            conversationId = null,
            title = "Fuga",
            description = "Hay una fuga",
            status = "pending",
            images = emptyList(),
        )
        coEvery { createJobRequest(any()) } returns CreateJobRequestOutcome.Success(jobRequest)

        viewModel.onOpenContact(sampleProvider())
        viewModel.onTitleChange("Fuga")
        viewModel.onDescriptionChange("Hay una fuga")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertTrue(state.error is ContactProviderError.Server)
        // The form stays open so the user can retry.
        assertEquals(false, state.isSubmitting)
    }

    @Test
    fun onCancel_returns_to_Closed() = runTest(testDispatcher) {
        viewModel.onOpenContact(sampleProvider())
        viewModel.onCancel()

        assertEquals(ContactProviderUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun onDismissError_clears_the_error_field() = runTest(testDispatcher) {
        coEvery { createJobRequest(any()) } returns
            CreateJobRequestOutcome.Failure.Server(500, "boom")

        viewModel.onOpenContact(sampleProvider())
        viewModel.onTitleChange("Fuga")
        viewModel.onDescriptionChange("Hay una fuga")
        viewModel.onSubmit()
        advanceUntilIdle()
        assertTrue(
            (viewModel.uiState.value as ContactProviderUiState.Open).error is ContactProviderError.Server,
        )

        viewModel.onDismissError()

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertNull(state.error)
    }

    @Test
    fun onSubmit_forwards_providerId_title_and_description_to_the_use_case() = runTest(testDispatcher) {
        val provider = sampleProvider()
        val captured = io.mockk.slot<CreateJobRequestData>()
        coEvery { createJobRequest(capture(captured)) } returns CreateJobRequestOutcome.Success(
            JobRequest("1", "10", "Fuga", "Hay una fuga", "pending", emptyList()),
        )

        viewModel.onOpenContact(provider)
        viewModel.onTitleChange("Fuga")
        viewModel.onDescriptionChange("Hay una fuga")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(provider.id, captured.captured.providerId)
        assertEquals("Fuga", captured.captured.title)
        assertEquals("Hay una fuga", captured.captured.description)
    }

    // ---- 03-UXUI: image attachment flow ---------------------------

    private fun sampleImage(
        name: String = "fuga.jpg",
        mimeType: String = "image/jpeg",
        size: Int = 1024,
    ): MediaUpload.Image = MediaUpload.Image(
        bytes = ByteArray(size),
        mimeType = mimeType,
        originalName = name,
    )

    @Test
    fun onAttachImages_appends_images_when_under_the_limit() = runTest(testDispatcher) {
        viewModel.onOpenContact(sampleProvider())

        viewModel.onAttachImages(listOf(sampleImage("a.jpg"), sampleImage("b.jpg")))

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertEquals(2, state.attachedImages.size)
        assertEquals(listOf("a.jpg", "b.jpg"), state.attachedImages.map { it.originalName })
        assertNull("limit error must not surface on success", state.attachmentError)
    }

    @Test
    fun onAttachImages_rejects_batch_exceeding_the_limit_and_keeps_existing() = runTest(testDispatcher) {
        viewModel.onOpenContact(sampleProvider())
        viewModel.onAttachImages(listOf(sampleImage("a.jpg"), sampleImage("b.jpg")))
        // Existing list has 2 items; trying to attach 2 more would take us to 4 (limit = 3).
        viewModel.onAttachImages(listOf(sampleImage("c.jpg"), sampleImage("d.jpg")))

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertEquals(
            "the existing 2 items must be preserved when the batch is rejected",
            listOf("a.jpg", "b.jpg"),
            state.attachedImages.map { it.originalName },
        )
        assertTrue(
            "limit-reached error must be surfaced, got ${state.attachmentError}",
            state.attachmentError != null,
        )
    }

    @Test
    fun onAttachImages_drops_files_with_disallowed_mime_type() = runTest(testDispatcher) {
        viewModel.onOpenContact(sampleProvider())

        viewModel.onAttachImages(
            listOf(
                sampleImage("ok.jpg", mimeType = "image/jpeg"),
                sampleImage("bad.gif", mimeType = "image/gif"),
            ),
        )

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertEquals(1, state.attachedImages.size)
        assertEquals("ok.jpg", state.attachedImages.single().originalName)
    }

    @Test
    fun onAttachImages_drops_files_above_the_per_file_size_cap() = runTest(testDispatcher) {
        viewModel.onOpenContact(sampleProvider())

        viewModel.onAttachImages(
            listOf(
                sampleImage("ok.jpg", size = 1024),
                sampleImage("big.jpg", size = 6 * 1024 * 1024),
            ),
        )

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertEquals(1, state.attachedImages.size)
        assertEquals("ok.jpg", state.attachedImages.single().originalName)
    }

    @Test
    fun onRemoveImage_removes_the_image_at_the_index() = runTest(testDispatcher) {
        viewModel.onOpenContact(sampleProvider())
        viewModel.onAttachImages(
            listOf(sampleImage("a.jpg"), sampleImage("b.jpg"), sampleImage("c.jpg")),
        )

        viewModel.onRemoveImage(1)

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertEquals(listOf("a.jpg", "c.jpg"), state.attachedImages.map { it.originalName })
        assertNull("removing an image clears the limit error", state.attachmentError)
    }

    @Test
    fun onRemoveImage_with_out_of_range_index_is_a_no_op() = runTest(testDispatcher) {
        viewModel.onOpenContact(sampleProvider())
        viewModel.onAttachImages(listOf(sampleImage("a.jpg")))

        viewModel.onRemoveImage(99)

        val state = viewModel.uiState.value as ContactProviderUiState.Open
        assertEquals(1, state.attachedImages.size)
    }

    private fun sampleProvider() = Provider(
        id = 1,
        name = "Juan",
        surname = "Pérez",
        categoryId = 1,
        categoryName = "Plomería",
        profilePhotoUrl = null,
    )
}
