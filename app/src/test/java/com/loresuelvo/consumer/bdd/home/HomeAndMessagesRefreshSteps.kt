package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.ui.screens.assistant.AssistantUiState
import com.loresuelvo.consumer.ui.screens.home.AiConversationsState
import com.loresuelvo.consumer.ui.screens.home.CategoriesState
import com.loresuelvo.consumer.ui.screens.home.HomeUiState
import com.loresuelvo.consumer.ui.screens.home.ServiceProposalsState
import com.loresuelvo.consumer.ui.screens.home.TurnosState
import com.loresuelvo.consumer.ui.screens.messages.MessagesListUiState
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class HomeAndMessagesRefreshSteps {

    private val world = HomeAndMessagesRefreshWorld()

    @After
    fun closeWorld() {
        world.close()
    }

    @Given("que estoy autenticado como consumidor")
    fun consumerIsAuthenticated() {
        world.startAuthenticatedSession()
    }

    @Given("que tengo cuatro sesiones con la IA ordenadas por {string}")
    fun haveFourAiSessionsOrderedByUpdatedOn(@Suppress("UNUSED_PARAMETER") field: String) {
        world.setAiConversations(
            listOf(
                world.aiConversation("old", "Pintura", 10L, "Pared vieja"),
                world.aiConversation("new", "Plomería", 40L, "Canilla rota"),
                world.aiConversation("middle", "Electricidad", 30L, "Cortocircuito"),
                world.aiConversation("third", "Gas", 20L, "Pérdida"),
            ),
        )
    }

    @When("abro la pantalla Home")
    fun openHome() {
        world.refreshRecentAiConversations()
    }

    @Then("la sección {string} muestra como máximo tres sesiones")
    fun recentSectionShowsAtMostThree(@Suppress("UNUSED_PARAMETER") section: String) {
        assertTrue(world.recentAiConversations().size <= 3)
    }

    @Then("las sesiones se muestran desde la más reciente hasta la más antigua")
    fun sessionsAreSortedByLatestActivity() {
        val sessions = world.recentAiConversations()
        assertEquals(listOf("new", "middle", "third"), sessions.map { it.id })
    }

    @Then("cada sesión muestra su título, preview del último mensaje y fecha")
    fun eachSessionHasTitlePreviewAndDate() {
        world.recentAiConversations().forEach { session ->
            assertTrue(session.title.isNotBlank())
            assertTrue(session.lastMessagePreview.orEmpty().isNotBlank())
            assertTrue(session.lastMessageAtEpochMillis > 0L)
        }
    }

    @Given("que tengo sesiones previas con la IA")
    fun havePreviousAiSessions() {
        world.setAiConversations(
            listOf(
                world.aiConversation("1", "Plomería", 20L, "Canilla"),
                world.aiConversation("2", "Electricidad", 10L, "Enchufe"),
                world.aiConversation("3", "Pintura", 5L, "Pared"),
                world.aiConversation("4", "Gas", 1L, "Pérdida"),
            ),
        )
        world.refreshRecentAiConversations()
    }

    @When("toco {string} en {string}")
    fun tapHomeLink(
        @Suppress("UNUSED_PARAMETER") link: String,
        @Suppress("UNUSED_PARAMETER") section: String,
    ) {
        world.openAssistantFromHome()
    }

    @Then("la aplicación navega a la pantalla {string}")
    fun appNavigatesTo(screen: String) {
        assertEquals(screen, world.currentRoute)
    }

    @Then("puedo consultar la lista completa de mis sesiones")
    fun canConsultFullSessionList() {
        assertEquals(4, world.assistantSessions().size)
    }

    @Given("que no tengo sesiones previas con la IA")
    fun haveNoAiSessions() {
        world.setAiConversations(emptyList())
        world.refreshRecentAiConversations()
    }

    @Then("veo el estado vacío de {string}")
    fun seeEmptyState(@Suppress("UNUSED_PARAMETER") section: String) {
        val state = world.homeState() as HomeUiState.Ready
        assertEquals(AiConversationsState.Ready(emptyList()), state.recentAiConversations)
    }

    @Then("puedo iniciar un nuevo diagnóstico con la IA")
    fun canStartNewDiagnosis() {
        world.startNewDiagnosis()
        assertEquals("Conversación IA", world.currentRoute)
    }

    @Given("que Home cargó una lista inicial de turnos y servicios")
    fun homeLoadedInitialTurnosAndServices() {
        world.refreshHome()
    }

    @Given("existe un turno o servicio nuevo en el backend")
    fun backendHasNewTurnoOrService() {
        world.setTurnos(TurnosOutcome.Success(listOf(sampleTurno("new-turno"))))
        world.setServiceProposals(ServiceProposalsOutcome.Success(emptyList()))
    }

    @When("regreso a Home desde {string} o {string}")
    fun returnToHomeFrom(
        @Suppress("UNUSED_PARAMETER") firstScreen: String,
        @Suppress("UNUSED_PARAMETER") secondScreen: String,
    ) {
        world.refreshHome()
    }

    @Then("Home vuelve a consultar los datos visibles")
    fun homeReloadsVisibleData() {
        val state = world.homeState() as HomeUiState.Ready
        assertTrue(state.turnos is TurnosState.Ready)
    }

    @Then("muestra el turno o servicio nuevo sin reiniciar la aplicación")
    fun homeShowsNewDataWithoutRestart() {
        val state = world.homeState() as HomeUiState.Ready
        assertEquals(
            listOf("new-turno"),
            (state.turnos as TurnosState.Ready).items.map { it.id },
        )
    }

    @Given("que Home inició dos cargas consecutivas de sus datos")
    fun homeStartedTwoConsecutiveLoads() {
        world.enqueueAiResponses(
            oldResponse = AiConversationListOutcome.Success(
                listOf(world.aiConversation("old", "Viejo", 1L, "Obsoleto")),
            ),
            latestResponse = AiConversationListOutcome.Success(
                listOf(world.aiConversation("latest", "Actual", 2L, "Vigente")),
            ),
        )
    }

    @When("la respuesta antigua llega después de la respuesta más reciente")
    fun oldResponseArrivesAfterLatest() {
        // The controlled repository completes the latest response first;
        // the request id guard decides which result is retained.
    }

    @Then("Home conserva los datos de la respuesta más reciente")
    fun homeKeepsLatestResponse() {
        assertEquals(listOf("latest"), world.recentAiConversations().map { it.id })
    }

    @Then("no vuelve a mostrar información obsoleta")
    fun homeDoesNotShowStaleInformation() {
        assertFalse(world.recentAiConversations().any { it.id == "old" })
    }

    @Given("que Home carga correctamente categorías y sesiones de IA")
    fun homeLoadsCategoriesAndAi() {
        world.setAiConversations(
            listOf(world.aiConversation("ai-1", "Diagnóstico", 1L, "Último mensaje")),
        )
        world.refreshHome()
    }

    @Given("falla la carga de turnos o servicios")
    fun turnosOrServicesLoadFails() {
        world.setTurnos(TurnosOutcome.Failure.Network(IOException("offline")))
        world.refreshTurnos()
    }

    @When("visualizo Home")
    fun viewHome() {
        // Home state is already loaded by the preceding Given steps.
    }

    @Then("continúo viendo las secciones que cargaron correctamente")
    fun successfulHomeSectionsRemainVisible() {
        val state = world.homeState() as HomeUiState.Ready
        assertTrue(state.categories is CategoriesState.Ready)
        assertTrue(state.recentAiConversations is AiConversationsState.Ready)
    }

    @Then("la sección fallida muestra su estado de error y una acción para reintentar")
    fun failedSectionExposesRetryableError() {
        val state = world.homeState() as HomeUiState.Ready
        assertEquals(TurnosState.Error, state.turnos)
        world.setTurnos(TurnosOutcome.Success(listOf(sampleTurno("recovered"))))
        world.refreshTurnos()
        assertTrue(world.homeState() is HomeUiState.Ready)
        assertTrue((world.homeState() as HomeUiState.Ready).turnos is TurnosState.Ready)
    }

    @Given("que tengo conversaciones con distintos prestadores")
    fun haveProviderConversations() {
        world.setConversations(
            listOf(
                world.conversation("juan", "Juan", "Pérez", "Necesito una reparación", 20L),
                world.conversation("ana", "Ana", "Gómez", "Presupuesto enviado", 10L),
            ),
        )
    }

    @Given("que tengo conversaciones con prestadores")
    fun haveConversationsWithProviders() {
        haveProviderConversations()
    }

    @When("abro la pantalla {string}")
    fun openScreen(@Suppress("UNUSED_PARAMETER") screen: String) {
        world.openMessages()
    }

    @Then("veo una barra de búsqueda en la parte superior")
    fun messagesSearchBarIsVisible() {
        assertTrue(world.messageState() is MessagesListUiState.Ready)
    }

    @Then("la barra permite buscar por nombre del prestador o contenido visible del último mensaje")
    fun messagesSearchSupportsProviderAndPreview() {
        world.filterMessages("reparación")
        val state = world.messageState() as MessagesListUiState.Ready
        assertEquals(listOf("juan"), state.conversations.map { it.id })
        world.filterMessages("Ana")
        assertEquals(listOf("ana"), (world.messageState() as MessagesListUiState.Ready).conversations.map { it.id })
    }

    @Given("que tengo conversaciones con {string} y {string}")
    fun haveNamedConversations(first: String, second: String) {
        world.setConversations(
            listOf(
                world.conversation("juan", first.substringBeforeLast(" "), first.substringAfterLast(" "), updatedOn = 20L),
                world.conversation("ana", second.substringBeforeLast(" "), second.substringAfterLast(" "), updatedOn = 10L),
            ),
        )
        world.openMessages()
    }

    @When("escribo {string} en la barra de búsqueda")
    fun typeSearchQuery(query: String) {
        world.filterMessages(query)
    }

    @Then("solo veo la conversación con {string}")
    fun onlyConversationWith(providerName: String) {
        val state = world.messageState() as MessagesListUiState.Ready
        assertEquals(1, state.conversations.size)
        assertEquals(providerName, "${state.conversations.single().counterpart.name} ${state.conversations.single().counterpart.surname}")
    }

    @Then("puedo limpiar la búsqueda para volver a ver todas las conversaciones")
    fun clearSearchRestoresAllConversations() {
        world.filterMessages("")
        assertEquals(2, (world.messageState() as MessagesListUiState.Ready).conversations.size)
    }

    @When("busco un nombre que no coincide con ninguna conversación")
    fun searchWithoutResults() {
        world.openMessages()
        world.filterMessages("Nadie")
    }

    @Then("veo un estado vacío específico para la búsqueda")
    fun searchEmptyStateIsVisible() {
        val state = world.messageState() as MessagesListUiState.Ready
        assertTrue(state.conversations.isEmpty())
        assertEquals(2, state.totalConversations)
    }

    @Then("puedo limpiar el texto para recuperar la lista completa")
    fun clearSearchRestoresCompleteList() {
        world.filterMessages("")
        assertEquals(2, (world.messageState() as MessagesListUiState.Ready).conversations.size)
    }

    @Given("que tengo al menos dos conversaciones con prestadores")
    fun haveAtLeastTwoProviderConversations() {
        haveProviderConversations()
        world.openMessages()
    }

    @When("visualizo la lista de {string}")
    fun viewMessagesList(@Suppress("UNUSED_PARAMETER") screen: String) {
        // The list is already materialized by openMessages().
    }

    @Then("cada conversación aparece en una fila independiente")
    fun eachConversationHasItsOwnRow() {
        assertEquals(2, (world.messageState() as MessagesListUiState.Ready).conversations.size)
    }

    @Then("se muestra una línea divisoria entre una conversación y la siguiente")
    fun dividerAppearsBetweenRows() {
        assertEquals(1, world.expectedDividerCount())
    }

    @Then("la línea no se muestra después de la última conversación")
    fun dividerDoesNotAppearAfterLastRow() {
        assertEquals(1, world.expectedDividerCount())
    }

    @Given("que estoy viendo la lista de {string}")
    fun viewingMessagesList(@Suppress("UNUSED_PARAMETER") screen: String) {
        haveProviderConversations()
        world.openMessages()
    }

    @Given("llega una conversación nueva o se actualiza el último mensaje de una existente")
    fun conversationListChanges() {
        world.setConversations(
            listOf(
                world.conversation("new", "Nuevo", "Prestador", "Mensaje nuevo", 30L),
                world.conversation("juan", "Juan", "Pérez", "Mensaje actualizado", 20L),
            ),
        )
    }

    @When("vuelvo a la sección {string}")
    fun returnToMessages(@Suppress("UNUSED_PARAMETER") section: String) {
        world.reloadMessages()
    }

    @Then("la lista refleja la conversación nueva o su preview actualizado")
    fun refreshedMessagesReflectNewData() {
        val state = world.messageState() as MessagesListUiState.Ready
        assertEquals(listOf("new", "juan"), state.conversations.map { it.id })
        assertEquals("Mensaje nuevo", state.conversations.first().lastMessage?.content)
    }

    @Then("conserva el orden definido por la fecha de actualización")
    fun refreshedMessagesKeepUpdatedOrder() {
        val state = world.messageState() as MessagesListUiState.Ready
        assertTrue(state.conversations.zipWithNext().all { (first, second) ->
            first.updatedOnEpochMillis >= second.updatedOnEpochMillis
        })
    }

    @Given("que Mensajes está cargando, vacío o muestra un error recuperable")
    fun messagesHasRecoverableStates() {
        world.exerciseMessageStatesAndNavigation()
    }

    @When("navego entre Home, Mensajes y una conversación")
    fun navigateAcrossHomeMessagesAndConversation() {
        // The world records the navigation while the VM transitions
        // through loading, empty, error and recovered states.
    }

    @Then("cada pantalla conserva su estado de loading, vacío o error correspondiente")
    fun eachScreenPreservesItsState() {
        assertTrue(world.routeHistory.containsAll(listOf("Home", "Mensajes", "Conversación")))
        assertTrue(world.messageState() is MessagesListUiState.Ready)
    }

    @Then("puedo reintentar sin quedar bloqueado en la navegación")
    fun canRetryWithoutBeingBlocked() {
        assertEquals("Mensajes", world.currentRoute)
        assertTrue((world.messageState() as MessagesListUiState.Ready).conversations.isNotEmpty())
    }

    private fun sampleTurno(id: String): Turno = Turno(
        id = id,
        serviceProposalId = "proposal-$id",
        status = TurnoStatus.Confirmed,
        counterpart = TurnoCounterpart(
            id = "provider-$id",
            name = "Juan",
            surname = "Pérez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "Reparación",
        amountCents = 100_000L,
        scheduledOnEpochMillis = 1_800_000_000_000L,
    )
}
