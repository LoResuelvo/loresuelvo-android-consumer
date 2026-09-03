package com.loresuelvo.consumer.domain.serviceproposal

/**
 * Lifecycle status of a service proposal sent by a provider to a
 * consumer. The backend exposes the same vocabulary lowercase via
 * `GET /service-proposals` (`"pending"`, `"accepted"`,
 * `"rejected"`); the mapper in
 * `data/api/mapper/ServiceProposalDtoMapper.kt` normalises the
 * wire value into one of these three cases (unknown statuses are
 * dropped from the list rather than coerced to a fourth value).
 *
 * - [Pending]: the provider's offer still requires the consumer's
 *   action; the Home dashboard surfaces these as
 *   "propuestas que requieren atención".
 * - [Accepted]: the consumer accepted the proposal; it becomes
 *   a scheduled work order (US-54 AC: "trabajos próximos a
 *   realizarse").
 * - [Rejected]: the consumer turned the proposal down.
 */
enum class ServiceProposalStatus { Pending, Accepted, Rejected }
