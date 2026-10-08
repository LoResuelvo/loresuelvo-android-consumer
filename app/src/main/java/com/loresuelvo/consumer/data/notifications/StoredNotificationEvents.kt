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

    private val inMemoryEvents: MutableSet<String> = LinkedHashSet()

    init {
        synchronized(this) {
            inMemoryEvents.addAll(persistedEntries())
        }
    }

    @Synchronized
    override fun contains(bindingId: String, eventId: String): Boolean {
        val key = "$bindingId|$eventId"
        if (inMemoryEvents.contains(key)) return true
        val disk = persistedEntries()
        inMemoryEvents.addAll(disk)
        return inMemoryEvents.contains(key)
    }

    @Synchronized
    override fun remember(bindingId: String, eventId: String): Boolean {
        val key = "$bindingId|$eventId"
        inMemoryEvents.add(key)
        val bounded = (persistedEntries() + key).distinct().takeLast(128)
        val persisted = preferences.edit().putString(KEY_EVENTS, bounded.joinToString("\n")).commit()
        if (persisted) {
            inMemoryEvents.clear()
            inMemoryEvents.addAll(bounded)
        }
        return persisted
    }

    private fun persistedEntries(): List<String> =
        preferences.getString(KEY_EVENTS, "").orEmpty()
            .split('\n')
            .filter { it.isNotBlank() }

    companion object {
        private const val KEY_EVENTS = "notification_events"
    }
}
