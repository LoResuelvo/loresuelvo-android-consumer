package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ProviderDto
import com.loresuelvo.consumer.domain.provider.Provider

/**
 * DTO -> domain translation for providers. The wire shape already
 * matches the domain field names; the `id` is numeric both at the
 * wire boundary and in the domain (`Int`).
 *
 * We keep this in a mapper file (vs an inline copy at the call
 * site) because the project rule for DTOs maps: `data/api/dto/` is
 * the only place that touches wire types; `data/api/mapper/` is the
 * only place the boundary crosses.
 */
internal fun ProviderDto.toDomain(): Provider = Provider(
    id = id,
    name = name,
    surname = surname,
    categoryId = categoryId,
    categoryName = categoryName,
    profilePhotoUrl = profilePhotoUrl,
)

internal fun List<ProviderDto>.toDomain(): List<Provider> =
    map { it.toDomain() }
