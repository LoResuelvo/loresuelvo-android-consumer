package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStatus
import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStore
import com.loresuelvo.consumer.domain.notifications.NotificationPlatformCapability
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationPermissionUseCasesTest {
    private val store = mockk<NotificationPermissionStore>(relaxed = true)
    private val platform = mockk<NotificationPlatformCapability>(relaxed = true)

    private lateinit var shouldPrompt: ShouldPromptNotificationPermissionUseCase
    private lateinit var recordDecision: RecordNotificationPermissionDecisionUseCase
    private lateinit var getStatus: GetNotificationPermissionStatusUseCase

    @Before
    fun setUp() {
        shouldPrompt = ShouldPromptNotificationPermissionUseCase(store, platform)
        recordDecision = RecordNotificationPermissionDecisionUseCase(store)
        getStatus = GetNotificationPermissionStatusUseCase(store, platform)
    }

    @Test
    fun should_not_prompt_when_runtime_permission_not_required() {
        every { platform.requiresRuntimeNotificationPermission() } returns false
        every { store.hasDecided() } returns false

        assertFalse(shouldPrompt())
    }

    @Test
    fun should_not_prompt_when_already_decided() {
        every { platform.requiresRuntimeNotificationPermission() } returns true
        every { store.hasDecided() } returns true

        assertFalse(shouldPrompt())
    }

    @Test
    fun should_prompt_when_runtime_permission_required_and_undecided() {
        every { platform.requiresRuntimeNotificationPermission() } returns true
        every { store.hasDecided() } returns false

        assertTrue(shouldPrompt())
    }

    @Test
    fun record_decision_delegates_to_store() {
        recordDecision(granted = true)
        verify { store.recordDecision(true) }

        recordDecision(granted = false)
        verify { store.recordDecision(false) }
    }

    @Test
    fun get_status_returns_granted_when_notifications_enabled() {
        every { platform.areNotificationsEnabled() } returns true

        assertEquals(NotificationPermissionStatus.GRANTED, getStatus())
    }

    @Test
    fun get_status_returns_denied_when_disabled_and_decided() {
        every { platform.areNotificationsEnabled() } returns false
        every { store.hasDecided() } returns true

        assertEquals(NotificationPermissionStatus.DENIED, getStatus())
    }

    @Test
    fun get_status_returns_undecided_on_android13_when_disabled_and_not_decided() {
        every { platform.areNotificationsEnabled() } returns false
        every { store.hasDecided() } returns false
        every { platform.requiresRuntimeNotificationPermission() } returns true

        assertEquals(NotificationPermissionStatus.UNDECIDED, getStatus())
    }

    @Test
    fun get_status_returns_denied_on_pre_android13_when_disabled_by_user() {
        every { platform.areNotificationsEnabled() } returns false
        every { store.hasDecided() } returns false
        every { platform.requiresRuntimeNotificationPermission() } returns false

        assertEquals(NotificationPermissionStatus.DENIED, getStatus())
    }
}
