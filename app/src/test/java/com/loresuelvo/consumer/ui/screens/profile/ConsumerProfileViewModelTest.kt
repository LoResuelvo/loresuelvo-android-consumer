package com.loresuelvo.consumer.ui.screens.profile

import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.usecase.auth.GetConsumerProfileUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConsumerProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val getConsumerProfile = mockk<GetConsumerProfileUseCase>()
    private lateinit var viewModel: ConsumerProfileViewModel

    private val user = User(
        displayName = "Ana Perez",
        firstName = "Ana",
        lastName = "Perez",
        email = "ana@example.com",
        profilePhotoUrl = "https://cdn.test/ana.webp",
        address = RegisterConsumerAddress("Tucuman", "123", "2", "B"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loads_profile_and_exposes_ready_state() = runTest {
        coEvery { getConsumerProfile() } returns CurrentUserOutcome.Success(user)

        viewModel = ConsumerProfileViewModel(getConsumerProfile)
        advanceUntilIdle()

        assertEquals(ConsumerProfileUiState.Ready(user), viewModel.uiState.value)
    }

    @Test
    fun retry_recovers_after_session_error() = runTest {
        coEvery { getConsumerProfile() } returnsMany listOf(
            CurrentUserOutcome.Failure.Unauthorized("Token expired"),
            CurrentUserOutcome.Success(user),
        )

        viewModel = ConsumerProfileViewModel(getConsumerProfile)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ConsumerProfileUiState.Error)

        viewModel.load()
        advanceUntilIdle()

        assertEquals(ConsumerProfileUiState.Ready(user), viewModel.uiState.value)
        coVerify(exactly = 2) { getConsumerProfile() }
    }
}
