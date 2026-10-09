package com.loresuelvo.consumer.domain.notifications

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class ConversationRefreshRequest(
    val conversationId: Int,
    val eventId: String,
    val recipientUserId: Int,
)

interface ConversationVisibility {
    fun visibleConversation(): Int?
    fun show(conversationId: Int?)
    val refreshRequests: Flow<ConversationRefreshRequest>
        get() = emptyFlow<ConversationRefreshRequest>()
    fun requestRefreshIfVisible(conversationId: Int, eventId: String, recipientUserId: Int): Boolean = false
    fun isCurrent(request: ConversationRefreshRequest): Boolean =
        visibleConversation() == request.conversationId

    companion object {
        val None = object : ConversationVisibility {
            override fun visibleConversation(): Int? = null
            override fun show(conversationId: Int?) = Unit
        }
    }
}
