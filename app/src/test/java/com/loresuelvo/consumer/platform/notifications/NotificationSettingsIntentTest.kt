package com.loresuelvo.consumer.platform.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSettingsIntentTest {

    @Test
    fun creates_intent_for_app_notification_settings() {
        val context = mockk<Context>()
        every { context.packageName } returns "com.loresuelvo.consumer"

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Settings.ACTION_APP_NOTIFICATION_SETTINGS
        every { intent.getStringExtra(Settings.EXTRA_APP_PACKAGE) } returns "com.loresuelvo.consumer"

        val generator = NotificationSettingsIntent { intent }
        val created = generator.create(context)

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, created.action)
        assertEquals("com.loresuelvo.consumer", created.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }
}
