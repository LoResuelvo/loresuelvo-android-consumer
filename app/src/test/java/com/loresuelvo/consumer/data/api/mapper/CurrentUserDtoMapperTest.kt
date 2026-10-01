package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CurrentUserDto
import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import org.junit.Assert.assertEquals
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
}
