package com.loresuelvo.consumer.integration.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.data.auth.SharedPreferencesAuthSessionStore
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SharedPreferencesAuthSessionStoreTest {

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

        val store = SharedPreferencesAuthSessionStore(context)

        store.saveSession(
            AuthSession(
                user = User(
                    displayName = "Andres",
                    email = "andres@example.com"
                )
            )
        )

        val restoredSession = store.getSession()

        assertEquals("Andres", restoredSession?.user?.displayName)
        assertEquals("andres@example.com", restoredSession?.user?.email)
    }

    @Test
    fun clears_authenticated_session() {

        val store = SharedPreferencesAuthSessionStore(context)
        store.saveSession(
            AuthSession(
                user = User(displayName = "Andres")
            )
        )

        store.clearSession()

        assertNull(store.getSession())
    }

    private fun clearSessionPreferences() {
        context
            .getSharedPreferences("auth_session", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
