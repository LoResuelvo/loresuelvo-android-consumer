package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CategoryDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the DTO -> domain translation for categories. The wire shape
 * matches the domain field names, so this guards against accidental
 * field drift or reordering.
 */
class CategoryDtoMapperTest {

    @Test
    fun maps_single_dto_to_domain() {
        val dto = CategoryDto(id = 4, name = "Carpintería")

        val category = dto.toDomain()

        assertEquals(4, category.id)
        assertEquals("Carpintería", category.name)
    }

    @Test
    fun maps_list_preserving_order() {
        val dtos = listOf(
            CategoryDto(id = 1, name = "Plomería"),
            CategoryDto(id = 2, name = "Electricidad"),
            CategoryDto(id = 531, name = "Streamer"),
        )

        val categories = dtos.toDomain()

        assertEquals(3, categories.size)
        assertEquals(listOf(1, 2, 531), categories.map { it.id })
        assertEquals(listOf("Plomería", "Electricidad", "Streamer"), categories.map { it.name })
    }

    @Test
    fun maps_empty_list_to_empty_list() {
        assertEquals(emptyList<Any>(), emptyList<CategoryDto>().toDomain())
    }
}
