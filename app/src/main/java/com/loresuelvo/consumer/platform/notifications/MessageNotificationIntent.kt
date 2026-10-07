package com.loresuelvo.consumer.platform.notifications

import android.content.Context
import android.content.Intent
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.domain.notifications.MessageNotification

class MessageNotificationIntent {
    fun create(context: Context, notification: MessageNotification): Intent = Intent(context, MainActivity::class.java)
        .setAction(ACTION)
        .setData(android.net.Uri.parse("loresuelvo-notification://${notification.bindingId}/${notification.eventId}"))
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra("version", notification.version).putExtra("event_id", notification.eventId)
        .putExtra("type", notification.type).putExtra("resource_type", notification.resourceType)
        .putExtra("destination", notification.destination).putExtra("resource_id", notification.conversationId)
        .putExtra("recipient_user_id", notification.recipientId).putExtra("recipient_app", notification.app)
        .putExtra("installation_id", notification.installationId).putExtra("binding_id", notification.bindingId)
        .putExtra("expires_at", notification.expiresAt)

    fun read(intent: Intent): MessageNotification? = try { decode(intent) }
    catch (_: android.os.BadParcelableException) { null }
    catch (_: ClassCastException) { null }

    private fun decode(intent: Intent): MessageNotification? {
        if (intent.action != ACTION) return null
        return MessageNotification(
            version = intent.getStringExtra("version").orEmpty(),
            eventId = intent.getStringExtra("event_id").orEmpty(),
            type = intent.getStringExtra("type").orEmpty(),
            resourceType = intent.getStringExtra("resource_type").orEmpty(),
            destination = intent.getStringExtra("destination").orEmpty(),
            conversationId = intent.getIntExtra("resource_id", 0),
            recipientId = intent.getIntExtra("recipient_user_id", 0),
            app = intent.getStringExtra("recipient_app").orEmpty(),
            installationId = intent.getStringExtra("installation_id").orEmpty(),
            bindingId = intent.getStringExtra("binding_id").orEmpty(),
            title = "", body = "", expiresAt = intent.getLongExtra("expires_at", 0),
        )
    }

    companion object { const val ACTION = "com.loresuelvo.consumer.OPEN_MESSAGE_NOTIFICATION" }
}
