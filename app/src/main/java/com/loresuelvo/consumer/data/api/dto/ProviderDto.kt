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
 * The endpoint is **public** (no auth required) per the spec, so the
 * `AuthInterceptor` simply doesn't add the `Authorization` header when
 * there is no session.
 */
@Serializable
data class ProviderDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
)
