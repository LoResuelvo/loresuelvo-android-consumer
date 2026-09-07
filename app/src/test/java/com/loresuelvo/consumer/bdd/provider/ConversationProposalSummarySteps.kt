package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.ui.screens.chat.ConversationProposalSummaryUiState
import com.loresuelvo.consumer.ui.util.ScheduledDateFormatter
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Real step implementations for the US-54 BDD spec
 * `14-VSP Consultar el resumen de la propuesta desde la
 * conversación`. The [ConversationProposalSummaryWorld] drives
 * the [com.loresuelvo.consumer.ui.screens.chat.ConversationProposalSummaryViewModel]
 * with a fake repo whose only proposal is linked to the seeded
 * `conversationId`, so the four pinned fields the scenario asserts
 * (monto, fecha, descripción, estado) are observable end-to-end
 * without depending on Hilt, Compose, or a backend.
 */
class ConversationProposalSummarySteps {

    private val world: ConversationProposalSummaryWorld = ConversationProposalSummaryWorld()

    // ---- Scenario 14-VSP --------------------------------------

    /**
     * "que existe una conversación relacionada con una propuesta
     * de servicio" — scenario 14-VSP. The world seeds a single
     * proposal whose `conversationId = "1000"` so the VM's
     * `load("1000")` round trip lands on the proposal.
     */
    @Given("que existe una conversación relacionada con una propuesta de servicio")
    fun queExisteUnaConversacionRelacionadaConUnaPropuestaDeServicio() {
        world.startScenario()
        world.seedProposalLinkedToConversation()
    }

    /**
     * "el usuario accede a la conversación" — scenario 14-VSP.
     * Drives the VM's `load(conversationId)` so the `Then`
     * assertions can observe the resolved state.
     */
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