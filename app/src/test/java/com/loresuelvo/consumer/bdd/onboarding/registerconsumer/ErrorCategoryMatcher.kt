package com.loresuelvo.consumer.bdd.onboarding.registerconsumer

import com.loresuelvo.consumer.ui.auth.CompleteProfileError

/**
 * Maps the Gherkin-level error category labels used in
 * `register-consumer.feature` to the typed [CompleteProfileError]
 * variants produced by `CompleteProfileViewModel`. Keeps the
 * `.feature` readable by non-developers while binding it to the
 * production state machine.
 *
 * The labels live in the same vocabulary as the user-facing errors:
 * "first name required" → `MissingFirstName`,
 * "session expired"     → `Unauthorized`, …
 */
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
