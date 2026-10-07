package com.loresuelvo.consumer.ui.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushRegistrationRequestsTest {
    @Test
    fun candidate_remains_pending_until_acknowledged() {
        val requests = PushRegistrationRequests()
        assertNull(requests.pending.value)

        requests.request()
        val candidate = requests.pending.value!!
        assertEquals(candidate, requests.pending.value)
        assertTrue(requests.acknowledge(candidate.generation))
        assertNull(requests.pending.value)
    }

    @Test
    fun latest_candidate_replaces_previous_and_old_ack_cannot_clear_it() {
        val requests = PushRegistrationRequests()
        requests.request()
        val previous = requests.pending.value!!
        requests.request()
        val latest = requests.pending.value!!

        assertTrue(latest.generation > previous.generation)
        assertFalse(requests.acknowledge(previous.generation))
        assertEquals(latest, requests.pending.value)
        assertTrue(requests.acknowledge(latest.generation))
    }

    @Test
    fun generations_are_not_reused_after_acknowledgement() {
        val requests = PushRegistrationRequests()
        requests.request()
        val previous = requests.pending.value!!
        requests.acknowledge(previous.generation)
        requests.request()

        assertNotNull(requests.pending.value)
        assertTrue(requests.pending.value!!.generation > previous.generation)
        assertFalse(requests.acknowledge(previous.generation))
    }
}
