package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.RegisterConsumerAddressDto
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress

internal fun RegisterConsumerAddress.toDto(): RegisterConsumerAddressDto =
    RegisterConsumerAddressDto(
        street = street,
        streetNumber = streetNumber,
        floor = floor,
        unit = unit,
    )