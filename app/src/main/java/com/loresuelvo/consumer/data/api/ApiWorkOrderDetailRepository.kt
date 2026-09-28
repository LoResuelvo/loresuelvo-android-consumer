package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.SubmitReviewRequestDto
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter that fulfils [WorkOrderDetailRepository] against the
 * dedicated `GET /work-orders/{workOrderID}` endpoint (US-27
 * `visualize-turns-detail`).
 *
 * The legacy adapter reused the proposal list endpoint to avoid
 * a second round trip; the dedicated endpoint now exists so this
 * adapter swaps the call. The [GetWorkOrderOutcome] surface stays
 * identical so the screen / use case / VM are untouched.
 *
 * Error mapping:
 *  - `404 Not Found` → [GetWorkOrderOutcome.NotFound] (the screen
 *    renders its not-found copy; same UX as the legacy
 *    `Success(emptyList())` outcome).
 *  - Other non-2xx → [GetWorkOrderOutcome.Failure.Server] so the
 *    screen renders its retry CTA.
 *  - `IOException` (no network, timeout, DNS failure) →
 *    [GetWorkOrderOutcome.Failure.Network] via
 *    [toApiError].
 *  - `401 Unauthorized` (the JWT expired) → mapped through the
 *    same path as any other `Server(code = 401, message = ...)`;
 *    the session layer observes the auth event out of band.
 *
 * Implementations never throw.
 */
@Singleton
class ApiWorkOrderDetailRepository @Inject constructor(
    private val backendApi: BackendApi,
) : WorkOrderDetailRepository {

    override suspend fun getWorkOrderDetail(
        workOrderId: String,
        provider: WorkOrderDetailCounterpart?,
    ): GetWorkOrderOutcome =
        try {
            val dto = backendApi.getWorkOrder(workOrderId)
            val detail = dto.toDomain(provider)
            if (detail == null) {
                // The mapper returns null for unknown statuses;
                // today that's a contract violation on the
                // backend, but we surface it as a server failure
                // rather than a not-found so the screen offers
                // the retry CTA.
                GetWorkOrderOutcome.Failure(
                    ServiceProposalsOutcome.Failure.Server(
                        code = 0,
                        message = "Unknown work-order status",
                    ),
                )
            } else {
                GetWorkOrderOutcome.Found(detail)
            }
        } catch (e: Throwable) {
            when (val error = e.toApiError()) {
                is ApiError.Server -> when (error.code) {
                    404 -> GetWorkOrderOutcome.NotFound
                    else -> GetWorkOrderOutcome.Failure(
                        ServiceProposalsOutcome.Failure.Server(
                            code = error.code,
                            message = error.errorMessage,
                        ),
                    )
                }
                is ApiError.Network ->
                    GetWorkOrderOutcome.Failure(
                        ServiceProposalsOutcome.Failure.Network(error.networkCause),
                    )
                is ApiError.Unauthorized ->
                    GetWorkOrderOutcome.Failure(
                        ServiceProposalsOutcome.Failure.Server(
                            code = 401,
                            message = error.errorMessage,
                        ),
                    )
                is ApiError.Unknown ->
                    GetWorkOrderOutcome.Failure(
                        ServiceProposalsOutcome.Failure.Server(
                            code = 0,
                            message = error.message ?: "Unknown error",
                        ),
                    )
            }
        }

    // Wires `POST /work-orders/{workOrderID}/review` (US-30
    //  `calify-provider-service`). The 2xx response carries the
    //  freshly stored [com.loresuelvo.consumer.data.api.dto.ReviewDto],
    //  which mirrors the `review` block of `GET /work-orders/{id}`.
    //  The VM merges it into the cached `WorkOrderDetail` so the
    //  composer hands off to the read-only [ReviewSection].
    //
    //  Error mapping mirrors `getWorkOrderDetail`:
    //   - `409 Conflict` ("already reviewed") → [AlreadyReviewed]
    //     so the UI can swap directly to the read-only surface
    //     without branching on a magic HTTP code.
    //   - other non-2xx → [Server] (the screen surfaces the
    //     server-error copy).
    //   - `IOException` → [Network] via [toApiError].
    //   - `401 Unauthorized` → [Server(code = 401, …)]; the
    //     session layer observes the auth event out of band.
    //
    //  Never throws.
    override suspend fun submitReview(
        workOrderId: String,
        rating: Int,
        description: String,
    ): SubmitWorkOrderReviewOutcome = try {
        val dto = backendApi.submitWorkOrderReview(
            workOrderID = workOrderId,
            body = SubmitReviewRequestDto(
                rating = rating,
                description = description,
            ),
        )
        SubmitWorkOrderReviewOutcome.Submitted(dto.toDomain())
    } catch (e: Throwable) {
        when (val error = e.toApiError()) {
            is ApiError.Server -> when (error.code) {
                409 ->
                    SubmitWorkOrderReviewOutcome.AlreadyReviewed(error.errorMessage)
                else ->
                    SubmitWorkOrderReviewOutcome.Server(
                        code = error.code,
                        message = error.errorMessage,
                    )
            }
            is ApiError.Network ->
                SubmitWorkOrderReviewOutcome.Network(error.networkCause)
            is ApiError.Unauthorized ->
                SubmitWorkOrderReviewOutcome.Server(
                    code = 401,
                    message = error.errorMessage,
                )
            is ApiError.Unknown ->
                SubmitWorkOrderReviewOutcome.Server(
                    code = 0,
                    message = error.message ?: "Unknown error",
                )
        }
    }
}
