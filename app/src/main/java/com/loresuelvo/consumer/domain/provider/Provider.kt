package com.loresuelvo.consumer.domain.provider

/**
 * A service provider (renamed from "Professional" in the user-facing
 * docs — the backend exposes them as `Provider` via
 * `GET /providers`; we stay aligned with the wire contract here).
 *
 * Pure domain type: camelCase, no framework deps. Wire format
 * (snake_case) is mapped in `data/api/mapper/ProviderDtoMapper.kt`.
 *
 * `categoryId` is **not** echoed by the backend in
 * `GET /providers?category_id=X` (verified 2026-07-27): the wire
 * response only carries `category_name`. The repository injects
 * the queried `categoryId` into every mapped [Provider] so the
 * domain keeps the non-null invariant. `categoryName` is still
 * denormalised server-side so the UI can render the row without a
 * second round-trip to the categories endpoint.
 */
data class Provider(
    val id: Int,
    val name: String,
    val surname: String,
    val categoryId: Int,
    val categoryName: String,
    val profilePhotoUrl: String?,
)
