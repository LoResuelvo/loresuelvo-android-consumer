package com.loresuelvo.consumer.domain.serviceproposal

/**
 * Outcome of fetching the consumer's service proposals through
 * the [ServiceProposalRepository] port. Sealed so callers
 * explicitly handle the happy path and every failure branch —
 * mirrors [com.loresuelvo.consumer.domain.category.CategoriesOutcome]
 * and [com.loresuelvo.consumer.domain.provider.ProvidersOutcome].
 */
sealed interface ServiceProposalsOutcome {

    data class Success(val proposals: List<ServiceProposal>) : ServiceProposalsOutcome

    sealed interface Failure : ServiceProposalsOutcome {

        /** Transport-level failure: timeouts, DNS, connection refused. */
        data class Network(val cause: Throwable) : Failure

        /**
         * Any non-2xx response. [code] is the HTTP status, [message]
         * the human-readable text extracted from the error body.
         */
        data class Server(val code: Int, val message: String) : Failure
    }
}
