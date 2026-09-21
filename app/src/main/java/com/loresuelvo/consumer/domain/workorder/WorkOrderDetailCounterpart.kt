package com.loresuelvo.consumer.domain.workorder

/**
 * Denormalised counterpart carried on a
 * [WorkOrderDetail] — the provider whose service the consumer
 * booked. Mirrors the shape the backend emits in the
 * `GET /work-orders/{workOrderID}` response so the domain
 * never has to flatten the name back from a first / last pair.
 *
 * Pure domain type: camelCase, no framework dependencies, no
 * JSON. Sourced from the dedicated work-order endpoint
 * (US-27 `visualize-turns-detail`); the same fields already
 * exist on `TurnoCounterpart` for the list endpoint.
 *
 * @property profilePhotoUrl nullable because the provider might
 *   not have uploaded one yet.
 */
data class WorkOrderDetailCounterpart(
    val id: String,
    val name: String,
    val surname: String,
    val categoryName: String,
    val profilePhotoUrl: String?,
)
