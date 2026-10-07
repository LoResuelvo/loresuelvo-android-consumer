package com.loresuelvo.consumer.data.installation

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import java.io.IOException
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun confirmed_binding_and_installation_survive_auth_logout_but_new_login_gets_new_binding() {
        val preferences = prefs()
        val store = EncryptedInstallationStateStore(preferences)
        val first = store.prepare(17, "first-login")
        store.confirm(first)
        assertTrue(EncryptedInstallationStateStore(preferences).prepare(17, "first-login").confirmed)
        EncryptedAuthSessionStore(prefs()).clearSession()
        val next = EncryptedInstallationStateStore(preferences).prepare(29, "second-login")
        assertEquals(first.identity, next.identity)
        assertEquals(first.id, next.previousId)
        assertFalse(first.id == next.id)
        assertFalse(next.confirmed)
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
