package com.loresuelvo.consumer.data.installation

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import java.io.IOException
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EncryptedInstallationStateStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun prefs() = context.getSharedPreferences(UUID.randomUUID().toString(), Context.MODE_PRIVATE)

    @Test fun pending_attempt_reuses_identity_and_binding_after_restart() {
        val preferences = prefs()
        val first = EncryptedInstallationStateStore(preferences).prepare(17, "attempt")
        val retry = EncryptedInstallationStateStore(preferences).prepare(17, "attempt")
        assertEquals(first, retry)
        for (id in listOf(first.identity.id, first.identity.secret, first.id)) {
            assertEquals(4, UUID.fromString(id).version())
            assertEquals(id, UUID.fromString(id).toString())
        }
        assertFalse(retry.confirmed)
    }

    @Test fun retry_after_process_recreation_keeps_pending_binding_when_attempt_changes() {
        val preferences = prefs()
        val first = EncryptedInstallationStateStore(preferences).prepare(17, "first-process")

        val retry = EncryptedInstallationStateStore(preferences).prepare(17, "restored-session")

        assertEquals(first.identity, retry.identity)
        assertEquals(first.id, retry.id)
        assertEquals(first.previousId, retry.previousId)
        assertFalse(retry.confirmed)
    }

    @Test fun restoring_confirmed_session_keeps_its_binding_when_attempt_changes() {
        val preferences = prefs()
        val store = EncryptedInstallationStateStore(preferences)
        val confirmed = store.prepare(17, "first-login")
        store.confirm(confirmed)

        val restored = EncryptedInstallationStateStore(preferences).prepare(17, "restored-session")

        assertEquals(confirmed.identity, restored.identity)
        assertEquals(confirmed.id, restored.id)
        assertTrue(restored.confirmed)
    }

    @Test fun confirmed_binding_and_installation_survive_auth_logout_but_new_login_gets_new_binding() {
        val preferences = prefs()
        val store = EncryptedInstallationStateStore(preferences)
        val first = store.prepare(17, "first-login")
        store.confirm(first)
        assertTrue(EncryptedInstallationStateStore(preferences).prepare(17, "first-login").confirmed)
        EncryptedAuthSessionStore(prefs()).clearSession()
        val next = EncryptedInstallationStateStore(preferences).prepare(29, "second-login", newAuthentication = true)
        assertEquals(first.identity, next.identity)
        assertEquals(first.id, next.previousId)
        assertFalse(first.id == next.id)
        assertFalse(next.confirmed)
    }

    @Test fun explicit_new_authentication_creates_a_fresh_binding_for_the_same_account() {
        val preferences = prefs()
        val store = EncryptedInstallationStateStore(preferences)
        val first = store.prepare(17, "first-login")
        store.confirm(first)

        val next = EncryptedInstallationStateStore(preferences).prepare(
            17,
            "second-login",
            newAuthentication = true,
        )

        assertEquals(first.identity, next.identity)
        assertEquals(first.id, next.previousId)
        assertFalse(first.id == next.id)
        assertFalse(next.confirmed)
    }

    @Test fun logout_persists_pending_removal_and_immediately_revokes_local_binding() {
        val preferences = prefs()
        val store = EncryptedInstallationStateStore(preferences)
        val binding = store.prepare(17, "login")
        store.confirm(binding)

        val pending = store.beginRemoval(17)
        val restoredStore = EncryptedInstallationStateStore(preferences)

        assertNotNull(pending)
        assertEquals(binding.identity, pending?.identity)
        assertEquals(binding.id, pending?.bindingId)
        assertNull(store.confirmedInstallation())
        assertEquals(pending, restoredStore.pendingRemoval())
        assertNull(restoredStore.confirmedInstallation())
        assertFalse(preferences.contains("access_token"))
    }

    @Test fun completion_of_old_removal_cannot_clear_a_new_binding() {
        val store = EncryptedInstallationStateStore(prefs())
        val old = store.prepare(17, "old-login")
        store.confirm(old)
        assertEquals(old.id, store.beginRemoval(17)?.bindingId)

        val fresh = store.prepare(17, "new-login", newAuthentication = true)
        assertTrue(store.completeRemoval(old.id))
        store.confirm(fresh)

        assertNull(store.pendingRemoval())
        assertEquals(fresh.id, store.confirmedInstallation()?.bindingId)
    }

    @Test(expected = IOException::class)
    fun partial_identity_is_rejected_without_resetting_it() {
        val preferences = prefs()
        val id = UUID.randomUUID().toString()
        preferences.edit().putString("installation_id", id).commit()
        try { EncryptedInstallationStateStore(preferences).prepare(17, "attempt") }
        finally { assertEquals(id, preferences.getString("installation_id", null)) }
    }
}
