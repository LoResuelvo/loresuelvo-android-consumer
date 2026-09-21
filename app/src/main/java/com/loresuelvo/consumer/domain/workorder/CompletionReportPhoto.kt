package com.loresuelvo.consumer.domain.workorder

/**
 * Single photograph embedded in a [CompletionReport]. Mirrors
 * one element of the `completion_report.images[]` array in the
 * `GET /work-orders/{workOrderID}` wire response.
 *
 * Pure domain type: camelCase, no framework dependencies, no
 * JSON. The [url] is a temporary signed URL the backend emits
 * when the work order is fetched — `fileId` is the stable
 * identifier so the domain can refresh the URL on demand in a
 * future iteration without losing the photo identity.
 *
 * @property url signed URL valid for the current consumer
 *   session; the image viewer uses it directly via Coil.
 */
data class CompletionReportPhoto(
    val fileId: String,
    val originalName: String,
    val url: String,
)
