package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.CheckoutSessionDto
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

/**
 * Adapter for [CheckoutSessionRepository]. Translates the
 * `POST /service-proposals/{id}/checkout-sessions` wire call
 * into the domain [CheckoutSessionOutcome] hierarchy.
 *
 * Transport errors never throw: every [IOException] is wrapped
 * as [CheckoutSessionOutcome.Network] and every non-2xx response
 * is decoded as [CheckoutSessionOutcome.Server]. Only an explicit
 * already-paid response for a work order maps to [CheckoutSessionOutcome.AlreadyPaid].
 */
@Singleton
class ApiCheckoutSessionRepository @Inject constructor(
    private val backendApi: BackendApi,
) : CheckoutSessionRepository {

    override suspend fun startServiceProposalCheckout(
        serviceProposalId: Int,
    ): CheckoutSessionOutcome =
        runCheckout(backendApi::startServiceProposalCheckout, proposalId = serviceProposalId)

    override suspend fun startWorkOrderCheckout(
        workOrderId: Int,
    ): CheckoutSessionOutcome =
        runCheckout(backendApi::startWorkOrderCheckout, workOrderId = workOrderId)

    private suspend fun runCheckout(
        call: suspend (Int) -> CheckoutSessionDto,
        proposalId: Int? = null,
        workOrderId: Int? = null,
    ): CheckoutSessionOutcome =
        try {
            val response: CheckoutSessionDto = call(
                proposalId ?: workOrderId ?: error("proposalId or workOrderId required"),
            )
            val pricing = response.pricing.toDomain()
            // The intent's `serviceProposalId` field on the wire
            // is the source of truth; for the work-order flow the
            // backend reuses the originating proposal id, so we
            // pass `proposalId` when present and `0` otherwise. The
            // payNow call sites already key on the work-order id
            // (the route arg) so the integer in the intent is a
            // tiebreaker only — see [PaymentIntent].
            val intent = response.toDomain(
                serviceProposalId = proposalId ?: 0,
            )
            CheckoutSessionOutcome.Created(intent = intent, pricing = pricing)
        } catch (e: HttpException) {
            val error = e.toApiError()
            val message = (error as? ApiError.Server)?.errorMessage ?: "Could not start checkout"
            if (workOrderId != null && e.code() == HTTP_CONFLICT && message == WORK_ORDER_ALREADY_PAID_MESSAGE) {
                CheckoutSessionOutcome.AlreadyPaid(message)
            } else {
                CheckoutSessionOutcome.Server(code = e.code(), message = message)
            }
        } catch (e: IOException) {
            CheckoutSessionOutcome.Network(e)
        } catch (e: Throwable) {
            CheckoutSessionOutcome.Server(
                code = 0,
                message = e.message ?: "Unknown error",
            )
        }

    private companion object {
        const val HTTP_CONFLICT = 409
        const val WORK_ORDER_ALREADY_PAID_MESSAGE = "Work order is already fully paid"
    }
}
