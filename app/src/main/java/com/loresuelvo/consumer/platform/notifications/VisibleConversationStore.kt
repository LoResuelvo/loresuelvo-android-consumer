package com.loresuelvo.consumer.platform.notifications

import com.loresuelvo.consumer.domain.notifications.ConversationVisibility
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisibleConversationStore @Inject constructor() : ConversationVisibility {
    @Volatile private var conversationId: Int? = null
    override fun visibleConversation() = conversationId
    override fun show(conversationId: Int?) { this.conversationId = conversationId }
}
