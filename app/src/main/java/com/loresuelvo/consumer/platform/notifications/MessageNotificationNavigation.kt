package com.loresuelvo.consumer.platform.notifications

import android.content.Intent
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import javax.inject.Inject

class MessageNotificationNavigation @Inject constructor(
    private val authorize: AuthorizeMessageNotificationUseCase,
) {
    fun conversationId(intent: Intent): Int? {
        val notification = MessageNotificationIntent().read(intent) ?: return null
        return notification.conversationId.takeIf { authorize(notification) }
    }
}
