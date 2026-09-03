package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ServiceProposalCounterpartDto
import com.loresuelvo.consumer.data.api.dto.ServiceProposalDto
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the DTO -> domain translation for `GET /service-proposals`.
 *
 * The mapper is the only place that touches both worlds; every
 * rule that differs (snake_case → camelCase, `Long` ids →
 * `String` keys, ISO strings → epoch millis, lowercase wire
 * status → typed enum, unknown statuses silently dropped) lives
 * here and is pinned by one test below.
 */
class ServiceProposalDtoMapperTest {

    private fun dto(
        id: Long = 1L,
        conversationId: Long? = 10L,
        status: String = "pending",
        amountCents: Long = 1500000L,
        scheduledOn: String = "2026-10-15T14:30:00Z",
        createdOn: String = "2026-09-03T11:19:24.640Z",
        description: String = "Fuga en el lavamanos del baño",
        counterpartRole: String = "provider",
        counterpartName: String = "Carlos",
        counterpartSurname: String = "López",
        counterpartCategory: String = "Plomería",
        counterpartPhotoUrl: String? = "http://x/c.webp",
    ): ServiceProposalDto = ServiceProposalDto(
        id = id,
        conversationId = conversationId,
        amountCents = amountCents,
        scheduledOn = scheduledOn,
        description = description,
        status = status,
        createdOn = createdOn,
        counterpart = ServiceProposalCounterpartDto(
            id = 92L,
            role = counterpartRole,
            name = counterpartName,
            surname = counterpartSurname,
            categoryName = counterpartCategory,
            profilePhotoUrl = counterpartPhotoUrl,
        ),
    )

    @Test
    fun maps_single_dto_to_domain_with_stringified_ids_and_epoch_millis() {
        val mapped = dto().toDomain()!!

        assertEquals("1", mapped.id)
        assertEquals("10", mapped.conversationId)
        assertEquals(ServiceProposalStatus.Pending, mapped.status)
        assertEquals(1500000L, mapped.amountCents)
        assertEquals("Fuga en el lavamanos del baño", mapped.description)
        // 2026-10-15T14:30:00Z is the parser's first pattern
        // (millisecond precision, trailing 'Z'). The millis value
        // is platform-dependent only on timezone; the parser
        // always uses UTC, so the assertion is safe across hosts.
        assertEquals(1792074600000L, mapped.scheduledOnEpochMillis)
        assertEquals(1788434364640L, mapped.createdOnEpochMillis)
    }

    @Test
    fun counterpart_role_is_ignored_and_only_snapshot_fields_are_mapped() {
        val mapped = dto(counterpartRole = "consumer").toDomain()!!

        assertEquals("92", mapped.counterpart.id)
        assertEquals("Carlos", mapped.counterpart.name)
        assertEquals("López", mapped.counterpart.surname)
        assertEquals("Plomería", mapped.counterpart.categoryName)
        assertEquals("http://x/c.webp", mapped.counterpart.profilePhotoUrl)
    }

    @Test
    fun null_profile_photo_url_collapses_to_null_domain_field() {
        val mapped = dto(counterpartPhotoUrl = null).toDomain()!!

        assertNull(mapped.counterpart.profilePhotoUrl)
    }

    @Test
    fun null_conversation_id_collapses_to_null_domain_field() {
        val mapped = dto(conversationId = null).toDomain()!!

        assertNull(mapped.conversationId)
    }

    @Test
    fun lowercase_status_normalises_to_typed_enum_for_every_branch() {
        assertEquals(
            ServiceProposalStatus.Pending,
            dto(status = "pending").toDomain()!!.status,
        )
        assertEquals(
            ServiceProposalStatus.Accepted,
            dto(status = "accepted").toDomain()!!.status,
        )
        assertEquals(
            ServiceProposalStatus.Rejected,
            dto(status = "rejected").toDomain()!!.status,
        )
    }

    @Test
    fun unknown_status_returns_null_so_list_mapper_can_drop_it() {
        // Future-proofing: if the backend ever introduces a new
        // status before the client knows about it, the safest
        // behaviour is to drop the proposal from the visible list
        // rather than crash or surface it with a bogus status.
        assertNull(dto(status = "negotiating").toDomain())
    }

    @Test
    fun list_mapper_preserves_order_and_drops_unknown_status_entries() {
        val mapped = listOf(
            dto(id = 1L, status = "pending"),
            dto(id = 2L, status = "negotiating"),
            dto(id = 3L, status = "accepted"),
            dto(id = 4L, status = "rejected"),
        ).toDomain()

        assertEquals(listOf("1", "3", "4"), mapped.map { it.id })
    }

    @Test
    fun empty_list_maps_to_empty_list() {
        assertEquals(emptyList<Any>(), emptyList<ServiceProposalDto>().toDomain())
    }
}
