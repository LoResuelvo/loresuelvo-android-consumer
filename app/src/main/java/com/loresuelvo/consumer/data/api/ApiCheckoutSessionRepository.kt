package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.CheckoutSessionDto
import com.loresuelvo.consumer.data.api.mapper.parseIsoTimestampMillisOrZero
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter for [CheckoutSessionRepository]. Translates the
 * `POST /service-proposals/{id}/checkout-sessions` wire call
 * into the domain [CheckoutSessionOutcome] hierarchy.
 *
 * Transport errors never throw: every [IOException] is wrapped
 * as [CheckoutSessionOutcome.Network] and every non-2xx response
 * is decoded as [CheckoutSessionOutcome.Server]. A `409 Conflict`
 * is mapped to [CheckoutSessionOutcome.AlreadyPaid] so the UI
 * can show a specific "este acuerdo ya fue confirmado" copy
 * without guessing.
 */
@Singleton
class ApiCheckoutSessionRepository @Inject constructor(
    private val backendApi: BackendApi,
) : CheckoutSessionRepository {

    override suspend fun startServiceProposalCheckout(
        serviceProposalId: Int,
    ): CheckoutSessionOutcome =
        try {
            val response: CheckoutSessionDto = backendApi.startServiceProposalCheckout(serviceProposalId)
            val pricing = response.pricing.toDomain()
            val intent = response.toDomain(serviceProposalId)
            CheckoutSessionOutcome.Created(intent = intent, pricing = pricing)
        } catch (e: HttpException) {
            when (e.code()) {
                409 -> CheckoutSessionOutcome.AlreadyPaid(
                    message = decodeMessage(e) ?: "Este acuerdo ya fue confirmado.",
                )
                else -> CheckoutSessionOutcome.Server(
                    code = e.code(),
                    message = decodeMessage(e) ?: "Could not start checkout",
                )
            }
        } catch (e: IOException) {
            CheckoutSessionOutcome.Network(e)
        } catch (e: Throwable) {
            CheckoutSessionOutcome.Server(
                code = 0,
                message = e.message ?: "Unknown error",
            )
        }

    private fun decodeMessage(e: HttpException): String? = try {
        e.response()?.errorBody()?.string()
    } catch (_: Throwable) {
        null
    }.takeIf { !it.isNullOrBlank() }
}
