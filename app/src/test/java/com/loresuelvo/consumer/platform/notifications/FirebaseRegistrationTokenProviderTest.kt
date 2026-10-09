package com.loresuelvo.consumer.platform.notifications

import android.content.Context
import com.google.firebase.FirebaseApp
import com.loresuelvo.consumer.domain.installation.PushTokenOutcome
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test

class FirebaseRegistrationTokenProviderTest {
    private val context = mockk<Context>()

    @Before fun setUp() {
        mockkStatic(FirebaseApp::class)
        every { FirebaseApp.getApps(context) } returns emptyList()
        every { FirebaseApp.initializeApp(context) } returns null
    }

    @After fun tearDown() = unmockkStatic(FirebaseApp::class)

    @Test fun missing_firebase_options_returns_configuration_unavailable() = runBlocking {
        val provider = FirebaseRegistrationTokenProvider(context)

        assertEquals(PushTokenOutcome.ConfigurationUnavailable, provider.token())
        verify(exactly = 1) { FirebaseApp.initializeApp(context) }
    }
}
