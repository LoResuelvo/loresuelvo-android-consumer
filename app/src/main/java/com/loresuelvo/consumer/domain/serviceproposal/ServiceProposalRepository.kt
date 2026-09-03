package com.loresuelvo.consumer.domain.serviceproposal

/**
 * Port for reading the consumer's service proposals. Implemented
 * in `data/api/ApiServiceProposalRepository.kt` against the
 * backend's authenticated `GET /service-proposals` endpoint.
 *
 * No Android, no Retrofit, no JSON — just the contract the use
 * cases depend on. Implementations must not throw on HTTP /
 * network failures; they translate to a
 * [ServiceProposalsOutcome.Failure].
 *
 * The endpoint returns every proposal regardless of status; the
 * Home / Mis Servicios use cases are responsible for filtering by
 * [ServiceProposalStatus]. That keeps the wire contract narrow
 * (one endpoint, no query params) at the cost of a slightly
 * bigger list crossing the repository boundary — acceptable
 * because the consumer typically has tens of proposals, not
 * thousands.
 */
interface ServiceProposalRepository {

    suspend fun getServiceProposals(): ServiceProposalsOutcome
}
