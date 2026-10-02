package com.loresuelvo.consumer.ui.screens.misservicios

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MisServiciosUiStateTest {

    private fun proposal(id: String): ServiceProposal = ServiceProposal(
        id = id,
        conversationId = null,
        status = ServiceProposalStatus.Pending,
        counterpart = ServiceProposalCounterpart(
            id = "1",
            name = "Juan",
            surname = "Pérez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "irrelevant",
        amountCents = 15000L,
        scheduledOnEpochMillis = 1_700_000_000_000L,
        createdOnEpochMillis = 1_699_999_000_000L,
    )

    @Test
    fun ready_default_filter_is_null() {
        val state: MisServiciosUiState =
            MisServiciosUiState.Ready(proposals = listOf(proposal("1")))
        assertNull(state.selectedStatusFilter)
    }

    @Test
    fun ready_carries_explicit_filter() {
        val state: MisServiciosUiState =
            MisServiciosUiState.Ready(
                proposals = listOf(proposal("1")),
                selectedStatusFilter = ServiceProposalStatus.Pending,
            )
        assertEquals(ServiceProposalStatus.Pending, state.selectedStatusFilter)
    }

    @Test
    fun loading_default_filter_is_null() {
        val state: MisServiciosUiState = MisServiciosUiState.Loading
        assertNull(state.selectedStatusFilter)
    }

    @Test
    fun error_default_filter_is_null() {
        val state: MisServiciosUiState = MisServiciosUiState.Error(
            failure = ServiceProposalsOutcome.Failure.Network(IOException("dns")),
        )
        assertNull(state.selectedStatusFilter)
    }

    @Test
    fun error_carries_explicit_filter() {
        val state: MisServiciosUiState = MisServiciosUiState.Error(
            failure = ServiceProposalsOutcome.Failure.Server(code = 500, message = "boom"),
            selectedStatusFilter = ServiceProposalStatus.Accepted,
        )
        assertEquals(ServiceProposalStatus.Accepted, state.selectedStatusFilter)
    }
}
