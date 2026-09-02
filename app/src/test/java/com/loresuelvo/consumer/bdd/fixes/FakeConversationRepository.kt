package com.loresuelvo.consumer.bdd.fixes

import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome

/**
 * Test-only [ConversationRepository] used by the 08-UXUI BDD
 * smoke test. Returns a single empty `Success` so the
 * [com.loresuelvo.consumer.ui.screens.messages.MessagesListViewModel]
 * settles on `Ready` and the BDD step can assert the renderable
 * surface exists. The visual sweep (status bar / nav bar / IME
 * insets) is carried out by the dev on a device.
 *
 * Only the list path is exercised; the other methods throw
 * `UnsupportedOperationException` so an accidental call from
 * another test surfaces as a clear failure rather than silently
 * returning a stub.
 */
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
