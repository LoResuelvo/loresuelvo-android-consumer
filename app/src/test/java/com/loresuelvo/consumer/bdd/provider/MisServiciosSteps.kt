package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosUiState
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Real step implementations for the US-54 "Mis Servicios" BDD
 * specs in `features/provider/visualize-service-proposal.feature`.
 *
 * Today only scenario 03-VSP is green ("ver todas las propuestas
 * en Mis Servicios"). Each subsequent scenario — order
 * chronologically (04-VSP), filter by status (05-VSP / 06-VSP /
 * 07-VSP) — will gain its step defs here as it lands.
 *
 * The Background steps ("sesión iniciada", "propuestas recibidas")
 * live on [VisualizeServiceProposalSteps] and seed the Home world.
 * That world is the one driving the Home dashboard scenarios
 * (01-VSP / 02-VSP); MisServicios has its own world seeded from
 * the `When` step below so we avoid cross-world coupling and
 * duplicate step definitions against the Background glue.
 *
 * At the JVM BDD layer "el usuario accede a Mis Servicios" maps
 * to "the MisServiciosViewModel mounts against the seeded repo";
 * the navigation graph itself is exercised by the
 * `MisServiciosScreenInstrumentedTest` on a real device.
 */
class MisServiciosSteps {

    private val world: MisServiciosWorld = MisServiciosWorld()

    // ---- Scenario 03-VSP --------------------------------------

    @When("accede a Mis Servicios")
    fun accedeAMisServicios() {
        // The Background already started the Home world; this
        // step mounts the MisServiciosViewModel against the
        // seeded repo so the Then assertion observes its state.
        // The seed mirrors the Home world's seed (mixed statuses)
        // because scenario 03-VSP asserts every status is present.
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
        // top of this list are scenarios 05-VSP / 06-VSP / 07-VSP).
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

    // ---- Scenario 04-VSP --------------------------------------

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

    // ---- Scenario 05-VSP --------------------------------------

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
        // status filter (US-54 scenario 05-VSP). The VM routes
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

    // ---- Scenario 06-VSP --------------------------------------

    /**
     * "selecciona el filtro de propuestas aceptadas" — scenario 06-VSP.
     * Routes through `GetAcceptedServiceProposalsUseCase`; the fake
     * repo's mixed-status seed (set up by [seedProposalsReceived])
     * carries exactly one `Accepted` entry (`id="11"`) so the
     * narrower result is observable.
     */
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

    // ---- Scenario 07-VSP --------------------------------------

    /**
     * "selecciona el filtro de propuestas rechazadas" — scenario 07-VSP.
     * Routes through `GetRejectedServiceProposalsUseCase`; the fake
     * repo's mixed-status seed (set up by [seedProposalsReceived])
     * carries exactly one `Rejected` entry (`id="12"`) so the
     * narrower result is observable.
     */
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

    // ---- Scenario 17-VSP --------------------------------------

    /**
     * "que el usuario no tiene propuestas de servicio" — scenario 17-VSP.
     * Seeds an empty list into the fake repo so the screen renders
     * its empty-state copy. The VM is started (and `load()` fired)
     * lazily from the `When` step so the `init` block doesn't run
     * against the default empty seed before the explicit empty
     * seed is in place.
     */
    @Given("que el usuario no tiene propuestas de servicio")
    fun queElUsuarioNoTienePropuestasDeServicio() {
        world.startScenario()
        world.seedProposalsEmpty()
    }

    /**
     * "accede a la sección correspondiente" — scenario 17-VSP. The
     * MisServiciosViewModel is already mounted by [startScenario];
     * this step is a no-op at the VM level and exists so the
     * Gherkin flow reads naturally.
     */
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

    // ---- Scenario 18-VSP --------------------------------------

    /**
     * "no tiene propuestas correspondientes al estado seleccionado" —
     * scenario 18-VSP. Seeds two Pending + one Accepted with zero
     * Rejected so the next filter tap on `Rejected` lands on an
     * empty result.
     */
    @And("no tiene propuestas correspondientes al estado seleccionado")
    fun noTienePropuestasCorrespondientesAlEstadoSeleccionado() {
        world.startScenario()
        world.seedProposalsPendingAndAcceptedOnly()
    }

    /**
     * "selecciona dicho estado" — scenario 18-VSP. The Gherkin
     * text is intentionally generic because the `And` step does
     * not pin a status. The seed has zero `Rejected` proposals, so
     * the chip tap below drives the only filter for which the
     * narrowed result is empty. If a future scenario needs a
     * different status here, the seed and the chip should match.
     */
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

    // ---- Scenario 09-VSP --------------------------------------

    /**
     * "que el prestador tiene una foto de perfil" — scenario 09-VSP.
     * Seeds a single proposal whose counterpart carries a
     * non-null `profilePhotoUrl`. The detail VM resolves the
     * proposal into `Ready(proposal)` so the `Then` step can read
     * the URL straight off the state.
     */
    @Given("que el prestador tiene una foto de perfil")
    fun queElPrestadorTieneUnaFotoDePerfil() {
        world.startScenario()
        world.seedProposalWithPhoto()
    }

    /**
     * "el usuario consulta el detalle de su propuesta" — scenario 09-VSP.
     * At the JVM BDD layer "querying the detail" maps to feeding
     * the detail VM with the chosen proposalId; the modal bottom
     * sheet itself is exercised by the Compose instrumented test.
     */
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

    // ---- Scenario 10-VSP --------------------------------------

    /**
     * "que el prestador no tiene una foto de perfil" — scenario 10-VSP.
     * Seeds a single proposal whose counterpart carries a `null`
     * `profilePhotoUrl`. The detail VM resolves the proposal into
     * `Ready(proposal)` so the `Then` step can pin the null URL
     * that backs the avatar fallback.
     */
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

    // ---- Scenario 13-VSP --------------------------------------

    /**
     * "que el usuario está consultando una propuesta de servicio" —
     * scenario 13-VSP. The consumer has tapped the proposal card
     * and the detail VM is in `Ready`. Reuses the standard seed
     * (id="10", conversationId="1000") so the conversation id
     * surfaced by the CTA is deterministic.
     */
    @Given("que el usuario está consultando una propuesta de servicio")
    fun queElUsuarioEstaConsultandoUnaPropuestaDeServicio() {
        world.startScenario()
        world.seedProposalsReceived()
        world.openProposalDetail("10")
    }

    /**
     * "selecciona 'Ver conversación'" — scenario 13-VSP. Mirrors
     * the production [ProposalDetailScreen] behaviour: when the
     * consumer taps the CTA, the screen fires `onViewConversation`
     * with the proposal's `conversationId`. The world captures
     * the invocation so the `Then` step can pin the navigation
     * intent.
     */
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

    // ---- Scenario 08-VSP --------------------------------------

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
        // US-54 scenario 08-VSP: at the JVM BDD layer "accessing
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
