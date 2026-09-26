package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CompletionReportDto
import com.loresuelvo.consumer.data.api.dto.CompletionReportPhotoDto
import com.loresuelvo.consumer.data.api.dto.ReviewDto
import com.loresuelvo.consumer.data.api.dto.WorkOrderDetailDto
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the DTO -> domain translation for
 * `GET /work-orders/{workOrderID}` (US-27
 * `visualize-turns-detail`).
 *
 * Every rule that differs (snake_case → camelCase, `Long` ids →
 * `String` keys, ISO strings → epoch millis, lowercase wire
 * status → typed enum, unknown statuses silently dropped) lives
 * in `WorkOrderDetailDto.toDomain()` and is pinned here.
 */
class WorkOrderDetailDtoMapperTest {

    private fun provider(): WorkOrderDetailCounterpart = WorkOrderDetailCounterpart(
        id = "7",
        name = "Ana",
        surname = "Gómez",
        categoryName = "Electricidad",
        profilePhotoUrl = "https://example.com/ana.jpg",
    )

    private fun dto(
        id: Long = 42L,
        serviceProposalId: Long = 10L,
        consumerId: Long = 3L,
        providerId: Long = 7L,
        amountCents: Long = 10_000_000L,
        scheduledOn: String = "2026-08-15T15:00:00Z",
        description: String = "Reparación de pérdida de agua en cocina",
        status: String = "scheduled",
        acceptedOn: String? = "2026-08-01T13:00:00Z",
        paidOn: String? = null,
        completionReport: CompletionReportDto? = null,
        review: ReviewDto? = null,
    ): WorkOrderDetailDto = WorkOrderDetailDto(
        id = id,
        serviceProposalId = serviceProposalId,
        consumerId = consumerId,
        providerId = providerId,
        amountCents = amountCents,
        scheduledOn = scheduledOn,
        description = description,
        status = status,
        acceptedOn = acceptedOn,
        paidOn = paidOn,
        completionReport = completionReport,
        review = review,
    )

    @Test
    fun maps_scheduled_work_order_to_confirmed_status() {
        val detail = dto(status = "scheduled").toDomain(provider())
        assertNotNull(detail)
        assertEquals(com.loresuelvo.consumer.domain.turno.TurnoStatus.Confirmed, detail!!.status)
    }

    @Test
    fun maps_awaiting_payment_work_order() {
        val detail = dto(
            status = "awaiting_payment",
            completionReport = CompletionReportDto(
                id = 17L,
                description = "Trabajo finalizado",
                reportedOn = "2026-08-15T16:00:00Z",
                images = emptyList(),
            ),
            paidOn = null,
        ).toDomain(provider())
        assertNotNull(detail)
        assertEquals(
            com.loresuelvo.consumer.domain.turno.TurnoStatus.AwaitingPayment,
            detail!!.status,
        )
        assertNotNull(detail.completionReport)
        assertNull(detail.paidOnEpochMillis)
    }

    @Test
    fun maps_paid_work_order_with_paid_on_and_review() {
        val detail = dto(
            status = "paid",
            paidOn = "2026-08-15T17:00:00Z",
            completionReport = CompletionReportDto(
                id = 17L,
                description = "Trabajo finalizado",
                reportedOn = "2026-08-15T16:00:00Z",
                images = listOf(
                    CompletionReportPhotoDto(
                        fileId = "uuid-1",
                        originalName = "trabajo.jpg",
                        url = "https://example.com/img1",
                    ),
                ),
            ),
            review = ReviewDto(rating = 5, description = "Excelente atención"),
        ).toDomain(provider())
        assertNotNull(detail)
        assertEquals(com.loresuelvo.consumer.domain.turno.TurnoStatus.Paid, detail!!.status)
        assertNotNull(detail.paidOnEpochMillis)
        assertNotNull(detail.completionReport)
        assertEquals(1, detail.completionReport!!.images.size)
        assertEquals("uuid-1", detail.completionReport.images[0].fileId)
        assertEquals("https://example.com/img1", detail.completionReport.images[0].url)
        assertNotNull(detail.review)
        assertEquals(5, detail.review!!.rating)
    }

    @Test
    fun unknown_status_returns_null() {
        val detail = dto(status = "mystery").toDomain()
        assertNull(detail)
    }

    @Test
    fun maps_provider_from_existing_counterpart_when_detail_payload_has_none() {
        val fallbackProvider = WorkOrderDetailCounterpart(
            id = "7",
            name = "Ana",
            surname = "Gómez",
            categoryName = "Electricidad",
            profilePhotoUrl = "https://example.com/ana.jpg",
        )

        val detail = dto().toDomain(fallbackProvider)
        assertNotNull(detail)
        assertEquals("Ana", detail!!.provider.name)
        assertEquals("Gómez", detail.provider.surname)
        assertEquals("Electricidad", detail.provider.categoryName)
        assertEquals("https://example.com/ana.jpg", detail.provider.profilePhotoUrl)
    }

    @Test
    fun long_ids_become_strings() {
        val detail = dto(id = 99L, serviceProposalId = 7L).toDomain(provider())
        assertNotNull(detail)
        // proposalId is the join key — we surface the
        // serviceProposalId rather than the work-order id since
        // today they happen to match (the legacy adapter
        // mirrors them). The unit test pins the conversion
        // surface.
        assertEquals("7", detail!!.proposalId)
    }

    @Test
    fun malformed_iso_string_collapses_to_zero() {
        val detail = dto(
            scheduledOn = "not-a-date",
            acceptedOn = "not-a-date",
        ).toDomain(provider())
        assertNotNull(detail)
        assertEquals(0L, detail!!.scheduledOnEpochMillis)
        assertEquals(0L, detail.acceptedOnEpochMillis)
    }
}
