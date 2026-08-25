package com.loresuelvo.consumer.domain.usecase.conversation

import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.MAX_AUDIO_BYTES
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
 * The port now accepts a list of media so the consumer can stage
 * several images in one session and send them as a single
 * bubble (mirrors the chat-with-AI flow's `pendingAttachments`
 * surface). The wire contract at
 * `POST /conversations/{id}/messages` already carries an
 * `image_file_ids[]` array, so the repository loops over each
 * attachment running the presign → upload → confirm pipeline and
 * then posts a single message with the joined IDs.
 *
 * "Empty" means the list is empty OR every entry has
 * `bytes.isEmpty()`. This catches the corner case where the
 * picker returns a URI but the [android.content.ContentResolver]
 * can't open an `InputStream` (revoked permission, deleted
 * file, misconfigured file provider) — without the guard the
 * repository would post an empty multipart body and the backend
 * would reject it with a less-actionable 400.
 *
 * For audio, an additional guard rejects clips larger than
 * [MAX_AUDIO_BYTES] with a typed
 * [SendMessageOutcome.Failure.PayloadTooLarge] (scenario 09-MM)
 * so the backend never sees the request — saves a wasted
 * round-trip and gives the UI a clearer copy than a generic
 * `Server(413)`.
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
        media: List<MediaUpload>,
    ): SendMessageOutcome {
        if (media.isEmpty() || media.all { it.bytes.isEmpty() }) {
            return SendMessageOutcome.Failure.Server(
                code = 0,
                message = "Media payload is empty",
            )
        }
        media.forEach { attachment ->
            if (attachment is MediaUpload.Audio &&
                attachment.bytes.size.toLong() > MAX_AUDIO_BYTES
            ) {
                return SendMessageOutcome.Failure.PayloadTooLarge(
                    maxBytes = MAX_AUDIO_BYTES,
                )
            }
        }
        return conversationRepository.sendMediaMessage(conversationId, media)
    }
}