package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CurrentUserDto
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import com.loresuelvo.consumer.domain.auth.User

internal fun CurrentUserDto.toDomain(): User = User(
    backendUserId = id,
    displayName = listOf(firstName, lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { email },
    firstName = firstName,
    lastName = lastName,
    email = email,
    calendarConnectionStatus = CalendarConnectionStatus.fromWire(calendarConnectionStatus),
    profilePhotoUrl = profilePhoto?.url,
    address = address?.let {
        RegisterConsumerAddress(
            street = it.street,
            streetNumber = it.streetNumber,
            floor = it.floor,
            unit = it.unit,
        )
    },
)
