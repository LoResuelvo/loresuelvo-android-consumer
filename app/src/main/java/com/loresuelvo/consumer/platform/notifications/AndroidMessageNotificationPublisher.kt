package com.loresuelvo.consumer.platform.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.notifications.MessageNotification
import com.loresuelvo.consumer.domain.notifications.MessageNotificationPublisher
import com.loresuelvo.consumer.domain.notifications.NotificationAvailability
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidMessageNotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authorize: AuthorizeMessageNotificationUseCase,
) : MessageNotificationPublisher, NotificationAvailability {
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    override fun messagesAllowed(): Boolean {
        createChannel()
        return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            channelAllowed()
    }

    override fun publish(notification: MessageNotification): Boolean {
        if (!authorize(notification) || !messagesAllowed()) return false
        val intent = MessageNotificationIntent().create(context, notification)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val publicVersion = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_message).setContentTitle(context.getString(R.string.app_name)).build()
        val visible = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setContentTitle(notification.title).setContentText(notification.body)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(publicVersion)
            .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true).build()
        if (!authorize(notification)) return false
        manager.notify("${notification.bindingId}:${notification.eventId}", 0, visible)
        return true
    }

    private fun channelAllowed(): Boolean = Build.VERSION.SDK_INT < 26 ||
        manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        if (manager.getNotificationChannel(CHANNEL) != null) return
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, context.getString(R.string.notification_messages_channel), NotificationManager.IMPORTANCE_DEFAULT,
        ))
    }

    companion object { const val CHANNEL = "messages" }
}
