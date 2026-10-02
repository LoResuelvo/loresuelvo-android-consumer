package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosUiState
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class MisServiciosSteps {

    private val world: MisServiciosWorld = MisServiciosWorld()


    @When("accede a Mis Servicios")
    fun accedeAMisServicios() {
        // The Background already started the Home world; this
        // step mounts the MisServiciosViewModel against the
        // seeded repo so the Then assertion observes its state.
        // The seed mirrors the Home world's seed (mixed statuses)
        world.startScenario()
        world.seedProposalsReceived()
        world.openMisServicios()
    }

    @Then("debe visualizar todas sus propuestas de servicio")
    fun debeVisualizarTodasSusPropuestasDeServicio() {
        val state = world.lastUiState()
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val items = (state as MisServiciosUiState.Ready).proposals
        assertEquals(
            "expected the GetAll use case to surface every seeded proposal " +
                "without filtering by status",
            listOf("10", "11", "12"),
            items.map { it.id },
        )
        // Pin the explicit breadth: every status is present in
        // the rendered list (the screen-level filter chips on
        val statuses = items.map { it.status }.toSet()
        assertEquals(
            "expected the seeded mixed-status set, was $statuses",
            setOf(
                ServiceProposalStatus.Pending,
                ServiceProposalStatus.Accepted,
                ServiceProposalStatus.Rejected,
            ),
            statuses,
        )
    }


    @Given("que el usuario tiene varias propuestas de servicio con fechas distintas")
    fun queElUsuarioTieneVariasPropuestasDeServicioConFechasDistintas() {
        // Seed three proposals whose `createdOnEpochMillis` is
        // intentionally NOT in insertion order so the use case
        // sort is observable in the resulting `Ready.items`.
        // The "When" step that follows reuses the same
        // `accedeAMisServicios` as 03-VSP, which re-mounts the
        // VM against the latest seed.
        world.startScenario()
        world.seedProposalsWithDistinctDates()
        world.openMisServicios()
    }

    @Then("debe visualizar primero la propuesta más reciente")
    fun debeVisualizarPrimeroLaPropuestaMasReciente() {
        val state = world.lastUiState()
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val items = (state as MisServiciosUiState.Ready).proposals
        assertEquals(
            "expected the GetAll use case to surface the newest " +
                "proposal first (sorted by createdOnEpochMillis " +
                "descending); the seed inserted id=\"30\" first but " +
                "it carries the largest createdOnEpochMillis",
            listOf("30", "31", "32"),
            items.map { it.id },
        )
    }


    @Given("que el usuario tiene propuestas en diferentes estados")
    fun queElUsuarioTienePropuestasEnDiferentesEstados() {
        // The Background step (`seedProposalsReceived`) already
        // wires a mixed-status seed (1 Pending, 1 Accepted, 1
        // Rejected) into the Home world. Re-mount the MisServicios
        // VM against that seed so this Given reads as "the
        // consumer has proposals of every status available".
        world.startScenario()
        world.seedProposalsReceived()
        world.openMisServicios()
    }

    @When("selecciona el filtro de propuestas que requieren su atención")
    fun seleccionaElFiltroDePropuestasQueRequierenSuAtencion() {
        // The "requieren atención" wording maps to the Pending
        // through `GetPendingServiceProposalsUseCase`.
        world.selectFilter(ServiceProposalStatus.Pending)
    }

    @Then("debe visualizar únicamente las propuestas pendientes")
    fun debeVisualizarUnicamenteLasPropuestasPendientes() {
        val state = world.lastUiState()
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val ready = state as MisServiciosUiState.Ready
        assertEquals(
            "expected the filter chip to land on Pending after the user tapped it",
            ServiceProposalStatus.Pending,
            ready.selectedStatusFilter,
        )
        assertEquals(
            "expected the GetPending use case to keep only the Pending entries " +
                "from the mixed seed (id=\"10\")",
            listOf("10"),
            ready.proposals.map { it.id },
        )
        assertTrue(
            "every visible proposal must be Pending, was ${ready.proposals.map { it.status }}",
            ready.proposals.all { it.status == ServiceProposalStatus.Pending },
        )
    }


    @When("selecciona el filtro de propuestas aceptadas")
    fun seleccionaElFiltroDePropuestasAceptadas() {
        world.selectFilter(ServiceProposalStatus.Accepted)
    }

    @Then("debe visualizar únicamente las propuestas aceptadas")
    fun debeVisualizarUnicamenteLasPropuestasAceptadas() {
        val state = world.lastUiState()
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val ready = state as MisServiciosUiState.Ready
        assertEquals(
            "expected the filter chip to land on Accepted after the user tapped it",
            ServiceProposalStatus.Accepted,
            ready.selectedStatusFilter,
        )
        assertEquals(
            "expected the GetAccepted use case to keep only the Accepted entries " +
                "from the mixed seed (id=\"11\")",
            listOf("11"),
            ready.proposals.map { it.id },
        )
        assertTrue(
            "every visible proposal must be Accepted, was ${ready.proposals.map { it.status }}",
            ready.proposals.all { it.status == ServiceProposalStatus.Accepted },
        )
    }


    @When("selecciona el filtro de propuestas rechazadas")
    fun seleccionaElFiltroDePropuestasRechazadas() {
        world.selectFilter(ServiceProposalStatus.Rejected)
    }

    @Then("debe visualizar únicamente las propuestas rechazadas")
    fun debeVisualizarUnicamenteLasPropuestasRechazadas() {
        val state = world.lastUiState()
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val ready = state as MisServiciosUiState.Ready
        assertEquals(
            "expected the filter chip to land on Rejected after the user tapped it",
            ServiceProposalStatus.Rejected,
            ready.selectedStatusFilter,
        )
        assertEquals(
            "expected the GetRejected use case to keep only the Rejected entries " +
                "from the mixed seed (id=\"12\")",
            listOf("12"),
            ready.proposals.map { it.id },
        )
        assertTrue(
            "every visible proposal must be Rejected, was ${ready.proposals.map { it.status }}",
            ready.proposals.all { it.status == ServiceProposalStatus.Rejected },
        )
    }


    @Given("que el usuario no tiene propuestas de servicio")
    fun queElUsuarioNoTienePropuestasDeServicio() {
        world.startScenario()
        world.seedProposalsEmpty()
    }

    @When("accede a la sección correspondiente")
    fun accedeALaSeccionCorrespondiente() {
        world.openMisServicios()
    }

    @Then("debe visualizar un mensaje indicando que no tiene propuestas para mostrar")
    fun debeVisualizarUnMensajeIndicandoQueNoTienePropuestasParaMostrar() {
        val state = world.lastUiState()
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val ready = state as MisServiciosUiState.Ready
        assertEquals(
            "expected the empty seed to produce an empty proposals list, " +
                "was ${ready.proposals.map { it.id }}",
            emptyList<String>(),
            ready.proposals.map { it.id },
        )
    }


    @And("no tiene propuestas correspondientes al estado seleccionado")
    fun noTienePropuestasCorrespondientesAlEstadoSeleccionado() {
        world.startScenario()
        world.seedProposalsPendingAndAcceptedOnly()
    }

    @When("selecciona dicho estado")
    fun seleccionaDichoEstado() {
        world.selectFilter(ServiceProposalStatus.Rejected)
    }

    @Then("debe visualizar un mensaje indicando que no hay propuestas para mostrar")
    fun debeVisualizarUnMensajeIndicandoQueNoHayPropuestasParaMostrar() {
        val state = world.lastUiState()
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val ready = state as MisServiciosUiState.Ready
        assertEquals(
            "expected the empty filter to produce an empty proposals list, " +
                "was ${ready.proposals.map { it.id }}",
            emptyList<String>(),
            ready.proposals.map { it.id },
        )
        assertEquals(
            "expected the filter chip to land on Rejected after the user tapped it",
            ServiceProposalStatus.Rejected,
            ready.selectedStatusFilter,
        )
    }


    @Given("que el prestador tiene una foto de perfil")
    fun queElPrestadorTieneUnaFotoDePerfil() {
        world.startScenario()
        world.seedProposalWithPhoto()
    }

    @When("el usuario consulta el detalle de su propuesta")
    fun elUsuarioConsultaElDetalleDeSuPropuesta() {
        world.openProposalDetail("40")
    }

    @Then("debe visualizar la foto de perfil del prestador")
    fun debeVisualizarLaFotoDePerfilDelPrestador() {
        val detail = world.lastDetailState()
        assertTrue(
            "expected ProposalDetailUiState.Ready, was $detail",
            detail is com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready,
        )
        val proposal = (detail as com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready).proposal
        assertEquals(
            "expected the seeded counterpart to carry a profilePhotoUrl, " +
                "was ${proposal.counterpart.profilePhotoUrl}",
            "https://cdn.loresuelvo.test/providers/400/avatar.jpg",
            proposal.counterpart.profilePhotoUrl,
        )
    }


    @Given("que el prestador no tiene una foto de perfil")
    fun queElPrestadorNoTieneUnaFotoDePerfil() {
        world.startScenario()
        world.seedProposalWithoutPhoto()
    }

    @Then("debe visualizar una imagen predeterminada")
    fun debeVisualizarUnaImagenPredeterminada() {
        val detail = world.lastDetailState()
        assertTrue(
            "expected ProposalDetailUiState.Ready, was $detail",
            detail is com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready,
        )
        val proposal = (detail as com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready).proposal
        assertEquals(
            "expected the seeded counterpart to carry a null profilePhotoUrl " +
                "so the screen can fall back to the placeholder avatar, " +
                "was ${proposal.counterpart.profilePhotoUrl}",
            null,
            proposal.counterpart.profilePhotoUrl,
        )
    }


    @Given("que existe una propuesta de servicio con una duración estimada de {string}")
    fun queExisteUnaPropuestaDeServicioConUnaDuracionEstimadaDe(duracion: String) {
        world.startScenario()
        world.seedProposalWithEstimatedDuration(world.parseDurationMinutes(duracion))
    }

    @Then("debe visualizar la duración estimada como {string}")
    fun debeVisualizarLaDuracionEstimadaComo(expectedFormatted: String) {
        val detail = world.lastDetailState()
        assertTrue(
            "expected ProposalDetailUiState.Ready, was $detail",
            detail is com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready,
        )
        val proposal = (detail as com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready).proposal
        val minutes = proposal.estimatedDurationMinutes
        assertTrue(
            "expected the proposal to carry an estimatedDurationMinutes " +
                "so the duration row renders, was $minutes",
            minutes != null,
        )
        assertEquals(
            "expected the EstimatedDurationFormatter to render the duration " +
                "as the scenario pins, was ${com.loresuelvo.consumer.ui.util.EstimatedDurationFormatter.formatDuration(minutes!!)}",
            expectedFormatted,
            com.loresuelvo.consumer.ui.util.EstimatedDurationFormatter.formatDuration(minutes),
        )
    }


    @Given("que existe una propuesta de servicio para el 15 de octubre de 2026 a las 14:30")
    fun queExisteUnaPropuestaDeServicioParaEl15DeOctubreDe2026ALas1430() {
        world.startScenario()
        world.seedProposalScheduledForOctober15At1430()
    }

    @Then("debe visualizar la fecha y hora como {string}")
    fun debeVisualizarLaFechaYHoraComo(expectedFormatted: String) {
        val detail = world.lastDetailState()
        assertTrue(
            "expected ProposalDetailUiState.Ready, was $detail",
            detail is com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready,
        )
        val proposal = (detail as com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready).proposal
        assertEquals(
            "expected the ScheduledDateFormatter to render the scheduled date " +
                "as the scenario pins, was ${com.loresuelvo.consumer.ui.util.ScheduledDateFormatter.formatScheduled(proposal.scheduledOnEpochMillis)}",
            expectedFormatted,
            com.loresuelvo.consumer.ui.util.ScheduledDateFormatter.formatScheduled(proposal.scheduledOnEpochMillis),
        )
    }


    @Given("que existe una propuesta de servicio por un monto de 15000 pesos")
    fun queExisteUnaPropuestaDeServicioPorUnMontoDe15000Pesos() {
        world.startScenario()
        world.seedProposalWithFifteenThousandPesos()
    }

    @Then("debe visualizar el monto como {string}")
    fun debeVisualizarElMontoComo(expectedFormatted: String) {
        val detail = world.lastDetailState()
        assertTrue(
            "expected ProposalDetailUiState.Ready, was $detail",
            detail is com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready,
        )
        val proposal = (detail as com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready).proposal
        assertEquals(
            "expected the CurrencyFormatter to round 15000 pesos as the " +
                "scenario pins, was ${com.loresuelvo.consumer.ui.util.CurrencyFormatter.formatAmount(proposal.amountCents)}",
            expectedFormatted,
            com.loresuelvo.consumer.ui.util.CurrencyFormatter.formatAmount(proposal.amountCents),
        )
    }


    @Given("que el usuario está consultando una propuesta de servicio")
    fun queElUsuarioEstaConsultandoUnaPropuestaDeServicio() {
        world.startScenario()
        world.seedProposalsReceived()
        world.openProposalDetail("10")
    }

    @When("^selecciona \"Ver conversación\"$")
    fun seleccionaVerConversacion() {
        world.tapViewConversation()
    }

    @Then("debe acceder a la conversación relacionada con la propuesta")
    fun debeAccederALaConversacionRelacionadaConLaPropuesta() {
        val call = world.lastViewConversationCall()
        assertEquals(
            "expected the 'Ver conversación' CTA to fire with the proposal's " +
                "conversationId (\"1000\"), was $call",
            "1000",
            call,
        )
    }


    @Given("que existe una propuesta de servicio")
    fun queExisteUnaPropuestaDeServicio() {
        // The Background step already wired a mixed seed (10/11/12).
        // Re-mount the MisServicios VM against that seed so this
        // Given reads as "the proposal exists".
        world.startScenario()
        world.seedProposalsReceived()
        world.openMisServicios()
    }

    @When("el usuario accede a su detalle")
    fun elUsuarioAccedeASuDetalle() {
        // the detail" maps to "the host dispatches the chosen
        // proposalId into the detail VM" — the modal bottom sheet
        // is exercised by the Compose instrumented test.
        world.openProposalDetail("10")
    }

    @Then("debe visualizar el nombre completo del prestador")
    fun debeVisualizarElNombreCompletoDelPrestador() {
        val detail = world.lastDetailState()
        assertTrue(
            "expected ProposalDetailUiState.Ready, was $detail",
            detail is com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready,
        )
        val proposal = (detail as com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready).proposal
        assertEquals(
            "expected the seeded counterpart name (id=\"10\" uses Carlos López)",
            "Carlos López",
            "${proposal.counterpart.name} ${proposal.counterpart.surname}",
        )
    }

    @Then("debe visualizar el rubro del prestador")
    fun debeVisualizarElRubroDelPrestador() {
        val detail = world.lastDetailState()
        val proposal = (detail as com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready).proposal
        assertEquals("Plomería", proposal.counterpart.categoryName)
    }

    @Then("debe visualizar el motivo de la visita")
    fun debeVisualizarElMotivoDeLaVisita() {
        val detail = world.lastDetailState()
        val proposal = (detail as com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready).proposal
        assertEquals("Fuga en el lavamanos", proposal.description)
    }

    @Then("debe visualizar el monto acordado")
    fun debeVisualizarElMontoAcordado() {
        val detail = world.lastDetailState()
        val proposal = (detail as com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready).proposal
        assertEquals(1500000L, proposal.amountCents)
    }

    @Then("debe visualizar el estado actual de la propuesta")
    fun debeVisualizarElEstadoActualDeLaPropuesta() {
        val detail = world.lastDetailState()
        val proposal = (detail as com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready).proposal
        assertEquals(ServiceProposalStatus.Pending, proposal.status)
    }
}
