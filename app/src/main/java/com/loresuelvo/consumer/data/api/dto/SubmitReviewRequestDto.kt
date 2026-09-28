package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /work-orders/{workOrderID}/review`
 * (US-30 `calify-provider-service`).
 *
 * Wire contract:
 *  - `rating` is required and lives in `1..5`. The composer in
 *    `ui/screens/workorderdetail/ReviewComposerSection` enforces
 *    the range before the request reaches the wire layer; the
 *    backend re-validates and rejects out-of-range ratings with
 *    a `400 Bad Request` (mapped through the standard
 *    [com.loresuelvo.consumer.data.api.ApiErrorMapping]).
 *  - `description` is optional and may be blank (the consumer
 *    can rate without commenting). The backend caps length at
 *    500 characters; exceeding it yields a `400 Bad Request`.
 *
 * The endpoint mirrors the `review` block of
 * `GET /work-orders/{workOrderID}` on the response side (see
 * [ReviewDto]): the server echoes back the freshly stored
 * [ReviewDto] so the client can merge into the cached
 * `WorkOrderDetail`.
 */
@Serializable
data class SubmitReviewRequestDto(
    @SerialName("rating") val rating: Int,
    @SerialName("description") val description: String,
)
