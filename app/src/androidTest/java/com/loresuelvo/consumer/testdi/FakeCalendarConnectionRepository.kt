package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.calendar.CalendarConnectionOutcome
import com.loresuelvo.consumer.domain.calendar.CalendarConnectionRepository
import javax.inject.Inject

/** No-op Calendar port for instrumented tests that do not exercise Google authorization. */
class FakeCalendarConnectionRepository @Inject constructor() : CalendarConnectionRepository {
    override suspend fun connect(serverAuthCode: String): CalendarConnectionOutcome =
        CalendarConnectionOutcome.Success
}
