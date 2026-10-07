package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CurrentUserDto
import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CurrentUserDtoMapperTest {

    @Test
    fun maps_calendar_connection_status_from_profile() {
        val user = CurrentUserDto(
            id = 10,
            firstName = "Ana",
            lastName = "Perez",
            email = "ana@example.com",
            role = "consumer",
            calendarConnectionStatus = "action_required",
        ).toDomain()

        assertEquals(CalendarConnectionStatus.ACTION_REQUIRED, user.calendarConnectionStatus)
    }

    @Test
    fun unknown_calendar_status_does_not_optimistically_mark_connection() {
        val user = CurrentUserDto(
            id = 10,
            firstName = "Ana",
            lastName = "Perez",
            email = "ana@example.com",
            role = "consumer",
            calendarConnectionStatus = "unexpected",
        ).toDomain()

        assertEquals(CalendarConnectionStatus.UNKNOWN, user.calendarConnectionStatus)
    }

    @Test
    fun preserves_api_identity_independently_of_email_and_display_name() {
        val profile = CurrentUserDto(
            id = 17,
            firstName = "Ana",
            lastName = "Perez",
            email = "consumer@example.test",
            role = "consumer",
        )

        val first = profile.toDomain()
        val second = profile.copy(id = 29).toDomain()

        assertEquals(17, first.backendUserId)
        assertEquals(29, second.backendUserId)
        assertEquals(first.email, second.email)
        assertEquals(first.displayName, second.displayName)
    }

    @Test
    fun incomplete_backend_profile_still_retains_its_api_identity() {
        val dto = CurrentUserDto(
            id = 43,
            firstName = "",
            lastName = "",
            email = "consumer@example.test",
            role = "consumer",
        )

        val user = dto.toDomain()

        assertEquals(43, user.backendUserId)
        assertFalse(user.isProfileComplete())
        assertEquals(dto.email, user.displayName)
    }
}
