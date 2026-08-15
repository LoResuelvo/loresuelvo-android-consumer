package com.loresuelvo.consumer.domain.usecase.conversation

import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-layer use case that wraps
 * [ConversationRepository.sendMediaMessage] with the same guard
 * rail as [SendMessageUseCase]: an empty payload must never
 * reach the backend.
 *
 * For media, "empty" means `MediaUpload.bytes.isEmpty()`. This
 * catches the corner case where the picker returns a URI but
 * the [android.content.ContentResolver] can't open an
 * `InputStream` (a revoked permission, a deleted file, a
 * misconfigured file provider) — without the guard the
 * repository would post an empty multipart body and the backend
 * would reject it with a less-actionable 400.
 *
 * The use case does NOT swallow typed repository failures
 * (Network / Server / Unauthorized); they propagate unchanged so
 * the VM can render each branch explicitly, mirroring
 * [SendMessageUseCase].
 */
@Singleton
class SendMediaMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(
        conversationId: String,
        media: MediaUpload,
    ): SendMessageOutcome {
        if (media.bytes.isEmpty()) {
            return SendMessageOutcome.Failure.Server(
                code = 0,
                message = "Media payload is empty",
            )
        }
        return conversationRepository.sendMediaMessage(conversationId, media)
    }
}