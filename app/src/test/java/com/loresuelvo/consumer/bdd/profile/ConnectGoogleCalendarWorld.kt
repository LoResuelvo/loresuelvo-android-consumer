package com.loresuelvo.consumer.bdd.profile

import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.calendar.CalendarConnectionOutcome
import com.loresuelvo.consumer.domain.usecase.auth.GetConsumerProfileUseCase
import com.loresuelvo.consumer.domain.usecase.calendar.ConnectGoogleCalendarUseCase
import com.loresuelvo.consumer.ui.screens.profile.CalendarConnectionUiState
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
class ConnectGoogleCalendarWorld : AutoCloseable {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val getConsumerProfile = mockk<GetConsumerProfileUseCase>()
    private val connectGoogleCalendar = mockk<ConnectGoogleCalendarUseCase>()
    private lateinit var viewModel: ConsumerProfileViewModel
    private var authorizationRequested = false

    fun open(status: CalendarConnectionStatus) {
        coEvery { getConsumerProfile() } returnsMany listOf(
            CurrentUserOutcome.Success(user(status)),
            CurrentUserOutcome.Success(user(CalendarConnectionStatus.CONNECTED)),
        )
        coEvery { connectGoogleCalendar(any()) } returns CalendarConnectionOutcome.Success
        Dispatchers.setMain(dispatcher)
        viewModel = ConsumerProfileViewModel(getConsumerProfile, connectGoogleCalendar)
        scheduler.advanceUntilIdle()
    }

    fun chooseConnect(label: String) {
        check(label == "Vincular Google Calendar" || label == "Volver a vincular Google Calendar")
        authorizationRequested = true
    }

    fun returnWithCode(code: String) {
        check(authorizationRequested)
        viewModel.connectCalendar(code)
        scheduler.advanceUntilIdle()
    }

    fun cancelAuthorization() {
        viewModel.onCalendarAuthorizationCancelled()
    }

    fun returnWithInvalidCode() {
        coEvery { connectGoogleCalendar("invalid-code") } returns
            CalendarConnectionOutcome.Failure.Server(400, "invalid")
        viewModel.connectCalendar("invalid-code")
        scheduler.advanceUntilIdle()
    }

    fun state(): ConsumerProfileUiState = viewModel.uiState.value

    fun authorizationWasRequested(): Boolean = authorizationRequested

    override fun close() {
        Dispatchers.resetMain()
    }

    private fun user(status: CalendarConnectionStatus) = User(
        displayName = "Ana Perez",
        firstName = "Ana",
        lastName = "Perez",
        email = "ana@example.com",
        calendarConnectionStatus = status,
    )
}
