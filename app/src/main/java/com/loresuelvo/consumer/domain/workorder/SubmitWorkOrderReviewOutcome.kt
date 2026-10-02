package com.loresuelvo.consumer.domain.workorder

sealed interface SubmitWorkOrderReviewOutcome {
    data class Submitted(val review: WorkOrderReview) : SubmitWorkOrderReviewOutcome
    data class Network(val cause: Throwable) : SubmitWorkOrderReviewOutcome
    data class Server(val code: Int, val message: String) : SubmitWorkOrderReviewOutcome
    data class AlreadyReviewed(val message: String) : SubmitWorkOrderReviewOutcome
}
