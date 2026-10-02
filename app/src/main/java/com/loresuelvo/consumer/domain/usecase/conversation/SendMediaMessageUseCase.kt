package com.loresuelvo.consumer.domain.usecase.conversation

import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.MAX_AUDIO_BYTES
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.conversation.validationError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendMediaMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(
        conversationId: String,
        media: List<MediaUpload>,
        content: String = "",
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
            if (attachment is MediaUpload.Video) {
                val validationError = attachment.validationError()
                if (validationError != null) {
                    return SendMessageOutcome.Failure.Server(
                        code = 422,
                        message = validationError.toString(),
                    )
                }
            }
        }
        val hasVideo = media.any { it is MediaUpload.Video }
        val hasNonVideo = media.any { it !is MediaUpload.Video }
        if (hasVideo && hasNonVideo || media.count { it is MediaUpload.Video } > 1) {
            return SendMessageOutcome.Failure.Server(
                code = 422,
                message = "Video messages cannot be combined with other media",
            )
        }
        return if (content.isBlank()) {
            conversationRepository.sendMediaMessage(conversationId, media)
        } else {
            conversationRepository.sendMediaMessageWithCaption(
                conversationId = conversationId,
                media = media,
                content = content,
            )
        }
    }
}
