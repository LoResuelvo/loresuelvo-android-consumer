package com.loresuelvo.consumer.domain.realtime

import com.loresuelvo.consumer.domain.conversation.ConversationMessage

data class WsEvent(
    val type: String,
    val conversationId: Long,
    val message: ConversationMessage,
) {
    companion object {
        /**
         * WebSocket discriminator the backend emits when a new
         * message is appended to a conversation. The constant is
         * here (not in the data layer) so the mapper and any
         * future event consumer can compare against a stable
         * name without re-importing the wire constant.
         */
        const val CONVERSATION_MESSAGE_CREATED: String =
            "conversation.message.created"
    }
}