package com.loresuelvo.consumer.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.calendar.CalendarConnectionOutcome
import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStatus
import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStore
import com.loresuelvo.consumer.domain.notifications.NotificationPlatformCapability
import com.loresuelvo.consumer.domain.usecase.calendar.ConnectGoogleCalendarUseCase
import com.loresuelvo.consumer.domain.usecase.auth.GetConsumerProfileUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.GetNotificationPermissionStatusUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.RecordNotificationPermissionDecisionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ConsumerProfileViewModel @Inject constructor(
    private val getConsumerProfile: GetConsumerProfileUseCase,
    private val connectGoogleCalendar: ConnectGoogleCalendarUseCase,
    private val getNotificationPermissionStatus: GetNotificationPermissionStatusUseCase =
        GetNotificationPermissionStatusUseCase(
            object : NotificationPermissionStore {
                override fun hasDecided() = false
                override fun isPermissionGranted() = false
                override fun recordDecision(granted: Boolean) {}
            },
            object : NotificationPlatformCapability {
                override fun requiresRuntimeNotificationPermission() = true
                override fun areNotificationsEnabled() = false
            },
        ),
    private val recordNotificationPermissionDecision: RecordNotificationPermissionDecisionUseCase =
        RecordNotificationPermissionDecisionUseCase(
            object : NotificationPermissionStore {
                override fun hasDecided() = false
                override fun isPermissionGranted() = false
                override fun recordDecision(granted: Boolean) {}
            },
        ),
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConsumerProfileUiState>(ConsumerProfileUiState.Loading)
    val uiState: StateFlow<ConsumerProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ConsumerProfileUiState.Loading
            when (val outcome = getConsumerProfile()) {
                is CurrentUserOutcome.Success ->
                    _uiState.value = ConsumerProfileUiState.Ready(
                        user = outcome.user,
                        notificationPermission = getNotificationPermissionStatus(),
                    )
                else ->
                    _uiState.value = ConsumerProfileUiState.Error(
                        outcome.toConsumerProfileFailure()!!,
                    )
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        recordNotificationPermissionDecision(granted)
        refreshNotificationPermission()
    }

    fun refreshNotificationPermission() {
        updateReadyState {
            copy(notificationPermission = getNotificationPermissionStatus())
        }
    }

    fun onCalendarAuthorizationCancelled() {
        updateReadyState { copy(calendarConnection = CalendarConnectionUiState.Cancelled) }
    }

    fun onCalendarAuthorizationUnavailable() {
        updateReadyState { copy(calendarConnection = CalendarConnectionUiState.ConfigurationError) }
    }

    fun connectCalendar(serverAuthCode: String) {
        val current = _uiState.value as? ConsumerProfileUiState.Ready ?: return
        _uiState.value = current.copy(calendarConnection = CalendarConnectionUiState.Connecting)
        viewModelScope.launch {
            when (val outcome = connectGoogleCalendar(serverAuthCode)) {
                CalendarConnectionOutcome.Success -> load()
                is CalendarConnectionOutcome.Failure -> updateReadyState {
                    copy(calendarConnection = outcome.toUiState())
                }
            }
        }
    }

    private fun updateReadyState(transform: ConsumerProfileUiState.Ready.() -> ConsumerProfileUiState.Ready) {
        val current = _uiState.value as? ConsumerProfileUiState.Ready ?: return
        _uiState.value = current.transform()
    }
}

private fun CalendarConnectionOutcome.Failure.toUiState(): CalendarConnectionUiState.Failed =
    CalendarConnectionUiState.Failed(
        when (this) {
            is CalendarConnectionOutcome.Failure.Network ->
                CalendarConnectionFailure.Network(cause)
            is CalendarConnectionOutcome.Failure.Unauthorized ->
                CalendarConnectionFailure.Unauthorized(message)
            is CalendarConnectionOutcome.Failure.Server ->
                CalendarConnectionFailure.Server(code, message)
        },
    )
