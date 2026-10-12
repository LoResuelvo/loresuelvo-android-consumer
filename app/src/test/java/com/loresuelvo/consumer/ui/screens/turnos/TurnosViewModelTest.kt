package com.loresuelvo.consumer.ui.screens.turnos

import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.auth.GetConsumerProfileUseCase
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import io.mockk.coEvery
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TurnosViewModelTest {

    private val turnosRepository = mockk<TurnosRepository>()
    private val userRepository = mockk<UserRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { userRepository.getCurrentUser() } returns CurrentUserOutcome.NotFound
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun search_matches_provider_category_or_description() = runTest {
        val turnos = listOf(
            turno("plumbing", "Juan", "Pérez", "Plomería", "Reparar pérdida"),
            turno("painting", "Ana", "Gómez", "Pintura", "Pintar habitación"),
        )
        coEvery { turnosRepository.getTurnos() } returns TurnosOutcome.Success(turnos)

        val viewModel = buildViewModel()

        viewModel.onSearchQueryChanged("ana")
        assertEquals(
            listOf("painting"),
            (viewModel.uiState.value as TurnosUiState.Ready).turnos.map { it.id },
        )

        viewModel.onSearchQueryChanged("pérdida")
        assertEquals(
            listOf("plumbing"),
            (viewModel.uiState.value as TurnosUiState.Ready).turnos.map { it.id },
        )
    }

    @Test
    fun status_filter_can_be_changed_and_cleared_without_reloading() = runTest {
        val turnos = listOf(
            turno("confirmed", status = TurnoStatus.Confirmed),
            turno("paid", status = TurnoStatus.Paid),
        )
        coEvery { turnosRepository.getTurnos() } returns TurnosOutcome.Success(turnos)

        val viewModel = buildViewModel()
        viewModel.onFilterSelected(TurnoStatus.Paid)

        val filtered = viewModel.uiState.value as TurnosUiState.Ready
        assertEquals(TurnoStatus.Paid, filtered.selectedStatusFilter)
        assertEquals(listOf("paid"), filtered.turnos.map { it.id })

        viewModel.onFilterSelected(null)
        assertEquals(
            listOf("confirmed", "paid"),
            (viewModel.uiState.value as TurnosUiState.Ready).turnos.map { it.id },
        )
    }

    private fun buildViewModel(): TurnosViewModel = TurnosViewModel(
        getTurnos = GetTurnosUseCase(turnosRepository),
        getConsumerProfile = GetConsumerProfileUseCase(userRepository),
    )

    private fun turno(
        id: String,
        name: String = "Juan",
        surname: String = "Pérez",
        category: String = "Plomería",
        description: String = "Reparación",
        status: TurnoStatus = TurnoStatus.Confirmed,
    ): Turno = Turno(
        id = id,
        serviceProposalId = "proposal-$id",
        status = status,
        counterpart = TurnoCounterpart(
            id = "provider-$id",
            name = name,
            surname = surname,
            categoryName = category,
            profilePhotoUrl = null,
        ),
        description = description,
        amountCents = 100_000,
        scheduledOnEpochMillis = TimeUnit.DAYS.toMillis(30),
    )
}
