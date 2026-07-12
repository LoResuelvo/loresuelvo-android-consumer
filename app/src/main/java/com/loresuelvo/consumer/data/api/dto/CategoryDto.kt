package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for `GET /categories`. The backend returns a JSON
 * array of objects with `id` (int) and `name` (string); both are
 * already camelCase-friendly, so the mapping in
 * `data/api/mapper/CategoryDtoMapper.kt` is a straight copy.
 *
 * Example element: `{ "id": 4, "name": "Carpintería" }`.
 */
@Serializable
data class CategoryDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
)
