package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ProviderProfileCategoryDto
import com.loresuelvo.consumer.data.api.dto.ProviderProfileCompletionReportDto
import com.loresuelvo.consumer.data.api.dto.ProviderProfileDto
import com.loresuelvo.consumer.data.api.dto.ProviderProfilePhotoDto
import com.loresuelvo.consumer.data.api.dto.ProviderProfileReviewDto
import com.loresuelvo.consumer.data.api.dto.ProviderProfileWorkOrderDto
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderProfileDtoMapperTest {

    @Test
    fun maps_nested_public_profile_contract_to_domain() {
        val profile = sampleDto().toDomain()

        assertEquals(12, profile.id)
        assertEquals("Juan", profile.name)
        assertEquals("Gómez", profile.surname)
        assertEquals("https://cdn.example/profile/foto-perfil.jpg", profile.profilePhotoUrl)
        assertEquals(1, profile.category.id)
        assertEquals("Plomería", profile.category.name)
        assertEquals(4.5, profile.ratingAverage, 0.0)
        assertEquals(2, profile.ratingCount)
        assertTrue(profile.identityVerified)
        assertEquals(1, profile.workOrders.size)
        assertEquals("84", profile.workOrders.single().id)
        assertEquals(ProviderWorkOrderStatus.Paid, profile.workOrders.single().status)
        assertEquals(
            "Trabajo finalizado y funcionamiento verificado.",
            profile.workOrders.single().completionReport?.description,
        )
        assertEquals(5, profile.workOrders.single().review?.rating)
    }

    @Test
    fun maps_missing_photo_and_review_as_null_without_exposing_private_fields() {
        val profile = sampleDto(
            profilePhoto = null,
            workOrders = listOf(
                ProviderProfileWorkOrderDto(
                    id = 85,
                    scheduledOn = "2026-08-16T15:00:00Z",
                    description = "Trabajo sin reseña",
                    status = "paid",
                    completionReport = null,
                    review = null,
                ),
            ),
        ).toDomain()

        assertNull(profile.profilePhotoUrl)
        assertNull(profile.workOrders.single().review)
        assertNull(profile.workOrders.single().completionReport)
    }

    private fun sampleDto(
        profilePhoto: ProviderProfilePhotoDto? = ProviderProfilePhotoDto(
            originalName = "foto-perfil.jpg",
            url = "https://cdn.example/profile/foto-perfil.jpg",
        ),
        workOrders: List<ProviderProfileWorkOrderDto> = listOf(
            ProviderProfileWorkOrderDto(
                id = 84,
                scheduledOn = "2026-08-15T15:00:00Z",
                description = "Reparación de pérdida de agua en cocina.",
                status = "paid",
                completionReport = ProviderProfileCompletionReportDto(
                    description = "Trabajo finalizado y funcionamiento verificado.",
                    reportedOn = "2026-08-15T16:00:00Z",
                ),
                review = ProviderProfileReviewDto(
                    rating = 5,
                    description = "Trabajo prolijo y excelente atención.",
                ),
            ),
        ),
    ): ProviderProfileDto = ProviderProfileDto(
        id = 12,
        name = "Juan",
        surname = "Gómez",
        profilePhoto = profilePhoto,
        category = ProviderProfileCategoryDto(id = 1, name = "Plomería"),
        ratingAverage = 4.5,
        ratingCount = 2,
        identityVerified = true,
        workOrders = workOrders,
    )
}
