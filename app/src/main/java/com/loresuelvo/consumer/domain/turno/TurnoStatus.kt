package com.loresuelvo.consumer.domain.turno

/**
 * Lifecycle of a "turno" (work order) between the consumer and
 * the provider. Shared by the listing endpoint (`GET
 * /work-orders`) and the detail endpoint (`GET
 * /work-orders/{workOrderID}`, US-27 `visualize-turns-detail`).
 *
 * Wire mapping (validated 2026-09):
 *  - `scheduled`         → [Confirmed] (initial state, the
 *    appointment is on the calendar)
 *  - `pending`           → [Pending]   (reserved)
 *  - `awaiting_payment`  → [AwaitingPayment] (service done,
 *    consumer must clear the remaining balance — US-27
 *    scenarios 04-VTD / 09-VTD)
 *  - `paid`              → [Paid] (fully paid off, evidence +
 *    review available — US-27 scenarios 05-VTD / 07-VTD /
 *    08-VTD)
 *  - `finished`          → [Finished]  (reserved for legacy
 *    terminal state)
 *  - `cancelled`         → [Cancelled] (reserved)
 *
 * The list endpoint (`GET /work-orders`) currently emits only
 * `scheduled`; the detail endpoint widens with `awaiting_payment`
 * and `paid`. The badge in `TurnoCard` and the detail screen
 * render the value they receive.
 */
enum class TurnoStatus { Pending, Confirmed, AwaitingPayment, Paid, Finished, Cancelled }
