package com.loresuelvo.consumer.platform.notifications

import android.app.Application
import android.content.Intent
import android.net.Uri
import com.loresuelvo.consumer.ui.navigation.paymentResultPathFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [24])
class NavigationIntentDispatcherTest {
    @Test fun payment_return_remains_buffered_until_navigation_is_ready() = runTest {
        val dispatcher = NavigationIntentDispatcher()
        val payment = Intent(Intent.ACTION_VIEW, Uri.parse("https://test.loresuelvo.com.ar/payments/success?external_reference=payment-42"))
        dispatcher.dispatch(payment)
        val delivered = dispatcher.events.first()
        assertEquals(payment.data, delivered.data)
        assertEquals("payment-result?external_reference=payment-42", paymentResultPathFor(delivered.data!!))
    }
}
