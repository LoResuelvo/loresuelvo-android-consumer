package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.ui.screens.chat.ConversationProposalSummaryUiState
import com.loresuelvo.consumer.ui.util.ScheduledDateFormatter
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ConversationProposalSummarySteps {

    private val world: ConversationProposalSummaryWorld = ConversationProposalSummaryWorld()


    @Given("que existe una conversación relacionada con una propuesta de servicio")
    fun queExisteUnaConversacionRelacionadaConUnaPropuestaDeServicio() {
        world.startScenario()
        world.seedProposalLinkedToConversation()
    }

    @When("el usuario accede a la conversación")
    fun elUsuarioAccedeALaConversacion() {
        world.openConversation()
    }

    @Then("debe visualizar un resumen de la propuesta")
    fun debeVisualizarUnResumenDeLaPropuesta() {
        val state = world.lastProposalSummaryState()
        assertTrue(
            "expected ConversationProposalSummaryUiState.Ready, was $state",
            state is ConversationProposalSummaryUiState.Ready,
        )
    }

    @Then("debe visualizar el monto acordado en el resumen de la conversación")
    fun debeVisualizarElMontoAcordadoEnElResumenDeLaConversacion() {
        val state = world.lastProposalSummaryState()
        assertTrue(
            "expected Ready, was $state",
            state is ConversationProposalSummaryUiState.Ready,
        )
        val proposal = (state as ConversationProposalSummaryUiState.Ready).proposal
        assertEquals(
            "expected the seeded proposal to carry 4_200_000 cents, " +
                "was ${proposal.amountCents}",
            4_200_000L,
            proposal.amountCents,
        )
    }

    @Then("debe visualizar la fecha acordada en el resumen de la conversación")
    fun debeVisualizarLaFechaAcordadaEnElResumenDeLaConversacion() {
        val state = world.lastProposalSummaryState()
        assertTrue(
            "expected Ready, was $state",
            state is ConversationProposalSummaryUiState.Ready,
        )
        val proposal = (state as ConversationProposalSummaryUiState.Ready).proposal
        assertEquals(
            "expected the ScheduledDateFormatter to render the scheduled date, " +
                "was ${ScheduledDateFormatter.formatScheduled(proposal.scheduledOnEpochMillis)}",
            "15/10/2026 - 14:30 hs",
            ScheduledDateFormatter.formatScheduled(proposal.scheduledOnEpochMillis),
        )
    }

    @Then("debe visualizar la descripción del servicio en el resumen de la conversación")
    fun debeVisualizarLaDescripcionDelServicioEnElResumenDeLaConversacion() {
        val state = world.lastProposalSummaryState()
        assertTrue(
            "expected Ready, was $state",
            state is ConversationProposalSummaryUiState.Ready,
        )
        val proposal = (state as ConversationProposalSummaryUiState.Ready).proposal
        assertEquals(
            "expected the seeded proposal to carry the seeded description",
            "Pintura de living y comedor",
            proposal.description,
        )
    }

    @Then("debe visualizar el estado actual de la propuesta en el resumen de la conversación")
    fun debeVisualizarElEstadoActualDeLaPropuestaEnElResumenDeLaConversacion() {
        val state = world.lastProposalSummaryState()
        assertTrue(
            "expected Ready, was $state",
            state is ConversationProposalSummaryUiState.Ready,
        )
        val proposal = (state as ConversationProposalSummaryUiState.Ready).proposal
        assertEquals(
            "expected the seeded proposal to be Pending, was ${proposal.status}",
            ServiceProposalStatus.Pending,
            proposal.status,
        )
    }
}
