package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.TurnoCounterpartDto
import com.loresuelvo.consumer.data.api.dto.TurnoDto
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the DTO -> domain translation for the `GET /work-orders`
 * endpoint (visualize-turns.feature + visualize-turns-detail).
 *
 * Every rule that differs (snake_case → camelCase, `Long` ids →
 * `String` keys, ISO strings → epoch millis, lowercase wire
 * status → typed enum, unknown statuses silently dropped) lives
 * in `TurnoDto.toDomain()` and is pinned here. US-27 widens the
 * recognised statuses with `awaiting_payment` and `paid` so
 * the same enum powers both the list and the detail surface.
 */
class TurnoDtoMapperTest {

    private fun dto(
        id: Long = 42L,
        serviceProposalId: Long = 10L,
        amountCents: Long = 1500050L,
        scheduledOn: String = "2026-07-05T12:30:00Z",
        description: String = "Reparación de cañería",
        status: String = "scheduled",
        counterpartRole: String = "provider",
        counterpartId: Long = 7L,
        counterpartName: String = "Juan",
        counterpartSurname: String = "Gómez",
        counterpartCategoryName: String = "Plomería",
        counterpartProfilePhotoUrl: String? = null,
    ): TurnoDto = TurnoDto(
        id = id,
        serviceProposalId = serviceProposalId,
        amountCents = amountCents,
        scheduledOn = scheduledOn,
        description = description,
        status = status,
        counterpart = TurnoCounterpartDto(
            id = counterpartId,
            role = counterpartRole,
            name = counterpartName,
            surname = counterpartSurname,
            categoryName = counterpartCategoryName,
            profilePhotoUrl = counterpartProfilePhotoUrl,
        ),
    )

    @Test
    fun maps_scheduled_status_to_confirmed() {
        val turno = dto(status = "scheduled").toDomain()
        assertNotNull(turno)
        assertEquals(TurnoStatus.Confirmed, turno!!.status)
        assertEquals("42", turno.id)
        assertEquals("10", turno.serviceProposalId)
        assertEquals(1500050L, turno.amountCents)
        assertEquals("Reparación de cañería", turno.description)
    }

    @Test
    fun maps_awaiting_payment_status() {
        val turno = dto(status = "awaiting_payment").toDomain()
        assertNotNull(turno)
        assertEquals(TurnoStatus.AwaitingPayment, turno!!.status)
    }

    @Test
    fun maps_paid_status() {
        val turno = dto(status = "paid").toDomain()
        assertNotNull(turno)
        assertEquals(TurnoStatus.Paid, turno!!.status)
    }

    @Test
    fun unknown_status_returns_null() {
        val turno = dto(status = "mystery").toDomain()
        assertNull(turno)
    }

    @Test
    fun status_is_normalised_lowercase() {
        // Backend may emit mixed case; mapper must accept it.
        val turno = dto(status = "Awaiting_Payment").toDomain()
        assertNotNull(turno)
        assertEquals(TurnoStatus.AwaitingPayment, turno!!.status)
    }

    @Test
    fun parses_iso_scheduled_on_to_epoch_millis() {
        val turno = dto(scheduledOn = "2026-07-05T12:30:00Z").toDomain()
        assertNotNull(turno)
        // 2026-07-05T12:30:00Z is non-zero; exact millis pinned by
        // the IsoTimestamp helper's own tests (we only assert that
        // the mapper passes through the parsed value here).
        assert(turno!!.scheduledOnEpochMillis > 0L) {
            "expected a positive epoch ms for a valid ISO timestamp, was ${turno.scheduledOnEpochMillis}"
        }
    }

    @Test
    fun malformed_scheduled_on_collapses_to_zero() {
        val turno = dto(scheduledOn = "not-a-date").toDomain()
        assertNotNull(turno)
        assertEquals(0L, turno!!.scheduledOnEpochMillis)
    }

    @Test
    fun maps_counterpart_without_role() {
        val turno = dto(
            counterpartId = 99L,
            counterpartName = "Ana",
            counterpartSurname = "Pérez",
            counterpartCategoryName = "Electricidad",
            counterpartProfilePhotoUrl = "https://example.com/ana.jpg",
        ).toDomain()
        assertNotNull(turno)
        assertEquals("99", turno!!.counterpart.id)
        assertEquals("Ana", turno.counterpart.name)
        assertEquals("Pérez", turno.counterpart.surname)
        assertEquals("Electricidad", turno.counterpart.categoryName)
        assertEquals("https://example.com/ana.jpg", turno.counterpart.profilePhotoUrl)
    }
}
