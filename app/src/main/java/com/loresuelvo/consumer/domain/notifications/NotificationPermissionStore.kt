package com.loresuelvo.consumer.domain.notifications

interface NotificationPermissionStore {
    fun hasDecided(): Boolean
    fun isPermissionGranted(): Boolean
    fun recordDecision(granted: Boolean)
}
