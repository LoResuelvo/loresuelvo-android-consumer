package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ProviderDto
import com.loresuelvo.consumer.domain.provider.Provider

/**
 * DTO -> domain translation for providers. The wire shape does
 * NOT echo `category_id` (verified against the real backend on
 * 2026-07-27: `GET /providers?category_id=X` returns objects with
 * `id`, `name`, `surname`, `category_name`, `profile_photo_url`
 * only). The queried [categoryId] is therefore passed explicitly to
 * the mapper and threaded into every mapped
 * [com.loresuelvo.consumer.domain.provider.Provider].
 *
 * Keeping the field nullable in the DTO while non-null in the
 * domain is intentional: the wire contract is lossy by design (the
 * backend knows the category from the query) and forcing the app
 * to invent a value it never received would be a lie. The
 * [com.loresuelvo.consumer.data.api.ApiProviderRepository] owns
 * the threading so the rest of the app keeps the non-null
 * invariant on [Provider.categoryId].
 */
internal fun ProviderDto.toDomain(categoryId: Int): Provider = Provider(
    id = id,
    name = name,
    surname = surname,
    categoryId = categoryId,
    categoryName = categoryName,
    profilePhotoUrl = profilePhotoUrl,
)

internal fun List<ProviderDto>.toDomain(categoryId: Int): List<Provider> =
    map { it.toDomain(categoryId) }