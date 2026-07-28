package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for `GET /providers?category_id=X` (the backend's
 * "providers" — we call them `Provider` in the domain for that
 * exact reason, see `domain/provider/Provider.kt`).
 *
 * Example element:
 * ```
 * {
 *   "id": 43,
 *   "name": "Carlos",
 *   "surname": "López",
 *   "category_name": "Plomería",
 *   "profile_photo_url": "http://minio.localhost:9000/.../provider-0043.webp"
 * }
 * ```
 *
 * `category_id` is **not** echoed in the response — the consumer
 * app queries by it and threads the value through
 * [com.loresuelvo.consumer.data.api.mapper.toDomain] (see
 * `ProviderDtoMapper`). Marking it nullable keeps
 * kotlinx-serialization honest with the real wire shape instead of
 * throwing `MissingFieldException`.
 *
 * The endpoint requires a valid Auth0 JWT: the backend returns
 * `401 {"error":"invalid_token"}` for unauthenticated calls. The
 * `AuthInterceptor` injects the bearer token automatically when a
 * session is present in the [com.loresuelvo.consumer.domain.auth.AuthSessionStore].
 */
@Serializable
data class ProviderDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
)
