package com.loresuelvo.consumer.domain.workorder

/**
 * Provider-submitted report that wraps up a finished visit.
 * Carried on a [WorkOrderDetail] when the work-order is in
 * `awaiting_payment` or `paid` (the completion report is filed
 * before the consumer clears the remaining balance). Mirrors
 * the `completion_report` block of the
 * `GET /work-orders/{workOrderID}` wire response.
 *
 * Pure domain type: camelCase, no framework dependencies, no
 * JSON. The image list is denormalised (a separate
 * [CompletionReportPhoto] per file) so the screen can render
 * each thumbnail via Coil without re-parsing the JSON.
 *
 * @property reportedOnEpochMillis the wall-clock instant the
 *   provider filed the report (the wire ships an ISO-8601
 *   string with a trailing `Z`; the mapper decodes via
 *   `data/api/mapper/IsoTimestamp.kt`).
 */
data class CompletionReport(
    val id: String,
    val description: String,
    val reportedOnEpochMillis: Long,
    val images: List<CompletionReportPhoto>,
)
