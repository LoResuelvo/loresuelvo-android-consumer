package com.loresuelvo.consumer.platform.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import javax.inject.Inject

class NotificationSettingsIntent @Inject constructor(
    private val intentFactory: (String) -> Intent = { Intent(it) },
) {
    fun create(context: Context): Intent =
        intentFactory(ACTION).apply {
            action = ACTION
            putExtra(EXTRA_APP_PACKAGE, context.packageName)
        }

    companion object {
        const val ACTION = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        const val EXTRA_APP_PACKAGE = Settings.EXTRA_APP_PACKAGE
    }
}
