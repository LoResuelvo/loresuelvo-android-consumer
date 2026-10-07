package com.loresuelvo.consumer.domain.notifications

fun interface NotificationClock {
    fun now(): Long
}
