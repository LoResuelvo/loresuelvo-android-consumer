package com.loresuelvo.consumer.instrumented.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.data.notifications.StoredNotificationPermission
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStatus
import com.loresuelvo.consumer.domain.notifications.NotificationPlatformCapability
import com.loresuelvo.consumer.domain.usecase.notifications.GetNotificationPermissionStatusUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.RecordNotificationPermissionDecisionUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ShouldPromptNotificationPermissionUseCase
import com.loresuelvo.consumer.platform.notifications.AndroidNotificationPlatformCapability
import com.loresuelvo.consumer.platform.notifications.NotificationSettingsIntent
import com.loresuelvo.consumer.ui.screens.profile.ConsumerProfileScreen
import com.loresuelvo.consumer.ui.screens.profile.ConsumerProfileUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationPermissionInstrumentedTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun generates_valid_system_notification_settings_intent() {
        val intentGenerator = NotificationSettingsIntent()
        val intent = intentGenerator.create(context)

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(context.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test
    fun store_persists_permission_decision_on_device() {
        val prefs = context.getSharedPreferences("test_notification_permission_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val store = StoredNotificationPermission(prefs)
        assertFalse(store.hasDecided())
        assertFalse(store.isPermissionGranted())

        store.recordDecision(granted = true)
        assertTrue(store.hasDecided())
        assertTrue(store.isPermissionGranted())

        store.recordDecision(granted = false)
        assertTrue(store.hasDecided())
        assertFalse(store.isPermissionGranted())
    }

    @Test
    fun decision_prevents_subsequent_automatic_permission_prompts() {
        val prefs = context.getSharedPreferences("test_prompt_policy_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val store = StoredNotificationPermission(prefs)
        val platformCapability = AndroidNotificationPlatformCapability(context)
        val shouldPrompt = ShouldPromptNotificationPermissionUseCase(store, platformCapability)
        val recordDecision = RecordNotificationPermissionDecisionUseCase(store)

        if (platformCapability.requiresRuntimeNotificationPermission()) {
            assertTrue("Should prompt initially when undecided on Android 13+", shouldPrompt())
            recordDecision(granted = true)
            assertFalse("Must not prompt automatically once decided", shouldPrompt())
        } else {
            assertFalse("Should not prompt on pre-Android 13", shouldPrompt())
        }
    }

    @Test
    fun profile_renders_undecided_notification_card_and_triggers_enable() {
        var actionFired = false
        compose.setContent {
            ConsumerProfileScreen(
                state = ConsumerProfileUiState.Ready(
                    user = User(displayName = "Test User", firstName = "Test", lastName = "User"),
                    notificationPermission = NotificationPermissionStatus.UNDECIDED,
                ),
                onRetryClick = {},
                onNotificationActionClick = { actionFired = true },
            )
        }

        compose.onNodeWithTag("consumer-profile-content")
            .performScrollToNode(hasTestTag("consumer-profile-notifications"))
        compose.onNodeWithTag("consumer-profile-notifications").assertIsDisplayed()
        compose.onNodeWithTag("consumer-profile-notifications-title").assertIsDisplayed()
        compose.onNodeWithTag("consumer-profile-notifications-action")
            .assertHasClickAction()
            .performClick()

        assertTrue(actionFired)
    }

    @Test
    fun profile_renders_denied_notification_card_and_triggers_settings() {
        var settingsFired = false
        compose.setContent {
            ConsumerProfileScreen(
                state = ConsumerProfileUiState.Ready(
                    user = User(displayName = "Test User", firstName = "Test", lastName = "User"),
                    notificationPermission = NotificationPermissionStatus.DENIED,
                ),
                onRetryClick = {},
                onNotificationActionClick = { settingsFired = true },
            )
        }

        compose.onNodeWithTag("consumer-profile-content")
            .performScrollToNode(hasTestTag("consumer-profile-notifications"))
        compose.onNodeWithTag("consumer-profile-notifications").assertIsDisplayed()
        compose.onNodeWithTag("consumer-profile-notifications-action")
            .assertHasClickAction()
            .performClick()

        assertTrue(settingsFired)
    }
}
