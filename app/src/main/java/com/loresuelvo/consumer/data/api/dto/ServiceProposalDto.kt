package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for a single element of `GET /service-proposals`.
 * The endpoint returns the consumer's full proposal list, ordered
 * by the backend's policy (most-recently-updated first is the
 * current contract). The shape below mirrors the swagger example
 * the user shared for US-54: every proposal carries its
 * counterpart snapshot, the agreed amount in cents, the
 * scheduled date, the textual reason for the visit, the lifecycle
 * status, and the originating conversation id.
 *
 * `booking_terms` is intentionally **not** modelled here: it
 * carries eleven platform-fee / deposit fields that are irrelevant
 * for the Home / Mis Servicios / Detalle surfaces of US-54 and
 * will be introduced together with the work-order detail screen
 * (scenario 16-VSP). With
 * `Json { ignoreUnknownKeys = true }` (see
 * `data/api/ApiErrorMapperDefaults`) the wire decoder drops the
 * block silently, so adding it later does not break older
 * clients.
 *
 * Field-name mapping rules:
 *
 * - `id`, `conversation_id`, `amount_cents`, `counterpart.id`
 *   are `Long` on the wire and become `String` in the domain
 *   (see [ServiceProposal]); `amount_cents` stays `Long` because
 *   it is a value, not an identifier.
 * - `scheduled_on` and `created_on` arrive as ISO-8601 strings
 *   with a trailing `Z`; the mapper parses them via
 *   `data/api/mapper/IsoTimestamp.kt` and stores epoch millis.
 * - `status` is the lowercase wire enum
 *   (`"pending"` / `"accepted"` / `"rejected"`); the mapper
 *   normalises it into [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus].
 * - `counterpart.role` (always a string, usually `"provider"` in
 *   the real backend) is decoded but intentionally ignored by
 *   the mapper — see the doc on
 *   [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart].
 *
 * The endpoint requires a valid Auth0 JWT (the `AuthInterceptor`
 * injects the bearer token from
 * [com.loresuelvo.consumer.domain.auth.AuthSessionStore]
 * automatically when a session is present).
 */
@Serializable
data class ServiceProposalDto(
    @SerialName("id") val id: Long,
    @SerialName("conversation_id") val conversationId: Long? = null,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("scheduled_on") val scheduledOn: String,
    @SerialName("description") val description: String,
    @SerialName("status") val status: String,
    @SerialName("created_on") val createdOn: String,
    @SerialName("counterpart") val counterpart: ServiceProposalCounterpartDto,
)

/**
 * Wire format for the denormalised counterpart attached to a
 * service proposal. See [ServiceProposalDto] for the rationale
 * behind each `@SerialName`. `profile_photo_url` is nullable
 * because the backend emits `null` for providers that have not
 * uploaded a photo yet (scenario 10-VSP relies on that fallback).
 */
@Serializable
data class ServiceProposalCounterpartDto(
    @SerialName("id") val id: Long,
    @SerialName("role") val role: String,
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
)
