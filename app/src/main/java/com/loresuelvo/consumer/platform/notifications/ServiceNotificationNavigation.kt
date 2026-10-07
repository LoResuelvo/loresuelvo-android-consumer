package com.loresuelvo.consumer.platform.notifications

import android.content.Intent
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import javax.inject.Inject

data class ServiceTarget(val destination: String, val resourceId: String)

class ServiceNotificationNavigation @Inject constructor(
    private val authorize: AuthorizeServiceNotificationUseCase,
) {
    fun target(intent: Intent): ServiceTarget? {
        val notification = ServiceNotificationIntent().read(intent) ?: return null
        if (!authorize(notification)) return null
        return ServiceTarget(notification.destination, notification.resourceId)
    }
}
