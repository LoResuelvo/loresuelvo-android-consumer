package com.loresuelvo.consumer.platform.notifications

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class FirebaseReceptionBootstrapTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @After fun cleanup() { FirebaseApp.getApps(context).forEach { it.delete() } }

    @Test fun missing_configuration_does_not_create_default_firebase_app() {
        val provider = FirebaseRegistrationTokenProvider(context, FirebaseRegistrationConfiguration("", "", "", ""))
        assertFalse(provider.initializeForReception())
        assertTrue(FirebaseApp.getApps(context).isEmpty())
    }

    @Test fun configured_cold_boot_creates_and_reuses_default_app_without_requesting_token() {
        val config = FirebaseRegistrationConfiguration("1:123456789:android:abc123", "test-api-key", "test-project", "123456789")
        assertTrue(FirebaseRegistrationTokenProvider(context, config).initializeForReception())
        assertEquals(config.applicationId, FirebaseApp.getInstance().options.applicationId)
        assertTrue(FirebaseRegistrationTokenProvider(context, config).initializeForReception())
        assertEquals(1, FirebaseApp.getApps(context).size)
        assertFalse(FirebaseRegistrationTokenProvider(context, config.copy(projectId = "other-project")).initializeForReception())
    }
}
