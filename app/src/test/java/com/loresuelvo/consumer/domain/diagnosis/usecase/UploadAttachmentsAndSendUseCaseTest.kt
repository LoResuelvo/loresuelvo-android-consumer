package com.loresuelvo.consumer.domain.diagnosis.usecase

import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.ConfirmedFile
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.PresignUploadResult
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import com.loresuelvo.consumer.ui.screens.chat.PendingMedia
import com.loresuelvo.consumer.ui.screens.chat.PendingMediaKind
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UploadAttachmentsAndSendUseCase]. Pins the
 * orchestration contract for 06-AIP — the consumer's "Diagnosticar"
 * tap must upload every staged image via the presign → upload →
 * confirm pipeline and then dispatch the prompt with the joined
 * `image_file_ids[]`.
 *
 * Companion file: [SendDiagnosisPromptUseCaseTest] (covers the
 * prompt-only path).
 */
class UploadAttachmentsAndSendUseCaseTest {

    private val fileRepository = mockk<FileRepository>()
    private val sendDiagnosisPrompt = mockk<SendDiagnosisPromptUseCase>(relaxed = true)
    private lateinit var useCase: UploadAttachmentsAndSendUseCase

    private val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    private fun attachment(name: String) = PendingMedia(
        localUri = null,
        mimeType = "image/jpeg",
        originalName = name,
        sizeBytes = jpegBytes.size.toLong(),
        bytes = jpegBytes,
        kind = PendingMediaKind.IMAGE,
        durationMillis = 0L,
    )

    private fun presignResult(id: String, fileId: String) = PresignUploadOutcome.Success(
        PresignUploadResult(
            fileId = fileId,
            key = "key-$id",
            uploadUrl = "https://upload.test/$id",
            headers = mapOf("Content-Type" to "image/jpeg"),
        ),
    )

    private val confirmResult = ConfirmUploadOutcome.Success(
        ConfirmedFile(
            id = "placeholder-id",
            mimeType = "image/jpeg",
            originalName = "placeholder.jpg",
            codec = "",
            durationSeconds = 0,
        ),
    )

    private val diagnosis = Diagnosis(
        conversationId = "conv-1",
        messages = emptyList(),
    )

    @org.junit.Before
    fun setUp() {
        useCase = UploadAttachmentsAndSendUseCase(fileRepository, sendDiagnosisPrompt)
        coEvery { fileRepository.uploadBytes(any(), any(), any()) } returns
            UploadBytesOutcome.Success
    }

    @Test
    fun uploadAttachmentsAndSend_uploads_each_then_dispatches_prompt_with_joined_ids() = runTest {
        coEvery { fileRepository.presign(any()) } returnsMany listOf(
            presignResult("upload-1", fileId = "file-1"),
            presignResult("upload-2", fileId = "file-2"),
        )
        coEvery { fileRepository.confirm("file-1", any()) } returns
            ConfirmUploadOutcome.Success(
                ConfirmedFile(
                    id = "file-1",
                    mimeType = "image/jpeg",
                    originalName = "first.jpg",
                    codec = "",
                    durationSeconds = 0,
                ),
            )
        coEvery { fileRepository.confirm("file-2", any()) } returns
            ConfirmUploadOutcome.Success(
                ConfirmedFile(
                    id = "file-2",
                    mimeType = "image/jpeg",
                    originalName = "second.jpg",
                    codec = "",
                    durationSeconds = 0,
                ),
            )
        coEvery { sendDiagnosisPrompt(any(), any(), any()) } returns
            SendDiagnosisPromptOutcome.Success(diagnosis)

        val outcome = useCase(
            prompt = "Tengo una gotera en el baño",
            conversationId = "conv-1",
            attachments = listOf(attachment("first.jpg"), attachment("second.jpg")),
        )

        // Two presigns, two uploads, two confirms — once per
        // staged attachment.
        coVerify(exactly = 2) { fileRepository.presign(any()) }
        coVerify(exactly = 2) { fileRepository.uploadBytes(any(), any(), jpegBytes) }
        coVerify(exactly = 2) { fileRepository.confirm(any(), any()) }
        // Final dispatch carries the joined file IDs.
        coVerify(exactly = 1) {
            sendDiagnosisPrompt(
                prompt = "Tengo una gotera en el baño",
                existingConversationId = "conv-1",
                imageFileIds = listOf("file-1", "file-2"),
            )
        }
        assertTrue(outcome is SendDiagnosisPromptOutcome.Success)
        assertEquals(diagnosis, (outcome as SendDiagnosisPromptOutcome.Success).diagnosis)
    }

    @Test
    fun uploadAttachmentsAndSend_presigns_with_conversation_message_image_purpose() = runTest {
        coEvery { fileRepository.presign(any()) } returns presignResult("upload-1", fileId = "file-1")
        coEvery { fileRepository.confirm(any(), any()) } returns
            ConfirmUploadOutcome.Success(
                ConfirmedFile(
                    id = "file-1",
                    mimeType = "image/jpeg",
                    originalName = "a.jpg",
                    codec = "",
                    durationSeconds = 0,
                ),
            )
        coEvery { sendDiagnosisPrompt(any(), any(), any()) } returns
            SendDiagnosisPromptOutcome.Success(diagnosis)

        useCase(
            prompt = "p",
            conversationId = "conv-1",
            attachments = listOf(attachment("a.jpg")),
        )

        coVerify(exactly = 1) {
            fileRepository.presign(
                match {
                    it.originalName == "a.jpg" &&
                        it.mimeType == "image/jpeg" &&
                        it.purpose == FilePurpose.CONVERSATION_MESSAGE_IMAGE
                },
            )
        }
    }

    @Test
    fun uploadAttachmentsAndSend_short_circuits_on_presign_failure() = runTest {
        coEvery { fileRepository.presign(any()) } returns
            PresignUploadOutcome.Failure.Server(code = 500, message = "boom")
        coEvery { sendDiagnosisPrompt(any(), any(), any()) } returns
            SendDiagnosisPromptOutcome.Success(diagnosis)

        val outcome = useCase(
            prompt = "p",
            conversationId = "conv-1",
            attachments = listOf(attachment("a.jpg")),
        )

        assertTrue(
            "presign failure must short-circuit the round-trip",
            outcome is SendDiagnosisPromptOutcome.Failure.Server,
        )
        coVerify(exactly = 0) { fileRepository.uploadBytes(any(), any(), any()) }
        coVerify(exactly = 0) { fileRepository.confirm(any(), any()) }
        coVerify(exactly = 0) { sendDiagnosisPrompt(any<String>(), any<String>(), any()) }
    }

    @Test
    fun uploadAttachmentsAndSend_short_circuits_on_upload_bytes_failure() = runTest {
        coEvery { fileRepository.presign(any()) } returns presignResult("upload-1", fileId = "file-1")
        coEvery { fileRepository.uploadBytes(any(), any(), any()) } returns
            UploadBytesOutcome.Failure.Server(code = 502, message = "boom")
        coEvery { sendDiagnosisPrompt(any(), any(), any()) } returns
            SendDiagnosisPromptOutcome.Success(diagnosis)

        val outcome = useCase(
            prompt = "p",
            conversationId = "conv-1",
            attachments = listOf(attachment("a.jpg")),
        )

        assertTrue(outcome is SendDiagnosisPromptOutcome.Failure.Server)
        coVerify(exactly = 0) { fileRepository.confirm(any(), any()) }
        coVerify(exactly = 0) { sendDiagnosisPrompt(any<String>(), any<String>(), any()) }
    }

    @Test
    fun uploadAttachmentsAndSend_short_circuits_on_confirm_failure() = runTest {
        coEvery { fileRepository.presign(any()) } returns presignResult("upload-1", fileId = "file-1")
        coEvery { fileRepository.uploadBytes(any(), any(), any()) } returns
            UploadBytesOutcome.Success
        coEvery { fileRepository.confirm(any(), any()) } returns
            ConfirmUploadOutcome.Failure.Server(code = 500, message = "boom")
        coEvery { sendDiagnosisPrompt(any(), any(), any()) } returns
            SendDiagnosisPromptOutcome.Success(diagnosis)

        val outcome = useCase(
            prompt = "p",
            conversationId = "conv-1",
            attachments = listOf(attachment("a.jpg")),
        )

        assertTrue(outcome is SendDiagnosisPromptOutcome.Failure.Server)
        coVerify(exactly = 0) { sendDiagnosisPrompt(any<String>(), any<String>(), any()) }
    }

    @Test
    fun uploadAttachmentsAndSend_surfaces_attach_failure_through_pendingAttachmentError() = runTest {
        // 08-AIP scope — the use case is the seam. Pinning the
        // behaviour here so the VM can map to a typed error
        // without re-implementing the orchestration.
        coEvery { fileRepository.presign(any()) } returns
            PresignUploadOutcome.Failure.Network(java.io.IOException("dns"))
        coEvery { sendDiagnosisPrompt(any(), any(), any()) } returns
            SendDiagnosisPromptOutcome.Success(diagnosis)

        val outcome = useCase(
            prompt = "p",
            conversationId = "conv-1",
            attachments = listOf(attachment("a.jpg")),
        )

        assertTrue(outcome is SendDiagnosisPromptOutcome.Failure.Network)
    }

    @Test
    fun uploadAttachmentsAndSend_send_failure_partially_uploaded_attachments_carries_them() =
        runTest {
            // 10-AIP: when the upload pipeline succeeds but the
            // backend rejects the message (IA service 5xx), the
            // orchestrated use case must carry the uploaded bytes
            // forward on the Failure so the VM can keep them
            // attached to the optimistic bubble and the user can
            // retry without re-uploading.
            val a = attachment("first.jpg")
            val b = attachment("second.jpg")
            coEvery { fileRepository.presign(any()) } returnsMany listOf(
                presignResult("upload-1", fileId = "file-1"),
                presignResult("upload-2", fileId = "file-2"),
            )
            coEvery { fileRepository.confirm("file-1", any()) } returns
                ConfirmUploadOutcome.Success(
                    ConfirmedFile(
                        id = "file-1",
                        mimeType = "image/jpeg",
                        originalName = "first.jpg",
                        codec = "",
                        durationSeconds = 0,
                    ),
                )
            coEvery { fileRepository.confirm("file-2", any()) } returns
                ConfirmUploadOutcome.Success(
                    ConfirmedFile(
                        id = "file-2",
                        mimeType = "image/jpeg",
                        originalName = "second.jpg",
                        codec = "",
                        durationSeconds = 0,
                    ),
                )
            coEvery { sendDiagnosisPrompt(any(), any(), any()) } returns
                SendDiagnosisPromptOutcome.Failure.Server(
                    code = 500,
                    message = "ia service down",
                )

            val outcome = useCase(
                prompt = "p",
                conversationId = "conv-1",
                attachments = listOf(a, b),
            )

            val failure = outcome as SendDiagnosisPromptOutcome.Failure.Server
            assertEquals(
                "both uploaded attachments must be carried on the failure",
                listOf(a, b),
                failure.partiallyUploadedAttachments,
            )
        }
}
