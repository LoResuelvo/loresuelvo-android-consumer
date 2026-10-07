package com.loresuelvo.consumer.platform.notifications

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.loresuelvo.consumer.domain.notifications.NotificationPlatformCapability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNotificationPlatformCapability @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationPlatformCapability {

    override fun requiresRuntimeNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
