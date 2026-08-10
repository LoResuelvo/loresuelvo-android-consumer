package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for `assessment` inside the AI diagnosis response.
 *
 * The backend (Go) emits:
 * ```
 * {
 *   "outcome": "collecting_information" | "professional_required" | ...,
 *   "problem_category": {"id": 3, "name": "Plomería"}   // optional
 * }
 * ```
 * Confirmed against `loresuelvo-api` on 2026-08-10 against the
 * staging dev backend. `outcome` carries one of the typed states
 * the AI settles into; `problem_category` is populated only on
 * the terminal `professional_required` state (so the app can show
 * the user the recommended rubro + the matching provider list
 * without a second round-trip to `GET /categories`).
 *
 * Reusing `CategoryDto` for `problem_category` matches the
 * `GET /categories` schema (id + name, both already
 * camelCase-friendly). New outcomes are decoded as opaque strings
 * on the domain side so a future backend addition doesn't crash
 * deserialization; only known outcomes gate UI behaviour.
 */
@Serializable
data class AssessmentDto(
    @SerialName("outcome") val outcome: String,
    @SerialName("problem_category") val problemCategory: CategoryDto? = null,
)
