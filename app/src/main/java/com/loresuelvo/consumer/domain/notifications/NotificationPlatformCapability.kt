package com.loresuelvo.consumer.domain.notifications

interface NotificationPlatformCapability {
    fun requiresRuntimeNotificationPermission(): Boolean
    fun areNotificationsEnabled(): Boolean
}
