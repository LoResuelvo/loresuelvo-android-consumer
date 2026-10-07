package com.loresuelvo.consumer.data.notifications

import android.content.SharedPreferences
import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStore
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class StoredNotificationPermission @Inject constructor(
    @Named("installationPrefs") private val preferences: SharedPreferences,
) : NotificationPermissionStore {

    override fun hasDecided(): Boolean = preferences.getBoolean(KEY_DECIDED, false)

    override fun isPermissionGranted(): Boolean = preferences.getBoolean(KEY_GRANTED, false)

    override fun recordDecision(granted: Boolean) {
        preferences.edit()
            .putBoolean(KEY_DECIDED, true)
            .putBoolean(KEY_GRANTED, granted)
            .commit()
    }

    companion object {
        private const val KEY_DECIDED = "notification_permission_decided"
        private const val KEY_GRANTED = "notification_permission_granted"
    }
}
