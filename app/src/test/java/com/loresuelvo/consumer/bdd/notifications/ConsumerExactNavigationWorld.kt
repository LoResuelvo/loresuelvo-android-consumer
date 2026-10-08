package com.loresuelvo.consumer.bdd.notifications

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.notifications.MessageNotification
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.ServiceNotification
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.payment.StartServiceProposalCheckoutUseCase
import com.loresuelvo.consumer.domain.usecase.payment.StartWorkOrderCheckoutUseCase
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.RateProviderUseCase
import com.loresuelvo.consumer.domain.workorder.CompletionReport
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.platform.notifications.MessageNotificationIntent
import com.loresuelvo.consumer.platform.notifications.MessageNotificationNavigation
import com.loresuelvo.consumer.platform.notifications.NavigationIntentDispatcher
import com.loresuelvo.consumer.platform.notifications.ServiceNotificationIntent
import com.loresuelvo.consumer.platform.notifications.ServiceNotificationNavigation
import com.loresuelvo.consumer.platform.notifications.VisibleConversationStore
import com.loresuelvo.consumer.ui.navigation.NavigationIntentViewModel
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.navigation.navigateFromNotification
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailViewModel
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailUiState
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.robolectric.Shadows
import java.util.concurrent.TimeUnit
import javax.inject.Provider

class ConsumerExactNavigationWorld {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sessionPreferences = fakePreferences()
    private val installationPreferences = fakePreferences()
    private val conversationRepository = mockk<ConversationRepository>(relaxed = true)
    private val proposalRepository = mockk<ServiceProposalRepository>(relaxed = true)
    private val checkoutRepository = mockk<CheckoutSessionRepository>(relaxed = true)
    private val turnosRepository = CurrentTurnosRepository()
    private val workOrderRepository = CurrentWorkOrderRepository()

    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var binding: InstallationBinding
    private lateinit var dispatcher: NavigationIntentDispatcher
    private lateinit var navigationIntents: NavigationIntentViewModel
    private lateinit var currentNoticeIntent: Intent
    private lateinit var currentConversation: ConversationDetail
    private lateinit var currentProposals: List<ServiceProposal>
    private var navController: TestNavHostController? = null
    private var navViewModelStore: ViewModelStore? = null
    private var navLifecycleOwner: NavigationLifecycleOwner? = null
    private var noticeKind: String = ""

    fun startNotice(aviso: String) {
        noticeKind = aviso
        sessions = EncryptedAuthSessionStore(sessionPreferences)
        sessions.saveSession(activeSession())
        installations = EncryptedInstallationStateStore(installationPreferences)
        binding = installations.prepare(17, "exact-nav-login")
        installations.confirm(binding)
        seedNoticeTimeData()
        configureCurrentRepositories()
        currentNoticeIntent = createNoticeIntent(aviso)
        createNavigationHost()
    }

    fun setAppState(estado: String) {
        when (estado) {
            "abierta" -> requireNotNull(navController)
            "cerrada" -> createNavigationHost()
            else -> error("Unknown app state: $estado")
        }
        refreshCurrentAccountData()
    }

    fun tapNotice() {
        val route = routeForNextDelivery()
        navController().navigateFromNotification(route)
    }

    fun assertDestination(destino: String) {
        when (destino) {
            "la conversación" -> assertCurrentConversation()
            "la propuesta" -> assertCurrentProposal()
            "la orden del turno", "la orden correspondiente" -> assertCurrentWorkOrder()
            else -> error("Unknown destination: $destino")
        }

        assertEquals(17, sessions.getSession()?.user?.backendUserId)
    }

    fun assertCanGoBackWithoutRepeatedScreens() {
        val controller = navController()
        val originalTarget = controller.currentBackStackEntry?.destination?.route
        val originalEntry = requireNotNull(controller.currentBackStackEntry)
        val originalWorkOrderId = originalEntry.arguments?.getString(Route.WorkOrderDetail.ARG_WORK_ORDER_ID)
        val originalProposalId = originalEntry.arguments?.getString(Route.MisServiciosProposal.ARG_PROPOSAL_ID)

        controller.navigateFromNotification(routeForNextDelivery())

        val repeatedEntry = requireNotNull(controller.currentBackStackEntry)
        assertEquals(originalTarget, repeatedEntry.destination.route)
        assertEquals(originalWorkOrderId, repeatedEntry.arguments?.getString(Route.WorkOrderDetail.ARG_WORK_ORDER_ID))
        assertEquals(originalProposalId, repeatedEntry.arguments?.getString(Route.MisServiciosProposal.ARG_PROPOSAL_ID))
        assertEquals(Route.Home.path, controller.previousBackStackEntry?.destination?.route)
        assertTrue("Back must return to Home", controller.popBackStack())
        assertEquals(Route.Home.path, controller.currentBackStackEntry?.destination?.route)
    }

    fun close() {
        navLifecycleOwner?.registry?.currentState = Lifecycle.State.DESTROYED
        navViewModelStore?.clear()
        navController = null
        navViewModelStore = null
        navLifecycleOwner = null
    }

    private fun createNavigationHost() {
        sessions = EncryptedAuthSessionStore(sessionPreferences)
        installations = EncryptedInstallationStateStore(installationPreferences)
        dispatcher = NavigationIntentDispatcher()
        navigationIntents = createNavigationIntentViewModel()
        navController = createNavController()
    }

    private fun createNavigationIntentViewModel(): NavigationIntentViewModel {
        val clock = NotificationClock { System.currentTimeMillis() }
        val messageNavigation = MessageNotificationNavigation(
            AuthorizeMessageNotificationUseCase(sessions, installations, clock),
        )
        val serviceNavigation = ServiceNotificationNavigation(
            AuthorizeServiceNotificationUseCase(sessions, installations, clock),
        )
        return NavigationIntentViewModel(
            dispatcher = dispatcher,
            conversationVisibility = VisibleConversationStore(),
            notifications = Provider { messageNavigation },
            serviceNotifications = Provider { serviceNavigation },
        )
    }

    private fun createNavController(): TestNavHostController {
        navViewModelStore = ViewModelStore()
        navLifecycleOwner = NavigationLifecycleOwner()
        return TestNavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setViewModelStore(requireNotNull(navViewModelStore))
            setLifecycleOwner(requireNotNull(navLifecycleOwner))
            graph = createGraph(startDestination = Route.Home.path) {
                composable(Route.Home.path) {}
                composable(
                    route = Route.Conversation("_id_").path,
                    arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
                ) {}
                composable(Route.MisServicios.path) {}
                composable(
                    route = Route.MisServiciosProposal.path,
                    arguments = listOf(
                        navArgument(Route.MisServiciosProposal.ARG_PROPOSAL_ID) {
                            type = NavType.StringType
                        },
                    ),
                ) {}
                composable(
                    route = Route.WorkOrderDetail.path,
                    arguments = workOrderRouteArguments(),
                ) {}
            }
        }
    }

    private fun workOrderRouteArguments() = listOf(
        navArgument(Route.WorkOrderDetail.ARG_WORK_ORDER_ID) { type = NavType.StringType },
        navArgument(Route.WorkOrderDetail.ARG_PROVIDER_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Route.WorkOrderDetail.ARG_PROVIDER_NAME) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Route.WorkOrderDetail.ARG_PROVIDER_SURNAME) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Route.WorkOrderDetail.ARG_PROVIDER_CATEGORY_NAME) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Route.WorkOrderDetail.ARG_PROVIDER_PROFILE_PHOTO_URL) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    )

    private fun routeForNextDelivery(): String {
        dispatcher.dispatch(currentNoticeIntent)
        val deliveredIntent = runBlocking { dispatcher.events.first() }
        return requireNotNull(navigationIntents.routeFor(deliveredIntent)) {
            "Notice intent must resolve to an authenticated destination"
        }
    }

    private fun createNoticeIntent(aviso: String): Intent {
        val expiresAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
        return when (aviso) {
            "nuevo mensaje" -> MessageNotificationIntent().create(
                context,
                MessageNotification(
                    version = "1",
                    eventId = "message:42:17",
                    type = "conversation.message.created",
                    resourceType = "conversation",
                    destination = "conversation",
                    conversationId = 42,
                    recipientId = 17,
                    app = "consumer",
                    installationId = binding.identity.id,
                    bindingId = binding.id,
                    title = "Mensaje",
                    body = "Contenido de aviso",
                    expiresAt = expiresAt,
                ),
            )
            "propuesta recibida" -> createServiceIntent(
                type = AuthorizeServiceNotificationUseCase.TYPE_PROPOSAL_RECEIVED,
                destination = AuthorizeServiceNotificationUseCase.DESTINATION_PROPOSAL,
                resourceId = "88",
                eventId = "notification:service_proposal_received:88",
                expiresAt = expiresAt,
            )
            "turno próximo" -> createServiceIntent(
                type = AuthorizeServiceNotificationUseCase.TYPE_REMINDER,
                destination = AuthorizeServiceNotificationUseCase.DESTINATION_WORK_ORDER,
                resourceId = "88",
                eventId = "notification:work_order_close_to_scheduled_time:88",
                expiresAt = expiresAt,
            )
            "servicio finalizado" -> createServiceIntent(
                type = AuthorizeServiceNotificationUseCase.TYPE_COMPLETION,
                destination = AuthorizeServiceNotificationUseCase.DESTINATION_WORK_ORDER,
                resourceId = "88",
                eventId = "notification:work_order_completion_reported:88",
                expiresAt = expiresAt,
            )
            else -> error("Unknown notice: $aviso")
        }
    }

    private fun createServiceIntent(
        type: String,
        destination: String,
        resourceId: String,
        eventId: String,
        expiresAt: Long,
    ): Intent = ServiceNotificationIntent().create(
        context,
        ServiceNotification(
            version = "1",
            eventId = eventId,
            type = type,
            resourceType = destination,
            destination = destination,
            resourceId = resourceId,
            recipientId = 17,
            app = "consumer",
            installationId = binding.identity.id,
            bindingId = binding.id,
            title = "Servicio",
            body = "Contenido de aviso",
            expiresAt = expiresAt,
        ),
    )

    private fun seedNoticeTimeData() {
        currentConversation = sampleConversation("Mensaje previo a abrir el aviso")
        currentProposals = listOf(sampleProposal("88", "Propuesta anterior"))
        workOrderRepository.details = mapOf(
            "88" to sampleWorkOrder("101", "Orden anterior", TurnoStatus.Confirmed),
        )
        turnosRepository.turnos = listOf(sampleTurno("88", "Prestador anterior"))
        configureCurrentRepositories()
    }

    private fun refreshCurrentAccountData() {
        currentConversation = sampleConversation("Mensaje actualizado de mi cuenta")
        currentProposals = listOf(
            sampleProposal("88", "Descripción actual de la propuesta 88"),
            sampleProposal("99", "Descripción señuelo de la propuesta 99"),
        )
        val status = if (noticeKind == "servicio finalizado") {
            TurnoStatus.AwaitingPayment
        } else {
            TurnoStatus.Confirmed
        }
        val completionReport = if (noticeKind == "servicio finalizado") {
            CompletionReport(
                id = "report-88",
                description = "Informe actual de finalización",
                reportedOnEpochMillis = 1_788_500_000_000L,
                images = emptyList(),
            )
        } else {
            null
        }
        workOrderRepository.details = mapOf(
            "88" to sampleWorkOrder("101", "Estado actual de la orden 88", status, completionReport),
            "99" to sampleWorkOrder("202", "Estado señuelo de la orden 99", TurnoStatus.Confirmed),
        )
        turnosRepository.turnos = listOf(
            sampleTurno("88", "Prestador actual"),
            sampleTurno("99", "Prestador señuelo"),
        )
        configureCurrentRepositories()
    }

    private fun configureCurrentRepositories() {
        coEvery { conversationRepository.getConversationById("42") } coAnswers {
            ConversationDetailOutcome.Success(currentConversation)
        }
        coEvery { proposalRepository.getServiceProposals() } coAnswers {
            ServiceProposalsOutcome.Success(currentProposals)
        }
    }

    private fun assertCurrentConversation() {
        val conversationId = requireNotNull(navController().currentBackStackEntry)
            .arguments?.getString("conversationId")
        assertEquals("42", conversationId)
        val outcome = runBlocking { GetConversationByIdUseCase(conversationRepository)(conversationId.orEmpty()) }
        assertTrue("expected current conversation, got $outcome", outcome is ConversationDetailOutcome.Success)
        val detail = (outcome as ConversationDetailOutcome.Success).detail
        assertEquals("Mensaje actualizado de mi cuenta", detail.messages.single().content)
    }

    private fun assertCurrentProposal() {
        val proposalId = requireNotNull(navController().currentBackStackEntry)
            .arguments?.getString(Route.MisServiciosProposal.ARG_PROPOSAL_ID)
        assertEquals("88", proposalId)
        val viewModel = ProposalDetailViewModel(
            serviceProposalRepository = proposalRepository,
            startServiceProposalCheckout = StartServiceProposalCheckoutUseCase(checkoutRepository),
        )
        viewModel.load(proposalId.orEmpty())
        drainMainLooper()
        val state = viewModel.uiState.value
        assertTrue("expected exact proposal Ready, got $state", state is ProposalDetailUiState.Ready)
        val proposal = (state as ProposalDetailUiState.Ready).proposal
        assertEquals("88", proposal.id)
        assertEquals("Descripción actual de la propuesta 88", proposal.description)
        assertTrue(currentProposals.any { it.id == "99" })
    }

    private fun assertCurrentWorkOrder() {
        val workOrderId = requireNotNull(navController().currentBackStackEntry)
            .arguments?.getString(Route.WorkOrderDetail.ARG_WORK_ORDER_ID)
        assertEquals("88", workOrderId)
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = GetWorkOrderDetailUseCase(
                workOrderRepository,
                GetTurnosUseCase(turnosRepository),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(checkoutRepository),
            rateProvider = RateProviderUseCase(workOrderRepository),
        )
        viewModel.load(workOrderId.orEmpty())
        drainMainLooper()
        val state = viewModel.uiState.value
        assertTrue("expected exact current work order Ready, got $state", state is WorkOrderDetailUiState.Ready)
        val ready = state as WorkOrderDetailUiState.Ready
        assertEquals("88", ready.workOrderId)
        assertEquals("101", ready.workOrder.proposalId)
        if (noticeKind == "turno próximo") {
            assertEquals(TurnoStatus.Confirmed, ready.workOrder.status)
            assertNull(ready.workOrder.completionReport)
            assertEquals("Estado actual de la orden 88", ready.workOrder.description)
            assertEquals("Prestador actual", ready.workOrder.provider.name)
        } else {
            assertEquals(TurnoStatus.AwaitingPayment, ready.workOrder.status)
            assertEquals("Informe actual de finalización", ready.workOrder.completionReport?.description)
        }
    }

    private fun drainMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun navController(): TestNavHostController = requireNotNull(navController)

    private fun activeSession() = AuthSession(
        User("Verified Consumer", email = "consumer@example.test", backendUserId = 17),
        "active-jwt",
    )

    private fun sampleConversation(message: String) = ConversationDetail(
        id = "42",
        status = ConversationStatus.Pending,
        counterpart = ConversationCounterpart(20L, "Juan", "Prestador actual", "Plomería", null),
        messages = listOf(ConversationMessage("1", ConversationSender.Provider, message, 1000L)),
        updatedOnEpochMillis = 1000L,
    )

    private fun sampleProposal(id: String, description: String) = ServiceProposal(
        id = id,
        conversationId = "42",
        status = ServiceProposalStatus.Pending,
        counterpart = ServiceProposalCounterpart(id, "Pedro", "Prestador", "Plomero", null),
        description = description,
        amountCents = 1500000L,
        scheduledOnEpochMillis = 1_788_600_000_000L,
        createdOnEpochMillis = 1_788_500_000_000L,
    )

    private fun sampleTurno(id: String, providerName: String) = Turno(
        id = id,
        serviceProposalId = if (id == "88") "101" else "202",
        status = TurnoStatus.Confirmed,
        counterpart = TurnoCounterpart("20", providerName, "Apellido", "Plomería", null),
        description = "Turno actual",
        amountCents = 1500000L,
        scheduledOnEpochMillis = 1_788_600_000_000L,
    )

    private fun sampleWorkOrder(
        proposalId: String,
        description: String,
        status: TurnoStatus,
        completionReport: CompletionReport? = null,
    ) = WorkOrderDetail(
        proposalId = proposalId,
        provider = WorkOrderDetailCounterpart("20", "Prestador", "Apellido", "Plomería", null),
        description = description,
        amountCents = 1500000L,
        scheduledOnEpochMillis = 1_788_600_000_000L,
        acceptedOnEpochMillis = 1_788_500_000_000L,
        paidOnEpochMillis = if (status == TurnoStatus.Paid) 1_788_500_000_000L else null,
        status = status,
        completionReport = completionReport,
        review = null,
        estimatedDurationMinutes = null,
    )

    private fun fakePreferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val preferences = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { preferences.getString(any(), any()) } answers {
            values[firstArg<String>()] as? String ?: secondArg()
        }
        every { preferences.getInt(any(), any()) } answers {
            values[firstArg<String>()] as? Int ?: secondArg()
        }
        every { preferences.getLong(any(), any()) } answers {
            values[firstArg<String>()] as? Long ?: secondArg()
        }
        every { preferences.getBoolean(any(), any()) } answers {
            values[firstArg<String>()] as? Boolean ?: secondArg()
        }
        every { preferences.contains(any()) } answers { values.containsKey(firstArg<String>()) }
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            values[firstArg<String>()] = secondArg<String?>()
            editor
        }
        every { editor.putInt(any(), any()) } answers {
            values[firstArg<String>()] = secondArg<Int>()
            editor
        }
        every { editor.putLong(any(), any()) } answers {
            values[firstArg<String>()] = secondArg<Long>()
            editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            values[firstArg<String>()] = secondArg<Boolean>()
            editor
        }
        every { editor.remove(any()) } answers {
            values.remove(firstArg<String>())
            editor
        }
        every { editor.clear() } answers {
            values.clear()
            editor
        }
        every { editor.apply() } returns Unit
        every { editor.commit() } returns true
        return preferences
    }

    private class NavigationLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.RESUMED
        }

        override val lifecycle: Lifecycle = registry
    }

    private inner class CurrentTurnosRepository : TurnosRepository {
        var turnos: List<Turno> = emptyList()

        override suspend fun getTurnos(): TurnosOutcome = TurnosOutcome.Success(turnos)
    }

    private inner class CurrentWorkOrderRepository : WorkOrderDetailRepository {
        var details: Map<String, WorkOrderDetail> = emptyMap()

        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome {
            if (provider == null) return GetWorkOrderOutcome.NotFound
            val detail = details[workOrderId] ?: return GetWorkOrderOutcome.NotFound
            return GetWorkOrderOutcome.Found(detail.copy(provider = provider))
        }

        override suspend fun submitReview(
            workOrderId: String,
            rating: Int,
            description: String,
        ): SubmitWorkOrderReviewOutcome = SubmitWorkOrderReviewOutcome.Server(0, "not configured")
    }
}
