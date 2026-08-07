package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test-only [ConversationRepository] for the acceptance /
 * integration tests that `@UninstallModules(RepositoryModule::class)`.
 * The production binding lives in
 * [com.loresuelvo.consumer.di.RepositoryModule]; once a test
 * uninstalls that module it must rebind every port the ViewModels
 * under test transitively depend on — including the conversation
 * ports that [MessagesListViewModel] / [ConversationViewModel]
 * pull in (added in 03-IC onwards).
 *
 * Behaviour:
 *  - `getConversations` → empty `Success` (no provider chats yet).
 *  - `getConversationById` → 404-style `Failure.Server` so any
 *    accidental navigation to `Route.Conversation` lands on the
 *    error retry surface instead of crashing the test with a
 *    silent `Failure.Network`.
 *  - `sendMessage` → 500-style `Failure.Server` (the acceptance
 *    flow does not exercise sending — that's the BDD layer's
 *    job — but the binding must still satisfy the type so the
 *    Hilt component compiles).
 *
 * Acceptance tests that DO want to drive the conversation surface
 * can subclass or replace this fake with one that seeds a
 * specific state.
 */
@Singleton
class FakeConversationRepository @Inject constructor() : ConversationRepository {

    override suspend fun getConversations(): ConversationsOutcome =
        ConversationsOutcome.Success(emptyList())

    override suspend fun getConversationById(
        conversationId: String,
    ): ConversationDetailOutcome = ConversationDetailOutcome.Failure.Server(
        code = 404,
        message = "FakeConversationRepository: no detail seeded",
    )

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
    ): SendMessageOutcome = SendMessageOutcome.Failure.Server(
        code = 500,
        message = "FakeConversationRepository: sendMessage not implemented",
    )
}
