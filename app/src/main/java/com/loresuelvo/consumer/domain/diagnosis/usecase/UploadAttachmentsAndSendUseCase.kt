package com.loresuelvo.consumer.domain.diagnosis.usecase

import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import com.loresuelvo.consumer.ui.screens.chat.PendingMedia
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrator for 06-AIP. When the consumer taps "Diagnosticar"
 * with one or more images staged, this use case uploads each
 * attachment through the [FileRepository] presign → upload →
 * confirm pipeline and then dispatches the prompt with the
 * joined `image_file_ids[]`.
 *
 * Failure semantics:
 *  - Any single step failure short-circuits the pipeline. No
 *    partial uploads reach the chat message endpoint — the user
 *    retries the whole batch.
 *  - `presign` / `uploadBytes` / `confirm` failures are mapped
 *    to the closest [SendDiagnosisPromptOutcome.Failure] variant
 *    so the VM renders the same error surface as the
 *    prompt-only path (Network / Server / Unauthorized).
 *  - The dispatch step's own failures (the message endpoint
 *    rejecting the joined IDs) propagate unchanged.
 *
 * Why not a separate "upload" use case: scenario 06-AIP treats
 * "upload" + "send prompt" as a single atomic user action. A
 * two-phase flow (upload → display IDs → "Send" tap) would force
 * the VM to re-derive `image_file_ids[]` after a config change,
 * which the current state machine doesn't carry.
 */
@Singleton
class UploadAttachmentsAndSendUseCase @Inject constructor(
    private val fileRepository: FileRepository,
    private val sendDiagnosisPrompt: SendDiagnosisPromptUseCase,
) {
    suspend operator fun invoke(
        prompt: String,
        conversationId: String,
        attachments: List<PendingMedia>,
    ): SendDiagnosisPromptOutcome {
        val fileIds = mutableListOf<String>()
        for (attachment in attachments) {
            val outcome = uploadOne(attachment)
            if (outcome is UploadFailure) return outcome.toSendFailure(attachment.originalName)
            fileIds += (outcome as UploadSuccess).fileId
        }
        return sendDiagnosisPrompt(
            prompt = prompt,
            existingConversationId = conversationId,
            imageFileIds = fileIds,
        )
    }

    private sealed interface UploadResult
    private data class UploadSuccess(val fileId: String) : UploadResult
    private sealed interface UploadFailure : UploadResult {
        fun toSendFailure(name: String): SendDiagnosisPromptOutcome.Failure
    }
    private data class PresignFailure(val cause: Throwable) : UploadFailure {
        override fun toSendFailure(name: String) =
            SendDiagnosisPromptOutcome.Failure.Network(cause)
    }
    private data class UploadBytesFailure(val cause: Throwable) : UploadFailure {
        override fun toSendFailure(name: String) =
            SendDiagnosisPromptOutcome.Failure.Network(cause)
    }
    private data class ConfirmFailure(val code: Int, val message: String) : UploadFailure {
        override fun toSendFailure(name: String) =
            SendDiagnosisPromptOutcome.Failure.Server(code, "$message for $name")
    }

    private suspend fun uploadOne(attachment: PendingMedia): UploadResult {
        val presign = fileRepository.presign(
            PresignUploadRequest(
                originalName = attachment.originalName,
                mimeType = attachment.mimeType,
                sizeBytes = attachment.bytes.size,
                purpose = FilePurpose.CONVERSATION_MESSAGE_IMAGE,
            ),
        )
        when (presign) {
            is PresignUploadOutcome.Success -> Unit
            is PresignUploadOutcome.Failure.Network ->
                return PresignFailure(presign.cause)
            is PresignUploadOutcome.Failure.Server ->
                return ConfirmFailure(presign.code, presign.message)
            is PresignUploadOutcome.Failure.Unauthorized ->
                return ConfirmFailure(code = 401, message = presign.message)
        }
        val presigned = presign.result

        val upload = fileRepository.uploadBytes(
            uploadUrl = presigned.uploadUrl,
            headers = presigned.headers,
            bytes = attachment.bytes,
        )
        when (upload) {
            is UploadBytesOutcome.Success -> Unit
            is UploadBytesOutcome.Failure.Network ->
                return PresignFailure(upload.cause)
            is UploadBytesOutcome.Failure.Server ->
                return ConfirmFailure(upload.code, upload.message)
            is UploadBytesOutcome.Failure.Unauthorized ->
                return ConfirmFailure(code = 401, message = upload.message)
        }

        val confirm = fileRepository.confirm(
            fileId = presigned.fileId,
            request = ConfirmUploadRequest(
                key = presigned.key,
                mimeType = attachment.mimeType,
                sizeBytes = attachment.bytes.size,
            ),
        )
        return when (confirm) {
            is ConfirmUploadOutcome.Success ->
                UploadSuccess(confirm.file.id)
            is ConfirmUploadOutcome.Failure.Network ->
                PresignFailure(confirm.cause)
            is ConfirmUploadOutcome.Failure.Server ->
                ConfirmFailure(confirm.code, confirm.message)
            is ConfirmUploadOutcome.Failure.Unauthorized ->
                ConfirmFailure(code = 401, message = confirm.message)
        }
    }

    /**
     * Runs the three-step upload for [attachment]. Returns the
     * confirmed file ID on success or a [UploadFailure] subtype
     * when any step fails. Each failure subtype knows how to
     * map itself onto the closest [SendDiagnosisPromptOutcome.Failure]
     * variant so the VM can render a single error surface.
     */
}

