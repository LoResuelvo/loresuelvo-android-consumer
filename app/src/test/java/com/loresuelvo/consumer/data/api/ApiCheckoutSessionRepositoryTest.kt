package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ApiCheckoutSessionRepositoryTest {
    @Test
    fun conflicts_only_mean_paid_when_backend_confirms_full_payment() = runTest {
        val api = mockk<BackendApi>()
        val repository = ApiCheckoutSessionRepository(api)
        for (message in listOf(
            "Booking payment deadline has been reached",
            "Only pending service proposals can start checkout",
            "Payment account connection not found",
        )) {
            val error = conflict(message)
            coEvery { api.startServiceProposalCheckout(1) } throws error
            assertEquals(
                CheckoutSessionOutcome.Server(409, message),
                repository.startServiceProposalCheckout(1),
            )
        }
        coEvery { api.startWorkOrderCheckout(1) } throws conflict("Work order is already fully paid")
        assertTrue(repository.startWorkOrderCheckout(1) is CheckoutSessionOutcome.AlreadyPaid)
        coEvery { api.startWorkOrderCheckout(1) } throws conflict("Only scheduled work orders can start balance checkout")
        assertTrue(repository.startWorkOrderCheckout(1) is CheckoutSessionOutcome.Server)
    }

    private fun conflict(message: String) = HttpException(
        Response.error<Unit>(409, """{"error":"$message"}""".toResponseBody("application/json".toMediaType())),
    )
}
