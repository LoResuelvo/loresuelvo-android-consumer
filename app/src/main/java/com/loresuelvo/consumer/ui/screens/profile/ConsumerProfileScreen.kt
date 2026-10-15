package com.loresuelvo.consumer.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStatus
import com.loresuelvo.consumer.ui.components.profile.ProfileActionRow
import com.loresuelvo.consumer.ui.components.profile.ProfileInfoRow
import com.loresuelvo.consumer.ui.components.profile.ProfileSectionCard
import com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerProfileScreen(
    state: ConsumerProfileUiState,
    onRetryClick: () -> Unit,
    onCalendarConnectClick: () -> Unit = {},
    onNotificationActionClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.consumer_profile_title)) })
        },
    ) { padding ->
        when (state) {
            ConsumerProfileUiState.Loading -> LoadingState(Modifier.padding(padding))
            is ConsumerProfileUiState.Error -> ErrorState(
                modifier = Modifier.padding(padding),
                failure = state.failure,
                onRetryClick = onRetryClick,
            )
            is ConsumerProfileUiState.Ready -> ReadyState(
                modifier = Modifier.padding(padding),
                user = state.user,
                calendarConnection = state.calendarConnection,
                notificationPermission = state.notificationPermission,
                onCalendarConnectClick = onCalendarConnectClick,
                onNotificationActionClick = onNotificationActionClick,
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag("consumer-profile-loading"))
    }
}

@Composable
private fun ReadyState(
    user: User,
    calendarConnection: CalendarConnectionUiState,
    notificationPermission: NotificationPermissionStatus,
    onCalendarConnectClick: () -> Unit,
    onNotificationActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fullName = listOfNotNull(user.firstName, user.lastName)
        .joinToString(" ")
        .ifBlank { user.displayName }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("consumer-profile-content"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProviderAvatar(
                        name = fullName,
                        profilePhotoUrl = user.profilePhotoUrl,
                        size = 104.dp,
                        testTag = "consumer-profile-avatar",
                    )
                    Text(
                        text = fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("consumer-profile-name"),
                    )
                    user.email?.let { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("consumer-profile-email"),
                        )
                    }
                }
            }
        }
        item {
            ProfileSectionCard(
                title = stringResource(R.string.consumer_profile_account_title),
                icon = Icons.Outlined.Person,
            ) {
                user.firstName?.let {
                    ProfileInfoRow(
                        icon = Icons.Outlined.Person,
                        label = stringResource(R.string.consumer_profile_first_name_label),
                        value = it,
                    )
                }
                user.lastName?.let {
                    ProfileInfoRow(
                        icon = Icons.Outlined.Person,
                        label = stringResource(R.string.consumer_profile_last_name_label),
                        value = it,
                    )
                }
            }
        }
        item {
            ProfileSectionCard(
                title = stringResource(R.string.consumer_profile_address_title),
                icon = Icons.Outlined.LocationOn,
            ) {
                user.address?.let { address ->
                    ProfileInfoRow(
                        icon = Icons.Outlined.LocationOn,
                        label = stringResource(R.string.consumer_profile_address_label),
                        value = "${address.street} ${address.streetNumber}",
                    )
                    if (address.floor.isNotBlank() || address.unit.isNotBlank()) {
                        ProfileInfoRow(
                            icon = Icons.Outlined.LocationOn,
                            label = stringResource(R.string.consumer_profile_floor_unit_label),
                            value = listOfNotNull(
                                address.floor.takeIf { it.isNotBlank() },
                                address.unit.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                        )
                    }
                } ?: Text(
                    text = stringResource(R.string.consumer_profile_address_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.loresuelvo.consumer.ui.theme.SubtitleGray,
                )
            }
        }
        item {
            ProfileSectionCard(
                title = stringResource(R.string.consumer_profile_notifications_title),
                icon = Icons.Outlined.NotificationsNone,
            )
            {
                NotificationsCard(
                    status = notificationPermission,
                    onActionClick = onNotificationActionClick,
                )
                HorizontalDivider()
                CalendarConnectionCard(
                    status = user.calendarConnectionStatus,
                    actionState = calendarConnection,
                    onConnectClick = onCalendarConnectClick,
                )
            }
        }
    }
}

@Composable
private fun NotificationsCard(
    status: NotificationPermissionStatus,
    onActionClick: () -> Unit,
) {
    val description = when (status) {
        NotificationPermissionStatus.GRANTED ->
            stringResource(R.string.consumer_profile_notifications_enabled)
        NotificationPermissionStatus.DENIED ->
            stringResource(R.string.consumer_profile_notifications_disabled)
        NotificationPermissionStatus.UNDECIDED ->
            stringResource(R.string.consumer_profile_notifications_undecided)
    }
    val buttonText = when (status) {
        NotificationPermissionStatus.UNDECIDED ->
            stringResource(R.string.consumer_profile_notifications_enable)
        NotificationPermissionStatus.GRANTED,
        NotificationPermissionStatus.DENIED ->
            stringResource(R.string.consumer_profile_notifications_settings)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("consumer-profile-notifications"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileActionRow(
            icon = Icons.Outlined.NotificationsNone,
            title = stringResource(R.string.consumer_profile_notifications_option_title),
            description = description,
            status = notificationStatusLabel(status),
            onClick = onActionClick,
            actionLabel = buttonText,
            actionModifier = Modifier.testTag("consumer-profile-notifications-action"),
        )
    }
}

@Composable
private fun CalendarConnectionCard(
    status: CalendarConnectionStatus,
    actionState: CalendarConnectionUiState,
    onConnectClick: () -> Unit,
) {
    val title = stringResource(R.string.consumer_profile_calendar_title)
    val description = when (status) {
        CalendarConnectionStatus.CONNECTED ->
            stringResource(R.string.consumer_profile_calendar_connected)
        CalendarConnectionStatus.ACTION_REQUIRED ->
            stringResource(R.string.consumer_profile_calendar_action_required)
        CalendarConnectionStatus.DISCONNECTED,
        CalendarConnectionStatus.UNKNOWN,
        -> stringResource(R.string.consumer_profile_calendar_disconnected)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("consumer-profile-calendar"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (actionState) {
                CalendarConnectionUiState.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("consumer-profile-calendar-loading"),
                    )
                }
                else -> {
                    ProfileActionRow(
                        icon = Icons.Outlined.CalendarMonth,
                        title = title,
                        description = description,
                        status = calendarStatusLabel(status),
                        onClick = onConnectClick,
                        actionLabel = stringResource(
                            if (status == CalendarConnectionStatus.ACTION_REQUIRED) {
                                R.string.consumer_profile_calendar_reconnect
                            } else {
                                R.string.consumer_profile_calendar_connect
                            },
                        ),
                        actionEnabled = status != CalendarConnectionStatus.CONNECTED,
                        actionModifier = Modifier.testTag("consumer-profile-calendar-connect"),
                    )
                    val actionMessage = when (actionState) {
                        CalendarConnectionUiState.Cancelled ->
                            stringResource(R.string.consumer_profile_calendar_cancelled)
                        CalendarConnectionUiState.ConfigurationError ->
                            stringResource(R.string.consumer_profile_calendar_configuration_error)
                        is CalendarConnectionUiState.Failed -> when (actionState.failure) {
                            is CalendarConnectionFailure.Network ->
                                stringResource(R.string.consumer_profile_calendar_error_network)
                            is CalendarConnectionFailure.Unauthorized ->
                                stringResource(R.string.consumer_profile_calendar_error_unauthorized)
                            is CalendarConnectionFailure.Server ->
                                stringResource(R.string.consumer_profile_calendar_error_server)
                        }
                        CalendarConnectionUiState.Idle,
                        CalendarConnectionUiState.Connecting,
                        -> null
                    }
                    actionMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("consumer-profile-calendar-message"),
                        )
                    }
                }
            }
    }
}

private fun notificationStatusLabel(status: NotificationPermissionStatus): String = when (status) {
    NotificationPermissionStatus.GRANTED -> "Activo"
    NotificationPermissionStatus.DENIED -> "Desactivado"
    NotificationPermissionStatus.UNDECIDED -> "Pendiente"
}

private fun calendarStatusLabel(status: CalendarConnectionStatus): String = when (status) {
    CalendarConnectionStatus.CONNECTED -> "Vinculado"
    CalendarConnectionStatus.ACTION_REQUIRED -> "Requiere atención"
    CalendarConnectionStatus.DISCONNECTED,
    CalendarConnectionStatus.UNKNOWN,
    -> "Sin vincular"
}

@Composable
private fun ErrorState(
    failure: ConsumerProfileFailure,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("consumer-profile-error"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = when (failure) {
                ConsumerProfileFailure.NotFound -> stringResource(R.string.consumer_profile_error_not_found)
                is ConsumerProfileFailure.Network -> stringResource(R.string.consumer_profile_error_network)
                is ConsumerProfileFailure.Server -> stringResource(R.string.consumer_profile_error_server)
                is ConsumerProfileFailure.Unauthorized -> stringResource(R.string.consumer_profile_error_unauthorized)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onRetryClick,
            modifier = Modifier.padding(top = 16.dp).testTag("consumer-profile-retry"),
        ) {
            Text(stringResource(R.string.consumer_profile_retry))
        }
    }
}
