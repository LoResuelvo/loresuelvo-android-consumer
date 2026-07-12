package com.loresuelvo.consumer.domain.provider

/**
 * A service provider (renamed from "Professional" in the user-facing
 * docs — the backend exposes them as `Provider` via
 * `GET /providers`; we stay aligned with the wire contract here).
 *
 * Pure domain type: camelCase, no framework deps. Wire format
 * (snake_case) is mapped in `data/api/mapper/ProviderDtoMapper.kt`.
 *
 * `categoryId`/`categoryName` are duplicated relative to
 * `Category` because the provider search response is denormalized
 * server-side; they let the UI render the row without a second
 * round-trip to the categories endpoint.
 */
data class Provider(
    val id: String,
    val name: String,
    val surname: String,
    val categoryId: Int,
    val categoryName: String,
    val profilePhotoUrl: String?,
)
