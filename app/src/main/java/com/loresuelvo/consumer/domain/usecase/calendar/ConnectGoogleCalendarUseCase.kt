package com.loresuelvo.consumer.domain.usecase.calendar

import com.loresuelvo.consumer.domain.calendar.CalendarConnectionOutcome
import com.loresuelvo.consumer.domain.calendar.CalendarConnectionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectGoogleCalendarUseCase @Inject constructor(
    private val repository: CalendarConnectionRepository,
) {
    suspend operator fun invoke(serverAuthCode: String): CalendarConnectionOutcome =
        repository.connect(serverAuthCode)
}
