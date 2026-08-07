package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for the `GET /conversations/{id}` response — the
 * full snapshot of a single conversation including its complete
 * `messages` thread.
 *
 * Example (dev backend):
 * ```
 * {
 *   "id": 4,
 *   "type": "work",
 *   "status": "pending",
 *   "work": {
 *     "counterpart": {
 *       "id": 56,
 *       "role": "provider",
 *       "name": "Florencia",
 *       ...
 *     }
 *   },
 *   "messages": [],
 *   "updated_on": "2026-08-07T15:46:40.928659Z"
 * }
 * ```
 *
 * Two non-obvious wire quirks this DTO absorbs:
 *
 *  1. **`counterpart` is nested under `work`** on the detail
 *     endpoint. The list endpoint (`ConversationDto`) keeps it
 *     at the root; the detail endpoint wraps it. The mapper in
 *     `ConversationDtoMapper` extracts from `work.counterpart`
 *     with a fallback to the root field for tolerance against
 *     future shape drift.
 *
 *  2. **`type: "work"`** is an opaque discriminator the backend
 *     emits; `ignoreUnknownKeys = true` drops it from the
 *     decoded object.
 *
 * `updated_on` carries microseconds + trailing `Z`
 * (`2026-08-07T15:46:40.928659Z`); the timestamp parser at
 * `ConversationDtoMapper.parseIsoMillisOrZero` accepts both
 * that form and the bare-seconds form.
 */
@Serializable
data class ConversationDetailDto(
    @SerialName("id") val id: Long,
    @SerialName("status") val status: String,
    @SerialName("work") val work: WorkDto? = null,
    @SerialName("counterpart") val counterpart: ConversationCounterpartDto? = null,
    @SerialName("messages") val messages: List<ConversationMessageDto> = emptyList(),
    @SerialName("updated_on") val updatedOn: String? = null,
) {
    /**
     * Wrapper the dev backend emits around the counterpart
     * block on the detail endpoint. Future revisions may drop
     * the wrapper — the mapper falls back to the root
     * `counterpart` field when `work` is absent.
     */
    @Serializable
    data class WorkDto(
        @SerialName("counterpart") val counterpart: ConversationCounterpartDto,
    )
}