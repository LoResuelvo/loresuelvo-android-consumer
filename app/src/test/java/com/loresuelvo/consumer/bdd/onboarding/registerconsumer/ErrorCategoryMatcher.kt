package com.loresuelvo.consumer.bdd.onboarding.registerconsumer

import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileError

internal object ErrorCategoryMatcher {

    private val matchers: Map<String, (CompleteProfileError) -> Boolean> = mapOf(
        "first name required" to { it is CompleteProfileError.MissingFirstName },
        "last name required" to { it is CompleteProfileError.MissingLastName },
        "network" to { it is CompleteProfileError.Network },
        "server" to { it is CompleteProfileError.Server },
        "session expired" to { it is CompleteProfileError.Unauthorized },
    )

    /**
     * Asserts that [error] matches the [category] alias. Throws
     * `AssertionError` with a human-readable message on mismatch and
     * `IllegalArgumentException` for unknown category names.
     */
    fun assertMatches(category: String, error: CompleteProfileError) {
        val matcher = matchers[category]
            ?: throw IllegalArgumentException(
                "Unknown error category '$category'. Known: ${matchers.keys}"
            )
        val ok = matcher(error)
        if (!ok) {
            val expected = matchers.entries.joinToString(separator = ", ") {
                "'${it.key}'"
            }
            throw AssertionError(
                "Expected an error matching $expected, got $error",
            )
        }
    }
}
