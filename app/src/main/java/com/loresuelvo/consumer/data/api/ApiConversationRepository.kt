package com.loresuelvo.consumer.data.api

import android.util.Log
import com.loresuelvo.consumer.data.api.dto.SendMessageRequestDto
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [ConversationRepository] against the
 * backend. Adapts the three consumer ↔ provider conversation
 * endpoints (`GET /conversations`, `GET /conversations/{id}`,
 * `POST /conversations/{id}/messages`) plus the presign / confirm
 * file upload flow (`POST /files/presign`, `POST /files/{id}/confirm`)
 * to the domain's typed outcome hierarchies.
 *
 * [sendMediaMessage] drives the three-step upload before it posts
 * the JSON message body — the backend does NOT accept multipart on
 * the conversations endpoint, so the client always uses the file
 * UUID the confirm step returns as `audio_file_id` (audio) or
 * `image_file_ids[]` (image, future). See
 * `openapi/paths/conversation-messages.yaml` and
 * `openapi/components/schemas/send-message-request.yaml` for the
 * wire contract; the Go-side reference flow is in the backend's
 * `features/steps/send_audio_test.go` `consumerSentAudioInActiveChat`.
 *
 * Like every other adapter in this package, it never throws on
 * HTTP / network failures: every exception is translated to a
 * typed `Failure` via [toApiError], so callers handle each branch
 * explicitly. The 401 branch maps to each outcome's dedicated
 * `Unauthorized` subtype so the VM can clear the local session
 * when the JWT expires. Failures from [FileRepository] are
 * collapsed into the same `SendMessageOutcome.Failure` tree so
 * the ViewModel renders them with the existing transient media
 * error card.
 */
@Singleton
class ApiConversationRepository @Inject constructor(
    private val backendApi: BackendApi,
    private val fileRepository: FileRepository,
) : ConversationRepository {

    override suspend fun getConversations(): ConversationsOutcome = try {
        val dtos = backendApi.getConversations()
        ConversationsOutcome.Success(dtos.map { it.toDomain() })
    } catch (t: Throwable) {
        mapConversationsFailure(t)
    }

    override suspend fun getConversationById(
        conversationId: String,
    ): ConversationDetailOutcome = try {
        val dto = backendApi.getConversationById(conversationId)
        ConversationDetailOutcome.Success(dto.toDomain())
    } catch (t: Throwable) {
        mapDetailFailure(t)
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
    ): SendMessageOutcome = try {
        val dto = backendApi.postMessage(
            conversationId,
            SendMessageRequestDto(content = content),
        )
        SendMessageOutcome.Success(dto.toDomain())
    } catch (t: Throwable) {
        mapSendFailure(t)
    }

    /**
     * Audio-only path for Phase 1: the WebM/Opus bytes go
     * through the presign → upload → confirm pipeline with
     * `purpose = conversation_message_audio`, and the backend's
     * returned `ConfirmedFile.id` (UUID) becomes the JSON
     * `audio_file_id` on the final `POST /messages` call. Audio
     * is exclusive (no `content`, no `image_file_ids`, no
     * `video_file_id`) per
     * `openapi/components/schemas/send-message-request.yaml`
     * `not.anyOf` rules — see also the backend's
     * `internal/domain/conversation/service.go:113-115` that
     * returns `ErrMessageAudioMustBeExclusive`.
     *
     * Image attachments are not yet supported on this code
     * path; the Phase 2 iteration will dispatch on
     * [MediaUpload] subtype and use `image_file_ids[]` against
     * the same `files/presign` + `files/{id}/confirm` flow
     * with `purpose = conversation_message_image`.
     */
    override suspend fun sendMediaMessage(
        conversationId: String,
        media: MediaUpload,
    ): SendMessageOutcome = when (media) {
        is MediaUpload.Audio -> sendAudio(conversationId, media)
        is MediaUpload.Image ->
            SendMessageOutcome.Failure.Server(
                code = 0,
                message = "Image attachments are not supported yet",
            )
    }

    private suspend fun sendAudio(
        conversationId: String,
        audio: MediaUpload.Audio,
    ): SendMessageOutcome {
        Log.d(
            TAG,
            "sendAudio start: conversationId=$conversationId " +
                "mime=${audio.mimeType} size=${audio.bytes.size}B " +
                "originalName=${audio.originalName}",
        )
        // 1) presign
        val presignResult = when (
            val outcome = fileRepository.presign(
                PresignUploadRequest(
                    originalName = audio.originalName,
                    mimeType = audio.mimeType,
                    sizeBytes = audio.bytes.size,
                    purpose = FilePurpose.CONVERSATION_MESSAGE_AUDIO,
                ),
            )
        ) {
            is PresignUploadOutcome.Success -> outcome.result
            is PresignUploadOutcome.Failure -> {
                Log.w(
                    TAG,
                    "presign failed: ${outcome::class.simpleName} " +
                        "code=${(outcome as? PresignUploadOutcome.Failure.Server)?.code} " +
                        "message=${(outcome as? PresignUploadOutcome.Failure.Server)?.message}",
                )
                return collapsePresignFailure(outcome)
            }
        }

        // 2) upload bytes to the storage URL
        when (
            val outcome = fileRepository.uploadBytes(
                uploadUrl = presignResult.uploadUrl,
                headers = presignResult.headers,
                bytes = audio.bytes,
            )
        ) {
            is UploadBytesOutcome.Success -> Unit
            is UploadBytesOutcome.Failure -> {
                Log.w(
                    TAG,
                    "uploadBytes failed: ${outcome::class.simpleName} " +
                        "code=${(outcome as? UploadBytesOutcome.Failure.Server)?.code} " +
                        "url=$presignResult.uploadUrl",
                )
                return collapseUploadFailure(outcome)
            }
        }

        // 3) confirm
        val confirmedId = when (
            val outcome = fileRepository.confirm(
                fileId = presignResult.fileId,
                request = ConfirmUploadRequest(
                    key = presignResult.key,
                    mimeType = audio.mimeType,
                    sizeBytes = audio.bytes.size,
                ),
            )
        ) {
            is ConfirmUploadOutcome.Success -> outcome.file.id
            is ConfirmUploadOutcome.Failure -> {
                Log.w(
                    TAG,
                    "confirm failed: ${outcome::class.simpleName} " +
                        "code=${(outcome as? ConfirmUploadOutcome.Failure.Server)?.code} " +
                        "message=${(outcome as? ConfirmUploadOutcome.Failure.Server)?.message}",
                )
                return collapseConfirmFailure(outcome)
            }
        }

        // 4) post the JSON message with audio_file_id
        return try {
            val dto = backendApi.postMessage(
                conversationId,
                SendMessageRequestDto(
                    content = "",
                    audioFileId = confirmedId,
                ),
            )
            SendMessageOutcome.Success(dto.toDomain())
        } catch (t: Throwable) {
            mapSendFailure(t)
        }
    }

    private fun collapsePresignFailure(
        failure: PresignUploadOutcome.Failure,
    ): SendMessageOutcome.Failure = when (failure) {
        is PresignUploadOutcome.Failure.Network ->
            SendMessageOutcome.Failure.Network(failure.cause)
        is PresignUploadOutcome.Failure.Server ->
            SendMessageOutcome.Failure.Server(failure.code, failure.message)
        is PresignUploadOutcome.Failure.Unauthorized ->
            SendMessageOutcome.Failure.Unauthorized(failure.message)
    }

    private fun collapseUploadFailure(
        failure: UploadBytesOutcome.Failure,
    ): SendMessageOutcome.Failure = when (failure) {
        is UploadBytesOutcome.Failure.Network ->
            SendMessageOutcome.Failure.Network(failure.cause)
        is UploadBytesOutcome.Failure.Server ->
            SendMessageOutcome.Failure.Server(failure.code, failure.message)
        is UploadBytesOutcome.Failure.Unauthorized ->
            SendMessageOutcome.Failure.Unauthorized(failure.message)
    }

    private fun collapseConfirmFailure(
        failure: ConfirmUploadOutcome.Failure,
    ): SendMessageOutcome.Failure = when (failure) {
        is ConfirmUploadOutcome.Failure.Network ->
            SendMessageOutcome.Failure.Network(failure.cause)
        is ConfirmUploadOutcome.Failure.Server ->
            SendMessageOutcome.Failure.Server(failure.code, failure.message)
        is ConfirmUploadOutcome.Failure.Unauthorized ->
            SendMessageOutcome.Failure.Unauthorized(failure.message)
    }

    private fun mapConversationsFailure(
        t: Throwable,
    ): ConversationsOutcome.Failure = when (val error = t.toApiError()) {
        is ApiError.Network ->
            ConversationsOutcome.Failure.Network(error.networkCause)
        is ApiError.Unauthorized ->
            ConversationsOutcome.Failure.Unauthorized(error.errorMessage)
        is ApiError.Server ->
            ConversationsOutcome.Failure.Server(error.code, error.errorMessage)
        is ApiError.Unknown ->
            ConversationsOutcome.Failure.Server(0, error.message ?: "Unknown error")
    }

    private fun mapDetailFailure(
        t: Throwable,
    ): ConversationDetailOutcome.Failure = when (val error = t.toApiError()) {
        is ApiError.Network ->
            ConversationDetailOutcome.Failure.Network(error.networkCause)
        is ApiError.Unauthorized ->
            ConversationDetailOutcome.Failure.Unauthorized(error.errorMessage)
        is ApiError.Server ->
            ConversationDetailOutcome.Failure.Server(error.code, error.errorMessage)
        is ApiError.Unknown ->
            ConversationDetailOutcome.Failure.Server(0, error.message ?: "Unknown error")
    }

    private fun mapSendFailure(
        t: Throwable,
    ): SendMessageOutcome.Failure = when (val error = t.toApiError()) {
        is ApiError.Network ->
            SendMessageOutcome.Failure.Network(error.networkCause)
        is ApiError.Unauthorized ->
            SendMessageOutcome.Failure.Unauthorized(error.errorMessage)
        is ApiError.Server ->
            SendMessageOutcome.Failure.Server(error.code, error.errorMessage)
        is ApiError.Unknown ->
            SendMessageOutcome.Failure.Server(0, error.message ?: "Unknown error")
    }

    private companion object {
        const val TAG: String = "ApiConversationRepo"
    }
}