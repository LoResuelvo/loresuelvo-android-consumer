package com.loresuelvo.consumer.domain.usecase.auth

import com.loresuelvo.consumer.domain.auth.UploadProfilePhotoOutcome
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import javax.inject.Inject
import javax.inject.Singleton

/** Runs the profile-photo presign → direct upload → confirm flow. */
@Singleton
class UploadProfilePhotoUseCase @Inject constructor(
    private val fileRepository: FileRepository,
) {
    suspend operator fun invoke(
        photo: MediaUpload.Image,
    ): UploadProfilePhotoOutcome {
        val presign = fileRepository.presign(
            PresignUploadRequest(
                originalName = photo.originalName,
                mimeType = photo.mimeType,
                sizeBytes = photo.bytes.size,
                purpose = FilePurpose.PROFILE_PHOTO,
            ),
        )
        val target = when (presign) {
            is PresignUploadOutcome.Success -> presign.result
            is PresignUploadOutcome.Failure.Network ->
                return UploadProfilePhotoOutcome.Failure.Network(presign.cause)
            is PresignUploadOutcome.Failure.Server ->
                return UploadProfilePhotoOutcome.Failure.Server(presign.code, presign.message)
            is PresignUploadOutcome.Failure.Unauthorized ->
                return UploadProfilePhotoOutcome.Failure.Unauthorized(presign.message)
        }

        when (val upload = fileRepository.uploadBytes(target.uploadUrl, target.headers, photo.bytes)) {
            UploadBytesOutcome.Success -> Unit
            is UploadBytesOutcome.Failure.Network ->
                return UploadProfilePhotoOutcome.Failure.Network(upload.cause)
            is UploadBytesOutcome.Failure.Server ->
                return UploadProfilePhotoOutcome.Failure.Server(upload.code, upload.message)
            is UploadBytesOutcome.Failure.Unauthorized ->
                return UploadProfilePhotoOutcome.Failure.Unauthorized(upload.message)
        }

        return when (
            val confirm = fileRepository.confirm(
                fileId = target.fileId,
                request = ConfirmUploadRequest(
                    key = target.key,
                    mimeType = photo.mimeType,
                    sizeBytes = photo.bytes.size,
                ),
            )
        ) {
            is ConfirmUploadOutcome.Success ->
                UploadProfilePhotoOutcome.Success(confirm.file.id)
            is ConfirmUploadOutcome.Failure.Network ->
                UploadProfilePhotoOutcome.Failure.Network(confirm.cause)
            is ConfirmUploadOutcome.Failure.Server ->
                UploadProfilePhotoOutcome.Failure.Server(confirm.code, confirm.message)
            is ConfirmUploadOutcome.Failure.Unauthorized ->
                UploadProfilePhotoOutcome.Failure.Unauthorized(confirm.message)
        }
    }
}
