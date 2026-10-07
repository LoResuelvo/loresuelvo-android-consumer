package com.loresuelvo.consumer.domain.notifications

interface NotificationInstallationReader {
    fun confirmedInstallation(): NotificationInstallation?
}
