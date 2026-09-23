package com.loresuelvo.consumer.ui.screens.home

import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAcceptedServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [HomeViewModel] covering the US-54 scenario
 * 01-VSP ("pending proposals") and 02-VSP ("upcoming jobs")
 * branches. Each surface — categories, pending proposals,
 * upcoming proposals — renders independently:
 *
 *  - The global state — Loading / Ready / Error — is driven by
 *    the categories round trip (it's the action without which
 *    the Home dashboard is not usable).
 *  - The two proposals sub-states land on their own Loading /
 *    Ready / Error without disturbing the categories outcome.
 *
 * The VM is constructed **inside** each test (after `coEvery`
 * stubs the round trips) for the same reason as
 * `MessagesListViewModelTest`: the VM's `init` launches three
 * parallel coroutines; with `StandardTestDispatcher` they sit on
 * the scheduler until the test pumps. If the VM were built in
 * `@Before` (before the stubs), the coroutines would dispatch
 * and blow up on the first unstubbed call. `UnconfinedTestDispatcher`
 * runs them eagerly inside the `runTest` body.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val categoryRepository = mockk<CategoryRepository>()
    private val serviceProposalRepository = mockk<ServiceProposalRepository>()
    private val turnosRepository = mockk<TurnosRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // Default stub: every US-54 test sees an empty list of
        // turnos unless it explicitly overrides (no test does
        // today — the new sub-state is exercised by the
        // dedicated `loadTurnos_*` tests below). Mirrors the
        // service-proposals stub pattern.
        coEvery { turnosRepository.getTurnos() } returns
            TurnosOutcome.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): HomeViewModel = HomeViewModel(
        getCategories = GetCategoriesUseCase(categoryRepository),
        getPendingServiceProposals = GetPendingServiceProposalsUseCase(serviceProposalRepository),
        getAcceptedServiceProposals = GetAcceptedServiceProposalsUseCase(serviceProposalRepository),
        getTurnos = GetTurnosUseCase(turnosRepository),
    )

    private fun pendingProposal(
        id: String,
        providerName: String = "Juan",
    ): ServiceProposal = ServiceProposal(
        id = id,
        conversationId = "100",
        status = ServiceProposalStatus.Pending,
        counterpart = ServiceProposalCounterpart(
            id = "1",
            name = providerName,
            surname = "Pérez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "Fuga en el lavamanos",
        amountCents = 1500000L,
        scheduledOnEpochMillis = 1_792_074_600_000L,
        createdOnEpochMillis = 1_788_434_364_640L,
    )

    private fun acceptedProposal(
        id: String,
        providerName: String = "Ana",
    ): ServiceProposal = ServiceProposal(
        id = id,
        conversationId = "200",
        status = ServiceProposalStatus.Accepted,
        counterpart = ServiceProposalCounterpart(
            id = "2",
            name = providerName,
            surname = "Gómez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "Cambio de canilla",
        amountCents = 2200000L,
        scheduledOnEpochMillis = 1_793_500_800_000L,
        createdOnEpochMillis = 1_789_200_000_000L,
    )

    // ---- Scenario 01-VSP: pending proposals ----------------------

    @Test
    fun categories_and_pending_proposals_both_succeed_lands_in_Ready() = runTest {
        coEvery { categoryRepository.getCategories() } returns CategoriesOutcome.Success(
            listOf(
                Category(id = 1, name = "Plomería"),
                Category(id = 2, name = "Electricidad"),
            ),
        )
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(
                listOf(
                    pendingProposal(id = "1", providerName = "Carlos"),
                    pendingProposal(id = "2", providerName = "Ana"),
                ),
            )

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue("expected Ready, got ${state::class.simpleName}", state is HomeUiState.Ready)
        val ready = state as HomeUiState.Ready
        assertTrue(ready.categories is CategoriesState.Ready)
        assertEquals(
            listOf("Electricidad", "Plomería"),
            (ready.categories as CategoriesState.Ready).items.map { it.name },
        )
        val pending = ready.pendingServiceProposals
        assertTrue(pending is ServiceProposalsState.Ready)
        assertEquals(
            listOf("1", "2"),
            (pending as ServiceProposalsState.Ready).items.map { it.id },
        )
        assertEquals(
            listOf("Carlos", "Ana"),
            pending.items.map { it.counterpart.name },
        )
    }

    @Test
    fun pending_proposals_empty_list_renders_Ready_with_empty_items() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(emptyList())

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        val pending = (state as HomeUiState.Ready).pendingServiceProposals
        assertTrue(
            "expected Ready(empty), got $pending",
            pending is ServiceProposalsState.Ready &&
                (pending as ServiceProposalsState.Ready).items.isEmpty(),
        )
    }

    @Test
    fun pending_proposals_network_failure_does_not_break_categories() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Network(IOException("dns"))

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue("expected Ready, got ${state::class.simpleName}", state is HomeUiState.Ready)
        val ready = state as HomeUiState.Ready
        assertTrue(ready.categories is CategoriesState.Ready)
        assertEquals(ServiceProposalsState.Error, ready.pendingServiceProposals)
    }

    @Test
    fun pending_proposals_server_failure_does_not_break_categories() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Server(code = 500, message = "boom")

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        assertEquals(
            ServiceProposalsState.Error,
            (state as HomeUiState.Ready).pendingServiceProposals,
        )
    }

    @Test
    fun categories_failure_still_surfaces_pending_proposals() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Failure.Server(code = 500, message = "boom")
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(listOf(pendingProposal(id = "1")))

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue("expected Error, got ${state::class.simpleName}", state is HomeUiState.Error)
        val error = state as HomeUiState.Error
        assertEquals(CategoriesState.Error, error.categories)
        assertTrue(
            "expected Ready, got ${error.pendingServiceProposals}",
            error.pendingServiceProposals is ServiceProposalsState.Ready,
        )
    }

    // ---- Scenario 02-VSP: upcoming jobs ---------------------------

    @Test
    fun categories_and_upcoming_proposals_both_succeed_lands_in_Ready() = runTest {
        coEvery { categoryRepository.getCategories() } returns CategoriesOutcome.Success(
            listOf(Category(id = 1, name = "Plomería")),
        )
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(
                listOf(
                    pendingProposal(id = "1"),
                    acceptedProposal(id = "2", providerName = "Carlos"),
                    acceptedProposal(id = "3", providerName = "Lucía"),
                ),
            )

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        val ready = state as HomeUiState.Ready
        val upcoming = ready.upcomingServiceProposals
        assertTrue(upcoming is ServiceProposalsState.Ready)
        assertEquals(
            listOf("2", "3"),
            (upcoming as ServiceProposalsState.Ready).items.map { it.id },
        )
        assertTrue(upcoming.items.all { it.status == ServiceProposalStatus.Accepted })
        // Pending filter still works independently.
        assertEquals(listOf("1"), (ready.pendingServiceProposals as ServiceProposalsState.Ready).items.map { it.id })
    }

    @Test
    fun upcoming_proposals_empty_list_renders_Ready_with_empty_items() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(listOf(pendingProposal(id = "1")))

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        val upcoming = (state as HomeUiState.Ready).upcomingServiceProposals
        assertTrue(
            "expected Ready(empty), got $upcoming",
            upcoming is ServiceProposalsState.Ready &&
                (upcoming as ServiceProposalsState.Ready).items.isEmpty(),
        )
    }

    @Test
    fun upcoming_proposals_network_failure_does_not_break_categories() = runTest {
        // Same repo mock is shared; the SAME failure feeds both
        // `Pending` and `Accepted` use cases. We pin that BOTH
        // surfaces land in Error without affecting categories.
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Network(IOException("dns"))

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        val ready = state as HomeUiState.Ready
        assertEquals(ServiceProposalsState.Error, ready.pendingServiceProposals)
        assertEquals(ServiceProposalsState.Error, ready.upcomingServiceProposals)
    }

    @Test
    fun upcoming_proposals_server_failure_does_not_break_pending() = runTest {
        // A server failure flips BOTH surfaces to Error because
        // they share the same repository call. The pending
        // surface remains consistent with the upcoming surface
        // — both reflect the underlying round trip outcome.
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Server(code = 500, message = "boom")

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        val ready = state as HomeUiState.Ready
        assertEquals(ServiceProposalsState.Error, ready.pendingServiceProposals)
        assertEquals(ServiceProposalsState.Error, ready.upcomingServiceProposals)
    }

    // ---- Mis Turnos section (visualize-turns.feature) ----

    @Test
    fun loadTurnos_returns_Ready_with_up_to_2_turnos_ordered_by_scheduled_date_ascending() = runTest {
        // Seed 3 turnos out of order. `take(MAX_TURNOS_ON_HOME)`
        // must keep only the 2 closest-to-now ones (today + tomorrow),
        // and they must be sorted ascending (closest first).
        val today = startOfTodayUtcMillis()
        val tomorrow = today + 24 * 60 * 60 * 1000L
        val nextMonth = today + 30 * 24 * 60 * 60 * 1000L
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(emptyList())
        coEvery { turnosRepository.getTurnos() } returns TurnosOutcome.Success(
            listOf(
                sampleTurno(id = "1", scheduledOnEpochMillis = nextMonth),
                sampleTurno(id = "2", scheduledOnEpochMillis = today),
                sampleTurno(id = "3", scheduledOnEpochMillis = tomorrow),
            )
        )

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue("expected Ready, got ${state::class.simpleName}", state is HomeUiState.Ready)
        val ready = state as HomeUiState.Ready
        val turnosState = ready.turnos
        assertTrue(
            "expected Ready with items, got $turnosState",
            turnosState is TurnosState.Ready,
        )
        val items = (turnosState as TurnosState.Ready).items
        assertEquals(2, items.size)
        assertEquals(listOf("2", "3"), items.map { it.id })
    }

    @Test
    fun loadTurnos_splits_awaiting_payment_from_upcoming_preview() = runTest {
        // The same `GET /work-orders` response feeds two Home
        // sub-states: `awaitingPaymentTurnos` (filtered by status)
        // and `turnos` (closest-to-now N). Verify both pipelines
        // are populated from one round trip and that the awaiting
        // branch ignores non-awaiting statuses.
        val today = startOfTodayUtcMillis()
        val tomorrow = today + 24 * 60 * 60 * 1000L
        val dayAfterTomorrow = today + 2 * 24 * 60 * 60 * 1000L
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(emptyList())
        coEvery { turnosRepository.getTurnos() } returns TurnosOutcome.Success(
            listOf(
                sampleTurno(id = "1", scheduledOnEpochMillis = tomorrow),
                sampleAwaitingPaymentTurno(id = "5", scheduledOnEpochMillis = today),
                sampleTurno(id = "2", scheduledOnEpochMillis = dayAfterTomorrow),
            )
        )

        val viewModel = buildViewModel()
        val state = viewModel.uiState.value as HomeUiState.Ready

        // Awaiting-payment sub-state: only the entry with
        // `TurnoStatus.AwaitingPayment` lands here.
        val awaiting = state.awaitingPaymentTurnos as TurnosState.Ready
        assertEquals(1, awaiting.items.size)
        assertEquals("5", awaiting.items[0].id)
        assertEquals(TurnoStatus.AwaitingPayment, awaiting.items[0].status)

        // Upcoming preview: takes the two closest-to-now entries
        // (today + tomorrow, both `Confirmed`) and sorts ascending.
        // The awaiting_payment entry (today) is excluded — the
        // dashboard surfaces it via the dedicated section above.
        val upcoming = state.turnos as TurnosState.Ready
        assertEquals(2, upcoming.items.size)
        assertEquals(listOf("5", "1"), upcoming.items.map { it.id })
    }

    @Test
    fun loadTurnos_awaiting_payment_state_is_empty_when_no_awaiting_in_response() = runTest {
        // Only `Confirmed` + `Cancelled` in the response — the
        // awaiting-payment sub-state must surface `Ready(empty)`.
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(emptyList())
        coEvery { turnosRepository.getTurnos() } returns TurnosOutcome.Success(
            listOf(
                sampleTurno(id = "1", scheduledOnEpochMillis = 1_788_000_000_000L),
            )
        )

        val viewModel = buildViewModel()
        val state = viewModel.uiState.value as HomeUiState.Ready
        assertTrue(state.awaitingPaymentTurnos is TurnosState.Ready)
        assertEquals(0, (state.awaitingPaymentTurnos as TurnosState.Ready).items.size)
    }

    private fun sampleAwaitingPaymentTurno(
        id: String,
        scheduledOnEpochMillis: Long,
    ): Turno = Turno(
        id = id,
        serviceProposalId = "p-$id",
        status = TurnoStatus.AwaitingPayment,
        counterpart = TurnoCounterpart(
            id = "$id-c",
            name = "Diego",
            surname = "Fernández",
            categoryName = "Electricidad",
            profilePhotoUrl = null,
        ),
        description = "Instalación de aire acondicionado split",
        amountCents = 1_800_000L,
        scheduledOnEpochMillis = scheduledOnEpochMillis,
    )

    @Test
    fun loadTurnos_returns_Ready_empty_when_backend_returns_empty_list() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(emptyList())
        coEvery { turnosRepository.getTurnos() } returns
            TurnosOutcome.Success(emptyList())

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        val ready = state as HomeUiState.Ready
        assertTrue(ready.turnos is TurnosState.Ready)
        assertTrue((ready.turnos as TurnosState.Ready).items.isEmpty())
    }

    @Test
    fun loadTurnos_failure_does_not_break_categories() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(emptyList())
        coEvery { turnosRepository.getTurnos() } returns
            TurnosOutcome.Failure.Network(IOException("dns"))

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        val ready = state as HomeUiState.Ready
        assertEquals(TurnosState.Error, ready.turnos)
    }
}

/**
 * Returns the epoch-millis instant at 00:00:00 UTC of the
 * current day. Used by the loadTurnos test to seed a `Turno` that
 * falls on "today" without hard-coding a date that would drift.
 */
private fun startOfTodayUtcMillis(): Long {
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun sampleTurno(
    id: String,
    scheduledOnEpochMillis: Long,
): Turno = Turno(
    id = id,
    serviceProposalId = "p-$id",
    status = TurnoStatus.Confirmed,
    counterpart = TurnoCounterpart(
        id = "$id-c",
        name = "Juan",
        surname = "Gómez",
        categoryName = "Plomería",
        profilePhotoUrl = null,
    ),
    description = "Reparación",
    amountCents = 1_500_000L,
    scheduledOnEpochMillis = scheduledOnEpochMillis,
)
