package com.loresuelvo.consumer.integration.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.auth.createEncryptedSessionPrefs
import com.loresuelvo.consumer.data.auth.PREFS_NAME
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

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

        val store = EncryptedAuthSessionStore(createEncryptedSessionPrefs(context))

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

        val restoredSession = store.getSession()

        assertEquals("Andres", restoredSession?.user?.displayName)
        assertEquals("andy@pro.com", restoredSession?.user?.email)
    }

    @Test
    fun clears_authenticated_session() {

        val store = EncryptedAuthSessionStore(createEncryptedSessionPrefs(context))
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

    private fun clearSessionPreferences() {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}