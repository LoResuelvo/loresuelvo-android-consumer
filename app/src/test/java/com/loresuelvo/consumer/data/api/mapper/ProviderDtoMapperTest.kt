package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ProviderDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the DTO -> domain translation for providers. The wire format
 * does NOT echo `category_id` (verified against the real backend on
 * 2026-07-27: `GET /providers?category_id=X` returns objects with
 * only `id`, `name`, `surname`, `category_name`, `profile_photo_url`).
 * The mapper therefore receives the queried `categoryId` as a
 * parameter and threads it into every mapped [com.loresuelvo.consumer.domain.provider.Provider].
 */
class ProviderDtoMapperTest {

    @Test
    fun maps_single_dto_to_domain_injecting_categoryId_from_query() {
        val dto = ProviderDto(
            id = 92,
            name = "Agustina",
            surname = "Molina",
            categoryName = "Electricidad",
            categoryId = null,
            profilePhotoUrl = "http://x/p1.webp",
        )

        val provider = dto.toDomain(categoryId = 2)

        assertEquals(92, provider.id)
        assertEquals("Agustina", provider.name)
        assertEquals("Molina", provider.surname)
        assertEquals(2, provider.categoryId)
        assertEquals("Electricidad", provider.categoryName)
        assertEquals("http://x/p1.webp", provider.profilePhotoUrl)
    }

    @Test
    fun maps_list_preserving_order_and_injecting_categoryId() {
        val dtos = listOf(
            ProviderDto(
                id = 1,
                name = "Laura",
                surname = "Gómez",
                categoryName = "Electricidad",
                categoryId = null,
                profilePhotoUrl = null,
            ),
            ProviderDto(
                id = 2,
                name = "Juan",
                surname = "Pérez",
                categoryName = "Electricidad",
                categoryId = null,
                profilePhotoUrl = "http://x/p2.webp",
            ),
        )

        val providers = dtos.toDomain(categoryId = 7)

        assertEquals(2, providers.size)
        assertEquals(listOf(1, 2), providers.map { it.id })
        // Same categoryId applied to every element regardless of
        // what the wire body carried.
        assertEquals(listOf(7, 7), providers.map { it.categoryId })
        assertEquals(listOf("Electricidad", "Electricidad"), providers.map { it.categoryName })
        assertNull(providers[0].profilePhotoUrl)
        assertEquals("http://x/p2.webp", providers[1].profilePhotoUrl)
    }

    @Test
    fun maps_empty_list_to_empty_list() {
        assertEquals(emptyList<Any>(), emptyList<ProviderDto>().toDomain(categoryId = 1))
    }

    @Test
    fun null_categoryId_in_dto_does_not_override_injected_categoryId() {
        // Guards against future regressions where the mapper might
        // accidentally read the (nullable) DTO field instead of
        // the injected parameter.
        val dto = ProviderDto(
            id = 1,
            name = "X",
            surname = "Y",
            categoryName = "Plomería",
            categoryId = null,
            profilePhotoUrl = null,
        )

        val provider = dto.toDomain(categoryId = 42)

        assertEquals(42, provider.categoryId)
    }
}