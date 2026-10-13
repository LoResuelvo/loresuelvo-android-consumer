package com.loresuelvo.consumer.domain.notifications

fun interface NotificationDismissal {
    fun dismissAll()

    fun dismissConversation(bindingId: String, conversationId: Int) = Unit
}
