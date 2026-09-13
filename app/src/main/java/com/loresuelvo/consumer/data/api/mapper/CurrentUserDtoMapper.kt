package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CurrentUserDto
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User

internal fun CurrentUserDto.toDomain(): User = User(
    displayName = listOf(firstName, lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { email },
    firstName = firstName,
    lastName = lastName,
    email = email,
    address = address?.let {
        RegisterConsumerAddress(
            street = it.street,
            streetNumber = it.streetNumber,
            floor = it.floor,
            unit = it.unit,
        )
    },
)