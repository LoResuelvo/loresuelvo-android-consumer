package com.loresuelvo.consumer.data.notifications

import android.content.SharedPreferences
import com.loresuelvo.consumer.domain.notifications.NotificationEventStore
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class StoredNotificationEvents @Inject constructor(
    @Named("installationPrefs") private val preferences: SharedPreferences,
) : NotificationEventStore {
    @Synchronized
    override fun contains(bindingId: String, eventId: String): Boolean =
        entries().contains("$bindingId|$eventId")

    @Synchronized
    override fun remember(bindingId: String, eventId: String): Boolean {
        val bounded = (entries() + "$bindingId|$eventId").takeLast(128)
        return preferences.edit().putString("notification_events", bounded.joinToString("\n")).commit()
    }

    private fun entries(): List<String> = preferences.getString("notification_events", "").orEmpty()
        .split('\n').filter { it.isNotBlank() }
}
