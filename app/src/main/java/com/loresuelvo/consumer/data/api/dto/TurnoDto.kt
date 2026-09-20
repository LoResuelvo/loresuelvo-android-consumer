package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for a single element of `GET /work-orders`
 * (visualize-turns.feature scenario 02-VT).
 *
 * Wire shape (validated 2026-09 against the dev backend):
 *
 * ```json
 * {
 *   "id": 42,
 *   "service_proposal_id": 10,
 *   "amount_cents": 1500050,
 *   "scheduled_on": "2026-07-05T12:30:00Z",
 *   "description": "Reparación de pérdida de agua...",
 *   "status": "scheduled",
 *   "counterpart": { "id": 2, "role": "provider", "name": "Juan",
 *     "surname": "Gómez", "category_name": "Plomería",
 *     "profile_photo_url": "https://..." }
 * }
 * ```
 *
 * Field-name mapping rules:
 * - `id`, `service_proposal_id`, `counterpart.id` are `Long` on
 *   the wire and become `String` in the domain (stable
 *   `LazyColumn` keys, no overflow concerns).
 * - `scheduled_on` arrives as an ISO-8601 string with a
 *   trailing `Z`; the mapper parses it via
 *   `data/api/mapper/IsoTimestamp.kt`.
 * - `amount_cents` stays `Long` because it is a value, not an
 *   identifier.
 * - `counterpart.role` (always `"provider"`) is decoded but
 *   intentionally ignored by the mapper.
 *
 * The endpoint requires a valid Auth0 JWT (the `AuthInterceptor`
 * injects the bearer token automatically).
 */
@Serializable
data class TurnoDto(
    @SerialName("id") val id: Long,
    @SerialName("service_proposal_id") val serviceProposalId: Long,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("scheduled_on") val scheduledOn: String,
    @SerialName("description") val description: String,
    @SerialName("status") val status: String,
    @SerialName("counterpart") val counterpart: TurnoCounterpartDto,
)

@Serializable
data class TurnoCounterpartDto(
    @SerialName("id") val id: Long,
    @SerialName("role") val role: String,
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
)
