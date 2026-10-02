package com.loresuelvo.consumer.bdd.fixes

import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome

class FakeConversationRepository : ConversationRepository {
    override suspend fun getConversations(): ConversationsOutcome =
        ConversationsOutcome.Success(emptyList())

    override suspend fun getConversationById(
        conversationId: String,
    ): ConversationDetailOutcome =
        ConversationDetailOutcome.Failure.Server(
            code = 404,
            message = "FakeConversationRepository only supports the list path",
        )

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
    ): SendMessageOutcome = throw UnsupportedOperationException(
        "FakeConversationRepository does not support sendMessage",
    )

    override suspend fun sendMediaMessage(
        conversationId: String,
        media: List<MediaUpload>,
    ): SendMessageOutcome = throw UnsupportedOperationException(
        "FakeConversationRepository does not support sendMediaMessage",
    )
}
