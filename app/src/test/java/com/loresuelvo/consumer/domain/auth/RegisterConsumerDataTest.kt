package com.loresuelvo.consumer.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for the pure-domain [RegisterConsumerData]. These are
 * intentionally minimal: a data class with three String fields has
 * nothing else to test beyond construction and copy semantics. The
 * test serves as a compile-time check that the public surface stays
 * unchanged.
 */
class RegisterConsumerDataTest {

    @Test
    fun constructs_with_required_fields() {
        val data = RegisterConsumerData(
            email = "ana@example.com",
            firstName = "Ana",
            lastName = "Perez",
        )
        assertEquals("ana@example.com", data.email)
        assertEquals("Ana", data.firstName)
        assertEquals("Perez", data.lastName)
    }

    @Test
    fun copy_preserves_immutability() {
        val original = RegisterConsumerData("a@x.com", "A", "B")
        val copy = original.copy(firstName = "C")
        assertEquals("A", original.firstName)
        assertEquals("C", copy.firstName)
        assertNotEquals(original, copy)
    }
}
