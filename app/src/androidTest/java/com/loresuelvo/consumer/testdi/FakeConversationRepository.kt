package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeConversationRepository @Inject constructor() : ConversationRepository {

    private var detailSeed: ConversationDetail? = null
    private var conversationsSeed: List<Conversation>? = null

    fun setConversationsSeed(conversations: List<Conversation>) {
        conversationsSeed = conversations
    }

    fun setDetailSeed(detail: ConversationDetail) {
        detailSeed = detail
    }

    fun clear() {
        detailSeed = null
        conversationsSeed = null
    }

    override suspend fun getConversationById(
        conversationId: String,
    ): ConversationDetailOutcome {
        val seeded = detailSeed
            ?: return ConversationDetailOutcome.Failure.Server(
                code = 404,
                message = "FakeConversationRepository: no detail seeded",
            )

        return ConversationDetailOutcome.Success(seeded)
    }
    override suspend fun getConversations(): ConversationsOutcome =
        ConversationsOutcome.Success(conversationsSeed ?: emptyList())

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
    ): SendMessageOutcome = SendMessageOutcome.Failure.Server(
        code = 500,
        message = "FakeConversationRepository: sendMessage not implemented",
    )

    override suspend fun sendMediaMessage(
        conversationId: String,
        media: List<MediaUpload>,
    ): SendMessageOutcome = SendMessageOutcome.Failure.Server(
        code = 500,
        message = "FakeConversationRepository: sendMediaMessage not implemented",
    )
}
