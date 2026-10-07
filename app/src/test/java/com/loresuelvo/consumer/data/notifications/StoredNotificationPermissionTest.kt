package com.loresuelvo.consumer.data.notifications

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StoredNotificationPermissionTest {
    private val preferences = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val values = mutableMapOf<String, Any?>()
    private lateinit var store: StoredNotificationPermission

    @Before
    fun setUp() {
        values.clear()
        every { preferences.getBoolean(any(), any()) } answers {
            values[firstArg<String>()] as? Boolean ?: secondArg()
        }
        every { preferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } answers {
            values[firstArg<String>()] = secondArg<Boolean>()
            editor
        }
        every { editor.commit() } returns true
        store = StoredNotificationPermission(preferences)
    }

    @Test
    fun initially_undecided_and_not_granted() {
        assertFalse(store.hasDecided())
        assertFalse(store.isPermissionGranted())
    }

    @Test
    fun records_granted_decision() {
        store.recordDecision(granted = true)

        assertTrue(store.hasDecided())
        assertTrue(store.isPermissionGranted())
        verify { editor.putBoolean("notification_permission_decided", true) }
        verify { editor.putBoolean("notification_permission_granted", true) }
        verify { editor.commit() }
    }

    @Test
    fun records_denied_decision() {
        store.recordDecision(granted = false)

        assertTrue(store.hasDecided())
        assertFalse(store.isPermissionGranted())
        verify { editor.putBoolean("notification_permission_decided", true) }
        verify { editor.putBoolean("notification_permission_granted", false) }
        verify { editor.commit() }
    }
}
