package com.loresuelvo.consumer.domain.category

/**
 * A service category offered on the platform (e.g. "Plomería",
 * "Electricidad"). Pure domain type: camelCase, no framework
 * dependencies. The backend's snake_case wire shape is mapped in
 * `data/api/mapper/CategoryDtoMapper.kt`.
 */
data class Category(
    val id: Int,
    val name: String,
)
