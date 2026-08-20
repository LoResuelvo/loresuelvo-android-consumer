package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ConfirmFileRequestDto
import com.loresuelvo.consumer.data.api.dto.FileResponseDto
import com.loresuelvo.consumer.data.api.dto.PresignFileRequestDto
import com.loresuelvo.consumer.data.api.dto.PresignFileResponseDto
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.ConfirmedFile
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.PresignUploadResult

/**
 * DTO ↔ domain translation for the presign / confirm file
 * upload flow. snake_case ↔ camelCase lives here per
 * AGENTS.md; the domain never sees wire types.
 *
 * [FilePurpose] case names are intentionally identical to the
 * backend's wire identifiers (`profile_photo`,
 * `conversation_message_audio`, `conversation_message_video`),
 * so [purposeToWire] / [purposeFromWire] are trivial
 * `lowercase` / `uppercase` round-trips. Adding a new purpose
 * anywhere — domain enum, OpenAPI enum, backend `file.go`
 * constant — without updating the other two is a build error
 * the test suite catches.
 */

internal fun PresignUploadRequest.toDto(): PresignFileRequestDto =
    PresignFileRequestDto(
        originalName = originalName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        purpose = purposeToWire(purpose),
    )

internal fun ConfirmUploadRequest.toDto(): ConfirmFileRequestDto =
    ConfirmFileRequestDto(
        key = key,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
    )

internal fun PresignFileResponseDto.toDomain(): PresignUploadResult =
    PresignUploadResult(
        fileId = fileId,
        key = key,
        uploadUrl = uploadUrl,
        headers = headers,
    )

internal fun FileResponseDto.toDomain(): ConfirmedFile =
    // The backend only returns the nested block for the
    // matching `type` discriminator; for any other case we
    // default to empty / zero so the domain stays a uniform
    // shape and callers always read the same fields.
    ConfirmedFile(
        id = id,
        mimeType = mimeType,
        originalName = originalName,
        codec = audio?.codec.orEmpty(),
        durationSeconds = audio?.durationSeconds ?: 0,
    )

internal fun purposeToWire(purpose: FilePurpose): String =
    when (purpose) {
        FilePurpose.PROFILE_PHOTO -> "profile_photo"
        FilePurpose.CONVERSATION_MESSAGE_AUDIO -> "conversation_message_audio"
        FilePurpose.CONVERSATION_MESSAGE_VIDEO -> "conversation_message_video"
        FilePurpose.CONVERSATION_MESSAGE_IMAGE -> "conversation_message_image"
    }

internal fun purposeFromWire(value: String): FilePurpose =
    when (value) {
        "profile_photo" -> FilePurpose.PROFILE_PHOTO
        "conversation_message_audio" -> FilePurpose.CONVERSATION_MESSAGE_AUDIO
        "conversation_message_video" -> FilePurpose.CONVERSATION_MESSAGE_VIDEO
        "conversation_message_image" -> FilePurpose.CONVERSATION_MESSAGE_IMAGE
        else -> throw IllegalArgumentException(
            "Unknown file purpose: $value",
        )
    }