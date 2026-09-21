package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter that fulfils [WorkOrderDetailRepository] by reusing the
 * existing [ServiceProposalRepository] round trip. Today the
 * work order is just the originating proposal flattened — the
 * dedicated `GET /work-orders/{workOrderID}` endpoint is wired
 * up in a follow-up US-27 commit; until then this adapter
 * feeds the same data the proposal list does.
 *
 * Reusing the proposal repository avoids a second round trip on
 * every work-order detail mount, and keeps the wire shape
 * consistent across the consumer app. When the dedicated
 * endpoint lands, this adapter swaps `getServiceProposals()` for
 * the new call without changing the [GetWorkOrderOutcome]
 * surface that callers depend on.
 *
 * Implementation notes:
 *  - A `Failure(failure)` outcome collapses to `Failure(failure)`
 *    so the screen can render its retry CTA on backend errors.
 *  - A `Success(emptyList())` and `Success(...)` whose head
 *    doesn't match the id both surface as [GetWorkOrderOutcome.NotFound].
 *  - The result is **never thrown** — exceptions are caught
 *    inside the upstream repository, so this adapter is also
 *    exception-free.
 */
@Singleton
class ApiWorkOrderDetailRepository @Inject constructor(
    private val serviceProposalRepository: ServiceProposalRepository,
) : WorkOrderDetailRepository {

    override suspend fun getWorkOrderDetail(workOrderId: String): GetWorkOrderOutcome =
        when (val outcome = serviceProposalRepository.getServiceProposals()) {
            is ServiceProposalsOutcome.Success ->
                outcome.proposals
                    .firstOrNull { it.id == workOrderId }
                    ?.toWorkOrderDetail()
                    ?.let { GetWorkOrderOutcome.Found(it) }
                    ?: GetWorkOrderOutcome.NotFound
            is ServiceProposalsOutcome.Failure ->
                GetWorkOrderOutcome.Failure(outcome)
        }
}

private fun ServiceProposal.toWorkOrderDetail(): WorkOrderDetail = WorkOrderDetail(
    proposalId = id,
    providerName = "${counterpart.name} ${counterpart.surname}",
    categoryName = counterpart.categoryName,
    description = description,
    amountCents = amountCents,
    scheduledOnEpochMillis = scheduledOnEpochMillis,
    estimatedDurationMinutes = estimatedDurationMinutes,
    // US-27: map the proposal-side status to the work-order
    // vocabulary. `Accepted` is the only proposal-side value the
    // legacy adapter surfaces (only accepted proposals are
    // promoted to work orders today); the TurnoStatus enum
    // already has the appropriate name for it.
    status = when (status) {
        com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Accepted -> TurnoStatus.Confirmed
        com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Pending -> TurnoStatus.Pending
        com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Rejected -> TurnoStatus.Cancelled
    },
)

/**
 * Public façade for the production [toWorkOrderDetail] mapping so JVM
 * unit tests can assert the conversion without re-implementing
 * it. The production site still calls the private extension
 * directly — the façade exists only to make the mapping
 * testable from `src/test`.
 */
object ApiWorkOrderMapping {
    fun toWorkOrderDetail(proposal: ServiceProposal): WorkOrderDetail = proposal.toWorkOrderDetail()
}