package com.loresuelvo.consumer.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [UserRegistrationOutcome] sealed interface. The
 * tests verify the type's structural shape and copy semantics so
 * callers can pattern-match on it without surprises.
 */
class UserRegistrationOutcomeTest {

    private val user = User(
        displayName = "Ana",
        firstName = "Ana",
        lastName = "Perez",
        email = "ana@example.com",
    )

    @Test
    fun success_carries_user() {
        val outcome: UserRegistrationOutcome = UserRegistrationOutcome.Success(user)
        assertTrue(outcome is UserRegistrationOutcome.Success)
        assertSame(user, (outcome as UserRegistrationOutcome.Success).user)
    }

    @Test
    fun network_failure_carries_cause() {
        val cause = RuntimeException("dns error")
        val outcome: UserRegistrationOutcome = UserRegistrationOutcome.Failure.Network(cause)
        assertTrue(outcome is UserRegistrationOutcome.Failure)
        assertSame(cause, (outcome as UserRegistrationOutcome.Failure.Network).cause)
    }

    @Test
    fun server_failure_carries_code_and_message() {
        val outcome: UserRegistrationOutcome =
            UserRegistrationOutcome.Failure.Server(code = 409, message = "Email is already registered")
        val failure = outcome as UserRegistrationOutcome.Failure.Server
        assertEquals(409, failure.code)
        assertEquals("Email is already registered", failure.message)
    }

    @Test
    fun unauthorized_failure_carries_message() {
        val outcome: UserRegistrationOutcome =
            UserRegistrationOutcome.Failure.Unauthorized(message = "Token expired")
        val failure = outcome as UserRegistrationOutcome.Failure.Unauthorized
        assertEquals("Token expired", failure.message)
    }

    @Test
    fun success_and_failure_are_distinct() {
        val success: UserRegistrationOutcome = UserRegistrationOutcome.Success(user)
        val failure: UserRegistrationOutcome = UserRegistrationOutcome.Failure.Network(RuntimeException())
        assertNotEquals(success, failure)
    }

    @Test
    fun failure_subtypes_share_parent_type() {
        val outcomes: List<UserRegistrationOutcome.Failure> = listOf(
            UserRegistrationOutcome.Failure.Network(RuntimeException()),
            UserRegistrationOutcome.Failure.Server(500, "internal"),
            UserRegistrationOutcome.Failure.Unauthorized("expired"),
        )
        outcomes.forEach { assertNotNull(it) }
    }
}
