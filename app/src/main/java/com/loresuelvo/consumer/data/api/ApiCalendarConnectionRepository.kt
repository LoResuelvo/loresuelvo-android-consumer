package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.ConnectCalendarRequestDto
import com.loresuelvo.consumer.domain.calendar.CalendarConnectionOutcome
import com.loresuelvo.consumer.domain.calendar.CalendarConnectionRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiCalendarConnectionRepository @Inject constructor(
    private val backendApi: BackendApi,
) : CalendarConnectionRepository {

    override suspend fun connect(serverAuthCode: String): CalendarConnectionOutcome {
        if (serverAuthCode.isBlank()) {
            return CalendarConnectionOutcome.Failure.Server(400, "Missing server authorization code")
        }

        return try {
            val response = backendApi.connectCalendar(
                ConnectCalendarRequestDto(serverAuthCode = serverAuthCode),
            )
            if (response.isSuccessful) {
                CalendarConnectionOutcome.Success
            } else if (response.code() == 401) {
                CalendarConnectionOutcome.Failure.Unauthorized("Calendar session expired")
            } else {
                CalendarConnectionOutcome.Failure.Server(
                    response.code(),
                    "Calendar connection failed",
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            when (val error = e.toApiError()) {
                is com.loresuelvo.consumer.domain.api.ApiError.Network ->
                    CalendarConnectionOutcome.Failure.Network(error.networkCause)
                is com.loresuelvo.consumer.domain.api.ApiError.Unauthorized ->
                    CalendarConnectionOutcome.Failure.Unauthorized(error.errorMessage)
                is com.loresuelvo.consumer.domain.api.ApiError.Server ->
                    CalendarConnectionOutcome.Failure.Server(error.code, error.errorMessage)
                is com.loresuelvo.consumer.domain.api.ApiError.Unknown ->
                    CalendarConnectionOutcome.Failure.Server(0, error.message ?: "Unknown error")
            }
        }
    }
}
