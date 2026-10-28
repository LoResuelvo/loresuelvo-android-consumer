package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.diagnosis.ChatImage
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.diagnosis.Sender
import com.loresuelvo.consumer.domain.diagnosis.usecase.LoadAiConversationUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.UploadAttachmentsAndSendUseCase
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmedFile
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadResult
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import com.loresuelvo.consumer.platform.media.MediaReader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
class ChatViewModelImageVisibilityTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<DiagnosisRepository>()
    private val files = mockk<FileRepository>()
    private lateinit var viewModel: ChatViewModel

    private val uploadedImage = ChatImage(
        "file-1", "https://images.test/file-1.jpg", "photo.jpg", "image/jpeg",
    )
    private val response = SendDiagnosisPromptOutcome.Success(
        Diagnosis(
            conversationId = "42",
            messages = listOf(
                ChatMessage("1", Sender.Consumer, "", 1L, images = listOf(uploadedImage)),
                ChatMessage("2", Sender.Assistant, "Veo la imagen.", 2L),
            ),
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val sendPrompt = SendDiagnosisPromptUseCase(repository)
        viewModel = ChatViewModel(
            sendPrompt,
            LoadAiConversationUseCase(repository),
            mockk<MediaReader>(),
            UploadAttachmentsAndSendUseCase(files, sendPrompt),
        )
        coEvery { files.presign(any()) } returns successfulPresign()
        coEvery { files.uploadBytes(any(), any(), any()) } returns UploadBytesOutcome.Success
        coEvery { files.confirm(any(), any()) } returns ConfirmUploadOutcome.Success(
            ConfirmedFile("file-1", "image/jpeg", "photo.jpg", "", 0),
        )
        coEvery { repository.sendPrompt(any(), any(), any()) } coAnswers { awaitCancellation() }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun selected_images_are_visible_in_the_message_while_waiting_for_the_assistant() = runTest {
        attachImage("photo.jpg", "content://gallery/1")
        attachImage("second.jpg", "content://gallery/2")
        viewModel.onPromptChange("Así está la pérdida")

        viewModel.onSendClick()
        advanceUntilIdle()

        val message = viewModel.uiState.value.messages.single()
        assertEquals("Así está la pérdida", message.content)
        assertEquals(listOf("content://gallery/1", "content://gallery/2"), message.images.map { it.url })
        assertEquals(listOf("photo.jpg", "second.jpg"), message.images.map { it.originalName })
        assertTrue(viewModel.uiState.value.sending)
    }

    @Test
    fun first_photo_creates_a_conversation_and_replaces_local_images_with_server_references() = runTest {
        coEvery { repository.sendPrompt(any(), any(), any()) } returns response
        attachImage("photo.jpg", "content://gallery/1")

        viewModel.onSendClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.sendPrompt("", null, listOf("file-1")) }
        val state = viewModel.uiState.value
        assertEquals("42", state.conversationId)
        assertEquals(listOf(uploadedImage), state.messages.first().images)
        assertTrue(state.pendingAttachments.isEmpty())
        assertFalse(state.sending)
    }

    @Test
    fun retrying_a_failed_photo_upload_without_text_keeps_the_same_message_and_images() = runTest {
        coEvery { files.presign(any()) } returnsMany listOf(
            PresignUploadOutcome.Failure.Network(IOException("Upload interrupted")),
            successfulPresign(),
        )
        attachImage("photo.jpg", "content://gallery/1")
        viewModel.onSendClick()
        advanceUntilIdle()
        val messageId = viewModel.uiState.value.messages.single().id

        viewModel.onRetryClick()
        advanceUntilIdle()

        coVerify(exactly = 2) { files.presign(any()) }
        coVerify(exactly = 1) { repository.sendPrompt("", null, listOf("file-1")) }
        val state = viewModel.uiState.value
        assertEquals(messageId, state.messages.single().id)
        assertEquals("content://gallery/1", state.messages.single().images.single().url)
        assertTrue(state.sending)
    }

    @Test
    fun retrying_after_upload_keeps_the_photo_and_reuses_the_uploaded_file_without_text() = runTest {
        coEvery { repository.sendPrompt(any(), any(), any()) } returnsMany listOf(
            SendDiagnosisPromptOutcome.Failure.Server(503, "Assistant unavailable"),
            response,
        )
        attachImage("photo.jpg", "content://gallery/1")
        viewModel.onSendClick()
        advanceUntilIdle()
        assertEquals("content://gallery/1", viewModel.uiState.value.messages.single().images.single().url)

        viewModel.onRetryClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { files.presign(any()) }
        coVerify(exactly = 2) { repository.sendPrompt("", null, listOf("file-1")) }
        assertEquals(listOf(uploadedImage), viewModel.uiState.value.messages.first().images)
        assertTrue(viewModel.uiState.value.sentAttachmentFileIds.isEmpty())
        assertFalse(viewModel.uiState.value.sending)
    }

    private fun attachImage(name: String, uri: String) {
        val source = mockk<Uri>(relaxed = true).also { every { it.toString() } returns uri }
        viewModel.onAttachMedia(MediaUpload.Image(byteArrayOf(1, 2, 3), "image/jpeg", name), source)
    }

    private fun successfulPresign() = PresignUploadOutcome.Success(
        PresignUploadResult(
            fileId = "file-1",
            key = "private/photo.jpg",
            uploadUrl = "https://uploads.test/photo.jpg",
            headers = emptyMap(),
        ),
    )
}
