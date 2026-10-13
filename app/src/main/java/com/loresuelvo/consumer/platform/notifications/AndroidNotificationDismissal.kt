package com.loresuelvo.consumer.platform.notifications

import android.app.NotificationManager
import android.content.Context
import com.loresuelvo.consumer.domain.notifications.NotificationDismissal
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNotificationDismissal @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationDismissal {
    override fun dismissAll() {
        context.getSystemService(NotificationManager::class.java)?.cancelAll()
    }

    override fun dismissConversation(bindingId: String, conversationId: Int) {
        if (bindingId.isBlank() || conversationId <= 0) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val tagPrefix = MessageNotificationTag.forConversation(bindingId, conversationId)
        manager.activeNotifications
            .filter { it.tag?.startsWith(tagPrefix) == true }
            .forEach { manager.cancel(it.tag, it.id) }
    }
}
