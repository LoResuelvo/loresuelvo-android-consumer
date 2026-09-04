package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the consumer's service proposals through the
 * [ServiceProposalRepository] port without applying any status
 * filter. Powers the "Mis Servicios" surface (US-54 scenario
 * 03-VSP) which lists every proposal regardless of status so the
 * consumer can see the full history; the per-status filter
 * chips on top of that list (scenarios 05-VSP / 06-VSP / 07-VSP)
 * are a presentation concern.
 *
 * Stateless; failures are propagated verbatim. The use case never
 * swallows a [ServiceProposalsOutcome.Failure] into an empty
 * `Success` — the VM needs the typed failure to decide whether
 * to render an error or stay on a cached list.
 */
@Singleton
class GetAllServiceProposalsUseCase @Inject constructor(
    private val repository: ServiceProposalRepository,
) {
    suspend operator fun invoke(): ServiceProposalsOutcome =
        repository.getServiceProposals()
}
