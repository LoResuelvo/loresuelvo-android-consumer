package com.loresuelvo.consumer.platform.notifications

import android.content.Context
import com.loresuelvo.consumer.domain.installation.PushTokenOutcome
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseRegistrationTokenProviderTest {
    @Test fun missing_configuration_does_not_initialize_firebase_or_supply_a_fake_token() = runBlocking {
        val provider = FirebaseRegistrationTokenProvider(mockk<Context>(), FirebaseRegistrationConfiguration("", "", "", ""))
        assertEquals(PushTokenOutcome.ConfigurationUnavailable, provider.token())
    }
}
