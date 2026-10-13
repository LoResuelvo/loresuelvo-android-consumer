package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.notifications.ConversationVisibility
import com.loresuelvo.consumer.domain.notifications.NotificationDismissal
import com.loresuelvo.consumer.domain.notifications.NotificationInstallationReader

class DismissConversationNotificationsUseCase(
    private val sessions: AuthSessionStore,
    private val installations: NotificationInstallationReader,
    private val visibility: ConversationVisibility,
    private val dismissal: NotificationDismissal,
) {
    operator fun invoke(conversationId: Int) {
        if (conversationId <= 0) return
        synchronized(visibility) {
            val session = sessions.getSession() ?: return
            val installation = installations.confirmedInstallation() ?: return
            if (installation.userId != session.user.backendUserId) return
            dismissal.dismissConversation(installation.bindingId, conversationId)
        }
    }
}
