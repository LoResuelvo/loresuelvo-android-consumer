package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.SubmitReviewRequestDto
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            when (val error = e.toApiError()) {
                is ApiError.Server -> when (error.code) {
                    404 -> GetWorkOrderOutcome.NotFound
                    403 -> GetWorkOrderOutcome.Failure(
                        ServiceProposalsOutcome.Failure.AccessDenied,
                    )
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
    //  Never throws for API or network failures.
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
    } catch (cancelled: CancellationException) {
        throw cancelled
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
