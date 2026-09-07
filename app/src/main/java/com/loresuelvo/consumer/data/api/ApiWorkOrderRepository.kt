package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrder
import com.loresuelvo.consumer.domain.workorder.WorkOrderRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter that fulfils [WorkOrderRepository] by reusing the
 * existing [ServiceProposalRepository] round trip. Today the
 * work order is just the originating proposal flattened — the
 * dedicated `GET /work-orders/by-proposal/{id}` endpoint does
 * not yet exist on the backend, so a single repository call
 * feeds both surfaces.
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
class ApiWorkOrderRepository @Inject constructor(
    private val serviceProposalRepository: ServiceProposalRepository,
) : WorkOrderRepository {

    override suspend fun getWorkOrderByProposalId(proposalId: String): GetWorkOrderOutcome =
        when (val outcome = serviceProposalRepository.getServiceProposals()) {
            is ServiceProposalsOutcome.Success ->
                outcome.proposals
                    .firstOrNull { it.id == proposalId }
                    ?.toWorkOrder()
                    ?.let { GetWorkOrderOutcome.Found(it) }
                    ?: GetWorkOrderOutcome.NotFound
            is ServiceProposalsOutcome.Failure ->
                GetWorkOrderOutcome.Failure(outcome)
        }
}

private fun ServiceProposal.toWorkOrder(): WorkOrder = WorkOrder(
    proposalId = id,
    providerName = "${counterpart.name} ${counterpart.surname}",
    categoryName = counterpart.categoryName,
    description = description,
    amountCents = amountCents,
    scheduledOnEpochMillis = scheduledOnEpochMillis,
    estimatedDurationMinutes = estimatedDurationMinutes,
    status = status,
)

/**
 * Public façade for the production [toWorkOrder] mapping so JVM
 * unit tests can assert the conversion without re-implementing
 * it. The production site still calls the private extension
 * directly — the façade exists only to make the mapping
 * testable from `src/test`.
 */
object ApiWorkOrderMapping {
    fun toWorkOrder(proposal: ServiceProposal): WorkOrder = proposal.toWorkOrder()
}