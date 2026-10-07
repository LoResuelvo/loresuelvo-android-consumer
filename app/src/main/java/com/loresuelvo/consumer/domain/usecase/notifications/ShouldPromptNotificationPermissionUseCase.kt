package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStore
import com.loresuelvo.consumer.domain.notifications.NotificationPlatformCapability
import javax.inject.Inject

class ShouldPromptNotificationPermissionUseCase @Inject constructor(
    private val store: NotificationPermissionStore,
    private val platform: NotificationPlatformCapability,
) {
    operator fun invoke(): Boolean {
        if (!platform.requiresRuntimeNotificationPermission()) return false
        return !store.hasDecided()
    }
}
