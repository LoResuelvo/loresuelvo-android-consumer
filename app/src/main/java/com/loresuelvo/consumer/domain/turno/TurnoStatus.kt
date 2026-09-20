package com.loresuelvo.consumer.domain.turno

/**
 * Lifecycle of a "turno" (scheduled appointment) between the
 * consumer and the provider.
 *
 * Landed minimally for scenario 02-VT: only the [Confirmed]
 * branch is mapped today (the dev backend's `GET /work-orders`
 * emits `status: "scheduled"` — validated 2026-09). The other
 * three states land in the mapper alongside scenarios 06-VT..
 * 09-VT (or sooner if backend widens the wire contract).
 *
 * Mirrors the backend wire enum:
 *  - `scheduled` → [Confirmed] (the only wire value known
 *    today)
 *  - `pending`   → [Pending]   (reserved)
 *  - `finished`  → [Finished]  (reserved)
 *  - `cancelled` → [Cancelled] (reserved)
 */
enum class TurnoStatus { Pending, Confirmed, Finished, Cancelled }
