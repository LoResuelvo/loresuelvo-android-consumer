package com.loresuelvo.consumer.domain.workorder

/**
 * Consumer-submitted review tied to a `paid` [WorkOrderDetail].
 * Mirrors the `review` block of the
 * `GET /work-orders/{workOrderID}` wire response (the block is
 * only emitted after the consumer has filed a review).
 *
 * Pure domain type: camelCase, no framework dependencies, no
 * JSON.
 *
 * @property rating integer in `1..5`. Out-of-range values come
 *   from a misbehaving backend — the screen renders whatever it
 *   receives; validation lives in the wire layer.
 */
data class WorkOrderReview(
    val rating: Int,
    val description: String,
)
