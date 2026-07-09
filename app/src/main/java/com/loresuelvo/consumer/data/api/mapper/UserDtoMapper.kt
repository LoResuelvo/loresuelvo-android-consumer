package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.RegisterConsumerRequestDto
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData

/**
 * Domain ↔ DTO translation for the consumer-registration payload.
 *
 * This is the only place where the camelCase domain model is mapped
 * to the snake_case wire format. The mismatch between
 * `firstName`/`lastName` (domain) and `name`/`surname` (wire) is
 * intentional and isolated here.
 */
internal fun RegisterConsumerData.toDto(): RegisterConsumerRequestDto =
    RegisterConsumerRequestDto(
        email = email,
        firstName = firstName,
        surname = lastName,
    )
