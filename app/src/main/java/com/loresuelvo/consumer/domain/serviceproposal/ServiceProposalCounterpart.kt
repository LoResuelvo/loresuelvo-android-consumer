package com.loresuelvo.consumer.domain.serviceproposal

/**
 * Snapshot of the counterpart (the provider, from the consumer's
 * point of view) attached to a service proposal. The backend
 * denormalises a small profile slice into `GET /service-proposals`
 * so the Home / Mis Servicios / Detalle surfaces can render the
 * card without a second round-trip to the providers endpoint.
 *
 * This is **not** the same type as
 * [com.loresuelvo.consumer.domain.provider.Provider]: that one
 * carries `categoryId` as a non-null `Int` and is shaped for the
 * search-by-category screen. The proposal counterpart is a
 * minimal projection — only what a single proposal card needs.
 *
 * `role` from the wire payload (`"consumer"` / `"provider"`) is
 * intentionally not exposed: the Home / Mis Servicios always
 * render the proposal from the consumer's perspective, where the
 * counterpart is always the provider. The mapper ignores that
 * field so a backend typo or future schema drift does not leak
 * into the domain.
 */
data class ServiceProposalCounterpart(
    val id: String,
    val name: String,
    val surname: String,
    val categoryName: String,
    val profilePhotoUrl: String?,
)
