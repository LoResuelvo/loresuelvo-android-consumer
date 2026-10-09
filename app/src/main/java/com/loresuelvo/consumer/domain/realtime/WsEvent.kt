package com.loresuelvo.consumer.domain.realtime

import com.loresuelvo.consumer.domain.conversation.ConversationMessage

sealed interface WsEvent {
    data class ConversationMessageCreated(
        val conversationId: Long,
        val message: ConversationMessage,
    ) : WsEvent

    data class NotificationCreated(
        val resourceType: String,
        val resourceId: String,
    ) : WsEvent

    companion object {
        const val CONVERSATION_MESSAGE_CREATED = "conversation.message.created"
        const val NOTIFICATION_CREATED = "notification.created"
        const val SERVICE_PROPOSAL_RESOURCE = "service_proposal"
        const val WORK_ORDER_RESOURCE = "work_order"
    }
}
