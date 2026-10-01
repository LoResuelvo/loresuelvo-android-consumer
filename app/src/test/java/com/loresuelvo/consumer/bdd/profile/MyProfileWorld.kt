package com.loresuelvo.consumer.bdd.profile

import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.usecase.auth.GetConsumerProfileUseCase
import com.loresuelvo.consumer.domain.usecase.calendar.ConnectGoogleCalendarUseCase
import com.loresuelvo.consumer.ui.screens.profile.ConsumerProfileUiState
import com.loresuelvo.consumer.ui.screens.profile.ConsumerProfileViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileWorld : AutoCloseable {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val getConsumerProfile = mockk<GetConsumerProfileUseCase>()
    private val connectGoogleCalendar = mockk<ConnectGoogleCalendarUseCase>()
    private lateinit var viewModel: ConsumerProfileViewModel
    private val responses = ArrayDeque<CurrentUserOutcome>()
    private var requests = 0

    fun profileWith(user: User) {
        responses += CurrentUserOutcome.Success(user)
        openProfile()
    }

    fun profileWithoutPhoto() {
        profileWith(User("Ana Perez", "Ana", "Perez", "ana@example.com"))
    }

    fun profileRequestFailsWithExpiredSession() {
        responses += CurrentUserOutcome.Failure.Unauthorized("Token expired")
        responses += CurrentUserOutcome.Success(User("Ana Perez", "Ana", "Perez", "ana@example.com"))
        openProfile()
    }

    fun retry() {
        viewModel.load()
        scheduler.advanceUntilIdle()
    }

    fun state(): ConsumerProfileUiState = viewModel.uiState.value

    fun requestCount(): Int = requests

    private fun openProfile() {
        Dispatchers.setMain(dispatcher)
        coEvery { getConsumerProfile() } answers {
            requests += 1
            responses.removeFirst()
        }
        viewModel = ConsumerProfileViewModel(getConsumerProfile, connectGoogleCalendar)
        scheduler.advanceUntilIdle()
    }

    override fun close() {
        Dispatchers.resetMain()
    }
}
