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
     * Dispatches on [MediaUpload] subtype and runs the
     * presign → upload → confirm dance before posting the
     * JSON message body. Both subtypes share the file-upload
     * pipeline through [FileRepository]; only the `purpose`,
     * the field on the JSON message (`audio_file_id` vs
     * `image_file_ids[]`), and the diagnostic log tag differ.
     *
     *  - Audio: `purpose = conversation_message_audio`,
     *    `audio_file_id = <uuid>`. Audio is exclusive on the
     *    message (no `content` / `image_file_ids` /
     *    `video_file_id`) per the OpenAPI `not.anyOf` rules and
     *    the backend's
     *    `internal/domain/conversation/service.go:113-115`
     *    `ErrMessageAudioMustBeExclusive`.
     *  - Image: `purpose = conversation_message_image`,
     *    `image_file_ids = [<uuid>]`. The consumer app sends
     *    one image per message (single-valued
     *    `MediaReference.Image`); the backend accepts up to
     *    five (`MaxConversationMessageImages = 5` in
     *    `internal/domain/file/upload_policy.go:12`). A future
     *    multi-image iteration fans out to multiple IDs.
     */
    override suspend fun sendMediaMessage(
        conversationId: String,
        media: MediaUpload,
    ): SendMessageOutcome = when (media) {
        is MediaUpload.Audio -> sendAudio(conversationId, media)
        is MediaUpload.Image -> sendImage(conversationId, media)
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
        val fileId = when (
            val r = runPresignUploadConfirm(
                originalName = audio.originalName,
                mimeType = audio.mimeType,
                bytes = audio.bytes,
                purpose = FilePurpose.CONVERSATION_MESSAGE_AUDIO,
            )
        ) {
            is UploadFlow.Failure -> return r.failure
            is UploadFlow.Success -> r.fileId
        }
        return postMessageWithAudioFileId(conversationId, fileId)
    }

    private suspend fun sendImage(
        conversationId: String,
        image: MediaUpload.Image,
    ): SendMessageOutcome {
        Log.d(
            TAG,
            "sendImage start: conversationId=$conversationId " +
                "mime=${image.mimeType} size=${image.bytes.size}B " +
                "originalName=${image.originalName}",
        )
        val fileId = when (
            val r = runPresignUploadConfirm(
                originalName = image.originalName,
                mimeType = image.mimeType,
                bytes = image.bytes,
                purpose = FilePurpose.CONVERSATION_MESSAGE_IMAGE,
            )
        ) {
            is UploadFlow.Failure -> return r.failure
            is UploadFlow.Success -> r.fileId
        }
        return postMessageWithImageFileId(conversationId, fileId)
    }

    /**
     * Outcome of the presign → upload → confirm pipeline. The
     * repository never throws: every HTTP / network failure is
     * mapped here so the caller (the message dispatcher) just
     * returns the carried [SendMessageOutcome.Failure] verbatim.
     */
    private sealed interface UploadFlow {
        data class Success(val fileId: String) : UploadFlow
        data class Failure(val failure: SendMessageOutcome.Failure) : UploadFlow
    }

    /**
     * Runs the three-step upload flow for [bytes] under
     * [purpose]. Returns the backend-issued file UUID on
     * success, or the typed [SendMessageOutcome.Failure] on
     * the first failure (logged with `purpose` for triage). The
     * caller maps [UploadFlow.Failure] back into the same
     * failure hierarchy the user already sees on the existing
     * media error card.
     */
    private suspend fun runPresignUploadConfirm(
        originalName: String,
        mimeType: String,
        bytes: ByteArray,
        purpose: FilePurpose,
    ): UploadFlow {
        // 1) presign
        val presignResult = when (
            val outcome = fileRepository.presign(
                PresignUploadRequest(
                    originalName = originalName,
                    mimeType = mimeType,
                    sizeBytes = bytes.size,
                    purpose = purpose,
                ),
            )
        ) {
            is PresignUploadOutcome.Success -> outcome.result
            is PresignUploadOutcome.Failure -> {
                Log.w(
                    TAG,
                    "presign[$purpose] failed: ${outcome::class.simpleName} " +
                        "code=${(outcome as? PresignUploadOutcome.Failure.Server)?.code} " +
                        "message=${(outcome as? PresignUploadOutcome.Failure.Server)?.message}",
                )
                return UploadFlow.Failure(collapsePresignFailure(outcome))
            }
        }

        // 2) upload bytes to the storage URL
        when (
            val outcome = fileRepository.uploadBytes(
                uploadUrl = presignResult.uploadUrl,
                headers = presignResult.headers,
                bytes = bytes,
            )
        ) {
            is UploadBytesOutcome.Success -> Unit
            is UploadBytesOutcome.Failure -> {
                Log.w(
                    TAG,
                    "uploadBytes[$purpose] failed: ${outcome::class.simpleName} " +
                        "code=${(outcome as? UploadBytesOutcome.Failure.Server)?.code} " +
                        "url=$presignResult.uploadUrl",
                )
                return UploadFlow.Failure(collapseUploadFailure(outcome))
            }
        }

        // 3) confirm
        return when (
            val outcome = fileRepository.confirm(
                fileId = presignResult.fileId,
                request = ConfirmUploadRequest(
                    key = presignResult.key,
                    mimeType = mimeType,
                    sizeBytes = bytes.size,
                ),
            )
        ) {
            is ConfirmUploadOutcome.Success ->
                UploadFlow.Success(outcome.file.id)
            is ConfirmUploadOutcome.Failure -> {
                Log.w(
                    TAG,
                    "confirm[$purpose] failed: ${outcome::class.simpleName} " +
                        "code=${(outcome as? ConfirmUploadOutcome.Failure.Server)?.code} " +
                        "message=${(outcome as? ConfirmUploadOutcome.Failure.Server)?.message}",
                )
                UploadFlow.Failure(collapseConfirmFailure(outcome))
            }
        }
    }

    private suspend fun postMessageWithAudioFileId(
        conversationId: String,
        fileId: String,
    ): SendMessageOutcome = try {
        val dto = backendApi.postMessage(
            conversationId,
            SendMessageRequestDto(
                content = "",
                audioFileId = fileId,
            ),
        )
        SendMessageOutcome.Success(dto.toDomain())
    } catch (t: Throwable) {
        mapSendFailure(t)
    }

    private suspend fun postMessageWithImageFileId(
        conversationId: String,
        fileId: String,
    ): SendMessageOutcome = try {
        val dto = backendApi.postMessage(
            conversationId,
            SendMessageRequestDto(
                content = "",
                imageFileIds = listOf(fileId),
            ),
        )
        SendMessageOutcome.Success(dto.toDomain())
    } catch (t: Throwable) {
        mapSendFailure(t)
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