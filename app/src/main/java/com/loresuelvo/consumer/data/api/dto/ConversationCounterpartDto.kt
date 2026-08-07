package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for the `counterpart` block inside
 * [ConversationDto]. Carries the provider's profile fields the
 * list cell needs without a second round-trip (avatar, full name,
 * category badge).
 *
 * `role` is `"provider"` from the consumer's perspective today.
 * The field is kept optional in the DTO so a future revision that
 * drops it does not break the deserializer; the mapper drops it
 * (the domain counterpart is always the provider in this app).
 */
@Serializable
data class ConversationCounterpartDto(
    @SerialName("id") val id: Long,
    @SerialName("role") val role: String? = null,
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
)