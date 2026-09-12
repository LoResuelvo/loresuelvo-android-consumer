package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.PaymentIntentDto
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.payment.GetPaymentIntentOutcome
import com.loresuelvo.consumer.domain.payment.PaymentIntentRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter for [PaymentIntentRepository]. Drives the
 * post-redirect polling loop (US-21 confirm-agreement flow +
 * US-28 service-balance flow). Returns the current
 * [com.loresuelvo.consumer.domain.payment.PaymentIntentStatus] so
 * the consumer can switch the UI between "pago aprobado",
 * "pago rechazado" and "pago en proceso" without trusting the
 * browser redirect.
 *
 * Idempotent: the endpoint is read-only and safe to call
 * repeatedly. A `404 Not Found` is mapped to
 * [GetPaymentIntentOutcome.NotFound] (e.g. the intent was
 * deleted by an admin) so the caller can stop polling without
 * crashing.
 */
@Singleton
class ApiPaymentIntentRepository @Inject constructor(
    private val backendApi: BackendApi,
) : PaymentIntentRepository {

    override suspend fun get(paymentIntentId: String): GetPaymentIntentOutcome =
        try {
            val dto: PaymentIntentDto = backendApi.getPaymentIntent(paymentIntentId)
            GetPaymentIntentOutcome.Found(dto.toDomain())
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> GetPaymentIntentOutcome.NotFound
                else -> GetPaymentIntentOutcome.Server(
                    code = e.code(),
                    message = e.message().orEmpty().ifBlank { "Could not get payment intent" },
                )
            }
        } catch (e: IOException) {
            GetPaymentIntentOutcome.Network(e)
        } catch (e: Throwable) {
            GetPaymentIntentOutcome.Server(
                code = 0,
                message = e.message ?: "Unknown error",
            )
        }
}
