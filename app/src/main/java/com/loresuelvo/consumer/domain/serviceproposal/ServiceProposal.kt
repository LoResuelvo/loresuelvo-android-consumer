package com.loresuelvo.consumer.domain.serviceproposal

/**
 * A provider-issued offer for a service job, as seen by the
 * consumer on the receiving end. Pure domain type: camelCase,
 * no framework dependencies, no JSON.
 *
 * Wire shape lives in
 * `data/api/dto/ServiceProposalDto.kt` (snake_case, `Long` ids,
 * `amount_cents` in cents, `scheduled_on` / `created_on` as ISO
 * strings). Mapping rules:
 *
 * - `id` and `counterpart.id` come as `Long` on the wire and are
 *   stringified in the domain to mirror the convention used by
 *   `JobRequest` and `Conversation` (stable LazyColumn keys, no
 *   overflow concerns).
 * - `conversationId` is nullable on the wire and stays nullable
 *   in the domain: a freshly-created proposal may not have its
 *   conversation materialised yet, and the "Ver conversación"
 *   CTA (scenario 13-VSP) must cope with that.
 * - `amountCents` is `Long` because the backend exposes very
 *   large cent values (the example body carries
 *   `9007199254740991`). Currency formatting (`"$ 15.000"`) is a
 *   presentation concern handled in scenario 11-VSP.
 * - `scheduledOnEpochMillis` / `createdOnEpochMillis` carry the
 *   timestamp as epoch millis parsed via
 *   `data/api/mapper/IsoTimestamp.kt`. `minSdk = 24` rules out
 *   `java.time.Instant` for the domain type; millis is enough
 *   for ordering and for the "15/10/2026 - 14:30 hs" formatter
 *   introduced in scenario 12-VSP.
 * - `booking_terms` from the wire is intentionally NOT modelled
 *   here: it carries eleven platform-fee and deposit fields that
 *   are irrelevant for the Home / Mis Servicios surfaces and will
 *   be introduced together with the work-order detail screen
 *   (scenario 16-VSP).
 */
data class ServiceProposal(
    val id: String,
    val conversationId: String?,
    val status: ServiceProposalStatus,
    val counterpart: ServiceProposalCounterpart,
    val description: String,
    val amountCents: Long,
    val scheduledOnEpochMillis: Long,
    val createdOnEpochMillis: Long,
)
