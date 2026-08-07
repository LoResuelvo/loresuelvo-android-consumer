package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ConversationCounterpartDto
import com.loresuelvo.consumer.data.api.dto.ConversationDetailDto
import com.loresuelvo.consumer.data.api.dto.ConversationDto
import com.loresuelvo.consumer.data.api.dto.ConversationMessageDto
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the DTO → domain translation for the consumer ↔ provider
 * conversations endpoint. The wire shape uses snake_case
 * (`sender_role`, `category_name`, `profile_photo_url`,
 * `created_on`, `updated_on`, `last_message`); the domain uses
 * camelCase. Each test pins one mapping decision so a backend
 * shape drift is caught immediately.
 */
class ConversationDtoMapperTest {

    private fun counterpartDto(
        id: Long = 20,
        role: String? = "provider",
        name: String = "Juan",
        surname: String = "Gómez",
        categoryName: String = "Plomería",
        profilePhotoUrl: String? = "https://cdn.example/juan.jpg",
    ) = ConversationCounterpartDto(
        id = id,
        role = role,
        name = name,
        surname = surname,
        categoryName = categoryName,
        profilePhotoUrl = profilePhotoUrl,
    )

    private fun messageDto(
        id: Long = 1,
        senderRole: String = "consumer",
        content: String = "Hola",
        createdOn: String? = "2026-05-30T14:20:00Z",
    ) = ConversationMessageDto(
        id = id,
        senderRole = senderRole,
        content = content,
        createdOn = createdOn,
    )

    private fun dto(
        id: Long = 1,
        status: String = "pending",
        counterpart: ConversationCounterpartDto = counterpartDto(),
        lastMessage: ConversationMessageDto? = messageDto(),
        updatedOn: String? = "2026-05-30T14:20:00Z",
    ) = ConversationDto(
        id = id,
        status = status,
        counterpart = counterpart,
        lastMessage = lastMessage,
        updatedOn = updatedOn,
    )

    @Test
    fun maps_numeric_id_to_string_conversationId() {
        // Backend returns `"id": 1`; domain keeps the String
        // representation so `LazyColumn` keys don't overflow.
        val conversation = dto(id = 42L).toDomain()

        assertEquals("42", conversation.id)
    }

    @Test
    fun maps_status_pending_to_Pending() {
        val conversation = dto(status = "pending").toDomain()

        assertEquals(ConversationStatus.Pending, conversation.status)
    }

    @Test
    fun unknown_status_maps_to_Other_carrying_the_raw_string() {
        // Forward-compat: a future revision adding `accepted` /
        // `rejected` must not crash the mapper.
        val conversation = dto(status = "accepted").toDomain()

        assertTrue(
            "expected ConversationStatus.Other, was ${conversation.status}",
            conversation.status is ConversationStatus.Other,
        )
        assertEquals(
            "accepted",
            (conversation.status as ConversationStatus.Other).raw,
        )
    }

    @Test
    fun status_is_case_insensitive() {
        // Tolerate `"Pending"` / `"PENDING"` etc. in case the
        // backend casing drifts.
        val conversation = dto(status = "Pending").toDomain()

        assertEquals(ConversationStatus.Pending, conversation.status)
    }

    @Test
    fun maps_counterpart_fields_and_drops_role() {
        // `role` is wire-only metadata (always "provider" from the
        // consumer's view); it must NOT leak into the domain.
        val counterpart = counterpartDto(
            id = 99,
            role = "provider",
            name = "Agustina",
            surname = "Molina",
            categoryName = "Electricidad",
            profilePhotoUrl = "https://cdn.example/agus.jpg",
        )

        val mapped = counterpart.toDomain()

        assertEquals(99L, mapped.id)
        assertEquals("Agustina", mapped.name)
        assertEquals("Molina", mapped.surname)
        assertEquals("Electricidad", mapped.categoryName)
        assertEquals("https://cdn.example/agus.jpg", mapped.profilePhotoUrl)
    }

    @Test
    fun counterpart_with_null_profile_photo_keeps_null_in_domain() {
        val mapped = counterpartDto(profilePhotoUrl = null).toDomain()

        assertNull(mapped.profilePhotoUrl)
    }

    @Test
    fun counterpart_with_blank_profile_photo_normalises_to_null() {
        // Defensive: blank strings ("") should not be passed to
        // Coil as a model URL.
        val mapped = counterpartDto(profilePhotoUrl = "   ").toDomain()

        assertNull(mapped.profilePhotoUrl)
    }

    @Test
    fun lastMessage_is_mapped_when_present() {
        val mapped = dto(
            lastMessage = messageDto(
                id = 7,
                senderRole = "consumer",
                content = "Hola Juan, ¿podés ayudarme?",
                createdOn = "2026-05-30T14:20:00Z",
            ),
        ).toDomain()

        assertTrue(mapped.lastMessage != null)
        assertEquals("7", mapped.lastMessage!!.id)
        assertEquals(ConversationSender.Consumer, mapped.lastMessage.sender)
        assertEquals("Hola Juan, ¿podés ayudarme?", mapped.lastMessage.content)
        assertTrue(
            "expected a non-zero epoch from '2026-05-30T14:20:00Z', " +
                "got ${mapped.lastMessage.createdOnEpochMillis}",
            mapped.lastMessage.createdOnEpochMillis != 0L,
        )
    }

    @Test
    fun lastMessage_is_null_when_absent() {
        val mapped = dto(lastMessage = null).toDomain()

        assertNull(mapped.lastMessage)
    }

    @Test
    fun maps_sender_role_provider_to_Provider() {
        val mapped = messageDto(senderRole = "provider").toDomain()

        assertEquals(ConversationSender.Provider, mapped.sender)
    }

    @Test
    fun unknown_sender_role_defaults_to_Provider_defensively() {
        // Future-proofing: a third sender_role (e.g. "system")
        // renders as a Provider bubble on the left so the user
        // can still read the body.
        val mapped = messageDto(senderRole = "system").toDomain()

        assertEquals(ConversationSender.Provider, mapped.sender)
    }

    @Test
    fun parses_iso_updated_on_to_epoch_millis() {
        // 2026-05-30T14:20:00Z must produce a non-zero epoch and
        // the row's lastMessage must be mapped too.
        val mapped = dto(updatedOn = "2026-05-30T14:20:00Z").toDomain()

        assertTrue(
            "expected a non-zero epoch from '2026-05-30T14:20:00Z', " +
                "got ${mapped.updatedOnEpochMillis}",
            mapped.updatedOnEpochMillis != 0L,
        )
    }

    @Test
    fun updated_on_with_trailing_Z_is_handled() {
        // The wire format always emits trailing 'Z'; the parser
        // must accept it explicitly.
        val mapped = dto(updatedOn = "2026-05-30T14:20:00Z").toDomain()
        val withoutZ = dto(updatedOn = "2026-05-30T14:20:00").toDomain()

        assertEquals(mapped.updatedOnEpochMillis, withoutZ.updatedOnEpochMillis)
    }

    @Test
    fun falls_back_to_zero_epoch_when_timestamp_is_null() {
        val mapped = dto(updatedOn = null).toDomain()

        assertEquals(0L, mapped.updatedOnEpochMillis)
    }

    @Test
    fun falls_back_to_zero_epoch_when_timestamp_is_unparseable() {
        val mapped = dto(updatedOn = "not-a-date").toDomain()

        assertEquals(0L, mapped.updatedOnEpochMillis)
    }

    @Test
    fun lastMessage_createdOn_unparseable_falls_back_to_zero() {
        val mapped = messageDto(createdOn = "???").toDomain()

        assertEquals(0L, mapped.createdOnEpochMillis)
    }

    // ---- Detail mapper (ConversationDetailDto → ConversationDetail) -----

    private fun detailDto(
        id: Long = 1,
        status: String = "pending",
        counterpart: ConversationCounterpartDto = counterpartDto(),
        messages: List<ConversationMessageDto> = listOf(messageDto()),
        updatedOn: String? = "2026-05-29T18:01:00Z",
    ) = ConversationDetailDto(
        id = id,
        status = status,
        counterpart = counterpart,
        messages = messages,
        updatedOn = updatedOn,
    )

    @Test
    fun detail_maps_numeric_id_to_string() {
        val detail = detailDto(id = 42L).toDomain()

        assertEquals("42", detail.id)
    }

    @Test
    fun detail_maps_messages_list_in_order() {
        val detail = detailDto(
            messages = listOf(
                messageDto(id = 1, senderRole = "consumer", content = "primera"),
                messageDto(id = 2, senderRole = "provider", content = "segunda"),
                messageDto(id = 3, senderRole = "consumer", content = "tercera"),
            ),
        ).toDomain()

        assertEquals(listOf("1", "2", "3"), detail.messages.map { it.id })
        assertEquals(
            listOf(ConversationSender.Consumer, ConversationSender.Provider, ConversationSender.Consumer),
            detail.messages.map { it.sender },
        )
        assertEquals(
            listOf("primera", "segunda", "tercera"),
            detail.messages.map { it.content },
        )
    }

    @Test
    fun detail_with_empty_messages_list_maps_to_empty_list() {
        val detail = detailDto(messages = emptyList()).toDomain()

        assertEquals(0, detail.messages.size)
    }

    @Test
    fun detail_maps_status_and_counterpart_and_timestamp() {
        val detail = detailDto(
            status = "pending",
            counterpart = counterpartDto(
                id = 99,
                name = "Lucía",
                surname = "Pérez",
                categoryName = "Gas",
                profilePhotoUrl = "https://cdn.example/lucia.jpg",
            ),
            updatedOn = "2026-05-29T18:01:00Z",
        ).toDomain()

        assertEquals(ConversationStatus.Pending, detail.status)
        assertEquals(99L, detail.counterpart.id)
        assertEquals("Lucía", detail.counterpart.name)
        assertEquals("Gas", detail.counterpart.categoryName)
        assertEquals("https://cdn.example/lucia.jpg", detail.counterpart.profilePhotoUrl)
        assertTrue(detail.updatedOnEpochMillis != 0L)
    }

    @Test
    fun detail_status_case_insensitive() {
        val detail = detailDto(status = "Pending").toDomain()

        assertEquals(ConversationStatus.Pending, detail.status)
    }

    @Test
    fun detail_unknown_status_falls_back_to_Other() {
        val detail = detailDto(status = "rejected").toDomain()

        assertTrue(detail.status is ConversationStatus.Other)
        assertEquals("rejected", (detail.status as ConversationStatus.Other).raw)
    }

    @Test
    fun detail_updatedOn_unparseable_falls_back_to_zero() {
        val detail = detailDto(updatedOn = "not-a-date").toDomain()

        assertEquals(0L, detail.updatedOnEpochMillis)
    }

    @Test
    fun detail_updatedOn_null_falls_back_to_zero() {
        val detail = detailDto(updatedOn = null).toDomain()

        assertEquals(0L, detail.updatedOnEpochMillis)
    }
}