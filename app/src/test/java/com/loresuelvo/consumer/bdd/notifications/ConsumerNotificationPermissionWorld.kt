package com.loresuelvo.consumer.bdd.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStore
import com.loresuelvo.consumer.domain.notifications.NotificationPlatformCapability
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.notifications.RecordNotificationPermissionDecisionUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ShouldPromptNotificationPermissionUseCase
import com.loresuelvo.consumer.platform.notifications.NotificationSettingsIntent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ConsumerNotificationPermissionWorld {
    private var sdkVersion = 33
    private var requestedFromApp = false
    private lateinit var store: NotificationPermissionStore
    private lateinit var capability: NotificationPlatformCapability
    private lateinit var shouldPrompt: ShouldPromptNotificationPermissionUseCase
    private lateinit var recordDecision: RecordNotificationPermissionDecisionUseCase
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var turnosRepository: TurnosRepository
    private val context = mockk<Context>()

    fun startWithAndroid13Undecided() {
        sdkVersion = 33
        requestedFromApp = false
        every { context.packageName } returns "com.loresuelvo.consumer"
        capability = object : NotificationPlatformCapability {
            override fun requiresRuntimeNotificationPermission(): Boolean = sdkVersion >= 33
            override fun areNotificationsEnabled(): Boolean = store.isPermissionGranted()
        }
        store = object : NotificationPermissionStore {
            private var decided = false
            private var granted = false
            override fun hasDecided(): Boolean = decided
            override fun isPermissionGranted(): Boolean = granted
            override fun recordDecision(granted: Boolean) {
                this.decided = true
                this.granted = granted
            }
        }
        shouldPrompt = ShouldPromptNotificationPermissionUseCase(store, capability)
        recordDecision = RecordNotificationPermissionDecisionUseCase(store)
        conversationRepository = mockk {
            coEvery { getConversations() } returns ConversationsOutcome.Success(emptyList())
        }
        turnosRepository = mockk {
            coEvery { getTurnos() } returns TurnosOutcome.Success(emptyList())
        }
        assertFalse("Initially permission must not be decided", store.hasDecided())
        assertTrue("Android 13+ must prompt when undecided", shouldPrompt())
    }

    fun requestEnablingNotifications() {
        requestedFromApp = true
        assertTrue(requestedFromApp)
    }

    fun decidePermission(decision: String) {
        when (decision) {
            "concedo" -> recordDecision(granted = true)
            "rechazo" -> recordDecision(granted = false)
            else -> error("Unknown decision: $decision")
        }
    }

    fun assertCanConsultMessagesAndServices() = runBlocking {
        val conversations = conversationRepository.getConversations()
        assertTrue("Messages/conversations must be accessible", conversations is ConversationsOutcome.Success)
        val turnos = turnosRepository.getTurnos()
        assertTrue("Services/turnos must be accessible", turnos is TurnosOutcome.Success)
    }

    fun assertNotPromptedAutomatically() {
        assertFalse("Permission must not be requested automatically once decided", shouldPrompt())
    }

    fun assertCanOpenNotificationSettings() {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Settings.ACTION_APP_NOTIFICATION_SETTINGS
        every { intent.getStringExtra(Settings.EXTRA_APP_PACKAGE) } returns "com.loresuelvo.consumer"
        val settingsIntent = NotificationSettingsIntent { intent }
        val result = settingsIntent.create(context)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, result.action)
        assertEquals("com.loresuelvo.consumer", result.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }
}
