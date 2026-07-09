package com.loresuelvo.consumer.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [EncryptedAuthSessionStore]. Migrated from the
 * `androidTest/integration/auth/` source set in Fase 8 of the master
 * plan: the test now runs under Robolectric (no emulator), pinning
 * `sdk = [34]`.
 *
 * The test passes a plain (non-encrypted) [android.content.SharedPreferences]
 * to the store. Robolectric 4.13 does NOT provide `AndroidKeyStore`,
 * which `EncryptedSharedPreferences` requires at construction time,
 * so the encryption-via-`createEncryptedSessionPrefs` helper is
 * exercised separately through `androidTest/integration/auth/` in CI.
 * The contract this unit test asserts is the persistence / restore /
 * clear behaviour plus the in-memory `StateFlow<AuthSession?>`
 * semantics that [com.loresuelvo.consumer.ui.session.SessionViewModel]
 * observes at runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EncryptedAuthSessionStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        clearSessionPreferences()
    }

    @After
    fun tearDown() {
        clearSessionPreferences()
    }

    @Test
    fun saves_and_restores_authenticated_session() {
        val store = EncryptedAuthSessionStore(plainSharedPrefs())

        store.saveSession(
            session = AuthSession(
                user = User(
                    displayName = "Andres",
                    firstName = "Andres",
                    lastName = "Colina",
                    email = "andy@pro.com"
                ),
                accessToken = "fake-token"
            )
        )

        val restored = store.getSession()
        assertNotNull(restored)
        assertEquals("Andres", restored?.user?.displayName)
        assertEquals("Andres", restored?.user?.firstName)
        assertEquals("Colina", restored?.user?.lastName)
        assertEquals("andy@pro.com", restored?.user?.email)
        assertEquals("fake-token", restored?.accessToken)
    }

    @Test
    fun clears_authenticated_session() {
        val store = EncryptedAuthSessionStore(plainSharedPrefs())
        store.saveSession(
            AuthSession(
                user = User(
                    displayName = "Andres",
                    firstName = "Andres",
                    lastName = "Colina",
                    email = "andy@pro.com"
                ),
                accessToken = "fake-token"
            )
        )

        store.clearSession()

        assertNull(store.getSession())
    }

    @Test
    fun second_store_sees_the_session_written_by_the_first() {
        // Persistence contract: a fresh store instance, reading from
        // the same SharedPreferences file, sees the session written
        // by a sibling store. This is what `LoResuelvoNav` relies on
        // across process restarts.
        val fixedPrefs = context.getSharedPreferences(
            "test_shared_prefs",
            Context.MODE_PRIVATE,
        )
        clearPrefs(fixedPrefs)

        val writer = EncryptedAuthSessionStore(fixedPrefs)
        writer.saveSession(
            AuthSession(
                user = User(
                    displayName = "Andres",
                    firstName = "Andres",
                    lastName = "Colina",
                    email = "andy@pro.com"
                ),
                accessToken = "fake-token"
            )
        )

        val reader = EncryptedAuthSessionStore(fixedPrefs)

        assertEquals("Andres", reader.getSession()?.user?.displayName)
    }

    private fun plainSharedPrefs() =
        context.getSharedPreferences(
            "test_session_prefs_${System.nanoTime()}",
            Context.MODE_PRIVATE,
        )

    private fun clearPrefs(prefs: android.content.SharedPreferences) {
        prefs.edit().clear().commit()
    }

    private fun clearSessionPreferences() {
        // Each test mints a unique prefs file (see `plainSharedPrefs()`)
        // so leftover state from a previous test cannot influence the
        // next. The cross-store test uses `test_shared_prefs` and
        // clears it explicitly in the test body.
    }
}
