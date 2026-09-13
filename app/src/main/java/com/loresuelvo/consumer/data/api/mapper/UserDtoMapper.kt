package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.RegisterConsumerRequestDto
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData

internal fun RegisterConsumerData.toDto(): RegisterConsumerRequestDto =
    RegisterConsumerRequestDto(
        email = email,
        firstName = firstName,
        surname = lastName,
        address = address.toDto(),
    )