package com.loresuelvo.consumer.domain.workorder

/**
 * Outcome of submitting the consumer's review (rating + optional
 * comment) for a paid [WorkOrderDetail] via
 * `POST /work-orders/{workOrderID}/review` (US-30
 * `calify-provider-service`).
 *
 * Mirrors [com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome]:
 * a flat sealed hierarchy with typed variants so callers handle
 * each branch explicitly instead of pattern-matching on HTTP
 * status codes.
 *
 *  - [Submitted] — the backend accepted the review and echoed
 *    back the freshly stored [WorkOrderReview] (1:1 with the
 *    `review` block of `GET /work-orders/{workOrderID}`). The VM
 *    merges it into the cached [WorkOrderDetail] via
 *    `copy(review = outcome.review)` so the screen swaps the
 *    composer for the read-only Review section. Carrying just the
 *    sub-resource (instead of the full updated parent) keeps the
 *    wire contract small and avoids re-mapping the provider
 *    counterpart.
 *  - [Network] — transport-level failure (timeouts, DNS, TLS,
 *    connection refused). The composer surfaces the network copy
 *    with a retry CTA.
 *  - [Server] — any non-2xx HTTP response (e.g. 400 invalid
 *    rating, 500 unexpected error). The composer surfaces the
 *    server error copy.
 *  - [AlreadyReviewed] — `409 Conflict`. The backend already has
 *    a review on file for this work order; the VM treats this
 *    like [Submitted] (re-fetches / merges the latest review and
 *    renders the read-only Review section). Returned as a typed
 *    variant so the UI does not have to pattern-match on
 *    `Server(code = 409, …)`.
 */
sealed interface SubmitWorkOrderReviewOutcome {
    data class Submitted(val review: WorkOrderReview) : SubmitWorkOrderReviewOutcome
    data class Network(val cause: Throwable) : SubmitWorkOrderReviewOutcome
    data class Server(val code: Int, val message: String) : SubmitWorkOrderReviewOutcome
    data class AlreadyReviewed(val message: String) : SubmitWorkOrderReviewOutcome
}
