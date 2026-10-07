package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStatus
import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStore
import com.loresuelvo.consumer.domain.notifications.NotificationPlatformCapability
import javax.inject.Inject

class GetNotificationPermissionStatusUseCase @Inject constructor(
    private val store: NotificationPermissionStore,
    private val platform: NotificationPlatformCapability,
) {
    operator fun invoke(): NotificationPermissionStatus {
        if (platform.areNotificationsEnabled()) {
            return NotificationPermissionStatus.GRANTED
        }
        if (store.hasDecided()) {
            return NotificationPermissionStatus.DENIED
        }
        return if (platform.requiresRuntimeNotificationPermission()) {
            NotificationPermissionStatus.UNDECIDED
        } else {
            NotificationPermissionStatus.DENIED
        }
    }
}
