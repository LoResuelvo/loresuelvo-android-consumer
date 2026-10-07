package com.loresuelvo.consumer.platform.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.notifications.ServiceNotification
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationAvailability
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationPublisher
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidServiceNotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authorize: AuthorizeServiceNotificationUseCase,
) : ServiceNotificationPublisher, ServiceNotificationAvailability {
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    override fun servicesAllowed(): Boolean {
        createChannel()
        return NotificationManagerCompat.from(context).areNotificationsEnabled() && channelAllowed()
    }

    override fun publish(notification: ServiceNotification): Boolean {
        if (!authorize(notification) || !servicesAllowed()) return false
        val intent = ServiceNotificationIntent().create(context, notification)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val publicVersion = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_service)
            .setContentTitle(context.getString(R.string.app_name))
            .build()
        val visible = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_service)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        if (!authorize(notification)) return false
        manager.notify("${notification.bindingId}:${notification.eventId}", 0, visible)
        return true
    }

    private fun channelAllowed(): Boolean = Build.VERSION.SDK_INT < 26 ||
        manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        if (manager.getNotificationChannel(CHANNEL) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.notification_services_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }

    companion object {
        const val CHANNEL = "services"
    }
}
