package com.loresuelvo.consumer.domain.notifications

interface ConversationVisibility {
    fun visibleConversation(): Int?
    fun show(conversationId: Int?)
}
