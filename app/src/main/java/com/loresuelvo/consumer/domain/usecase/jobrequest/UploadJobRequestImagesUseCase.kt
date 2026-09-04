package com.loresuelvo.consumer.domain.usecase.jobrequest

import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import com.loresuelvo.consumer.domain.jobrequest.UploadJobRequestImagesOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrator for the contact-provider image upload step. When
 * the consumer submits a `JobRequest` with one or more images,
 * this use case runs the [FileRepository] presign → upload →
 * confirm pipeline for every staged image and returns the
 * confirmed file IDs so the calling VM can pass them as
 * `image_file_ids[]` to `POST /job-requests`.
 *
 * Why split this from [CreateJobRequestUseCase]: the wire
 * contract on the backend accepts UUIDs, not raw bytes. Splitting
 * the pipeline keeps the create use case trivially testable
 * (the use case never sees bytes; the VM never sees the
 * presign/upload/confirm protocol). Mirrors the discipline
 * `UploadAttachmentsAndSendUseCase` already established for the
 * AI diagnostic chat's `image_file_ids[]` path.
 *
 * Failure semantics:
 *  - Any single step failure short-circuits the pipeline. The
 *    VM surfaces the typed failure on the existing transient
 *    error card; no partial uploads reach the create round-trip.
 *  - The pipeline is `JOB_REQUEST_IMAGE` so the backend's
 *    `job_request_image` upload policy is enforced at presign
 *    time (mime + size cap).
 *  - Empty input short-circuits to `Success(emptyList())` so the
 *    VM can call this unconditionally without branching on
 *    `state.attachedImages.isEmpty()`.
 */
@Singleton
class UploadJobRequestImagesUseCase @Inject constructor(
    private val fileRepository: FileRepository,
) {
    suspend operator fun invoke(
        images: List<MediaUpload.Image>,
    ): UploadJobRequestImagesOutcome {
        if (images.isEmpty()) {
            return UploadJobRequestImagesOutcome.Success(emptyList())
        }
        val fileIds = mutableListOf<String>()
        for (image in images) {
            when (val outcome = uploadOne(image)) {
                is UploadOne.Success -> fileIds += outcome.fileId
                is UploadOne.Failure -> return outcome.toOutcome()
            }
        }
        return UploadJobRequestImagesOutcome.Success(fileIds)
    }

    private sealed interface UploadOne {
        data class Success(val fileId: String) : UploadOne

        sealed interface Failure : UploadOne {
            fun toOutcome(): UploadJobRequestImagesOutcome.Failure
        }

        data class NetworkFailure(val cause: Throwable) : Failure {
            override fun toOutcome() = UploadJobRequestImagesOutcome.Failure.Network(cause)
        }

        data class ServerFailure(val code: Int, val message: String) : Failure {
            override fun toOutcome() = UploadJobRequestImagesOutcome.Failure.Server(code, message)
        }

        data class UnauthorizedFailure(val message: String) : Failure {
            override fun toOutcome() = UploadJobRequestImagesOutcome.Failure.Unauthorized(message)
        }
    }

    private suspend fun uploadOne(image: MediaUpload.Image): UploadOne {
        val presign = fileRepository.presign(
            PresignUploadRequest(
                originalName = image.originalName,
                mimeType = image.mimeType,
                sizeBytes = image.bytes.size,
                purpose = FilePurpose.JOB_REQUEST_IMAGE,
            ),
        )
        when (presign) {
            is PresignUploadOutcome.Success -> Unit
            is PresignUploadOutcome.Failure.Network ->
                return UploadOne.NetworkFailure(presign.cause)
            is PresignUploadOutcome.Failure.Server ->
                return UploadOne.ServerFailure(
                    code = presign.code,
                    message = "${presign.message} for ${image.originalName}",
                )
            is PresignUploadOutcome.Failure.Unauthorized ->
                return UploadOne.UnauthorizedFailure(presign.message)
        }
        val presigned = presign.result

        val upload = fileRepository.uploadBytes(
            uploadUrl = presigned.uploadUrl,
            headers = presigned.headers,
            bytes = image.bytes,
        )
        when (upload) {
            is UploadBytesOutcome.Success -> Unit
            is UploadBytesOutcome.Failure.Network ->
                return UploadOne.NetworkFailure(upload.cause)
            is UploadBytesOutcome.Failure.Server ->
                return UploadOne.ServerFailure(
                    code = upload.code,
                    message = "${upload.message} for ${image.originalName}",
                )
            is UploadBytesOutcome.Failure.Unauthorized ->
                return UploadOne.UnauthorizedFailure(upload.message)
        }

        val confirm = fileRepository.confirm(
            fileId = presigned.fileId,
            request = ConfirmUploadRequest(
                key = presigned.key,
                mimeType = image.mimeType,
                sizeBytes = image.bytes.size,
            ),
        )
        return when (confirm) {
            is ConfirmUploadOutcome.Success ->
                UploadOne.Success(confirm.file.id)
            is ConfirmUploadOutcome.Failure.Network ->
                UploadOne.NetworkFailure(confirm.cause)
            is ConfirmUploadOutcome.Failure.Server ->
                UploadOne.ServerFailure(
                    code = confirm.code,
                    message = "${confirm.message} for ${image.originalName}",
                )
            is ConfirmUploadOutcome.Failure.Unauthorized ->
                UploadOne.UnauthorizedFailure(confirm.message)
        }
    }
}