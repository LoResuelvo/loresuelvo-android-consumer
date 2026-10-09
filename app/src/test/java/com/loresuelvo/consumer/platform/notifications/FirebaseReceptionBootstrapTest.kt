package com.loresuelvo.consumer.platform.notifications

import android.content.Context
import com.google.firebase.FirebaseApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseReceptionBootstrapTest {
    private val context = mockk<Context>()

    @Before fun setUp() = mockkStatic(FirebaseApp::class)

    @After fun tearDown() = unmockkStatic(FirebaseApp::class)

    @Test fun missing_firebase_options_does_not_create_default_firebase_app() {
        every { FirebaseApp.getApps(context) } returns emptyList()
        every { FirebaseApp.initializeApp(context) } returns null
        val provider = FirebaseRegistrationTokenProvider(context)

        assertFalse(provider.initializeForReception())
        verify(exactly = 1) { FirebaseApp.initializeApp(context) }
    }

    @Test fun generated_firebase_options_initialize_the_default_app() {
        val configuredApp = mockk<FirebaseApp>()
        every { FirebaseApp.getApps(context) } returns emptyList()
        every { FirebaseApp.initializeApp(context) } returns configuredApp

        assertTrue(FirebaseRegistrationTokenProvider(context).initializeForReception())
        verify(exactly = 1) { FirebaseApp.initializeApp(context) }
    }

    @Test fun existing_default_firebase_app_is_reused() {
        val defaultApp = mockk<FirebaseApp>()
        every { defaultApp.name } returns FirebaseApp.DEFAULT_APP_NAME
        every { FirebaseApp.getApps(context) } returns listOf(defaultApp)

        assertTrue(FirebaseRegistrationTokenProvider(context).initializeForReception())
        verify(exactly = 0) { FirebaseApp.initializeApp(context) }
    }
}
