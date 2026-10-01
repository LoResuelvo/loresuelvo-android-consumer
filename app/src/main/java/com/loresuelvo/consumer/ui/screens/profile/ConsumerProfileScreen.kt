package com.loresuelvo.consumer.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerProfileScreen(
    state: ConsumerProfileUiState,
    onRetryClick: () -> Unit,
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProviderAvatar(
                        name = fullName,
                        profilePhotoUrl = user.profilePhotoUrl,
                        size = 88.dp,
                        testTag = "consumer-profile-avatar",
                    )
                    Column {
                        Text(
                            text = fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("consumer-profile-name"),
                        )
                        user.email?.let { email ->
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.testTag("consumer-profile-email"),
                            )
                        }
                    }
                }
            }
        }
        item {
            ProfileDataCard(
                title = stringResource(R.string.consumer_profile_account_title),
                values = listOfNotNull(
                    user.firstName?.let { stringResource(R.string.consumer_profile_first_name, it) },
                    user.lastName?.let { stringResource(R.string.consumer_profile_last_name, it) },
                    user.email?.let { stringResource(R.string.consumer_profile_email, it) },
                ),
            )
        }
        item {
            ProfileDataCard(
                title = stringResource(R.string.consumer_profile_address_title),
                values = user.address?.toDisplayValues().orEmpty().ifEmpty {
                    listOf(stringResource(R.string.consumer_profile_address_missing))
                },
            )
        }
    }
}

@Composable
private fun ProfileDataCard(
    title: String,
    values: List<String>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            values.forEach { value -> Text(value, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

private fun RegisterConsumerAddress.toDisplayValues(): List<String> = buildList {
    add("$street $streetNumber")
    if (floor.isNotBlank() || unit.isNotBlank()) {
        add(listOfNotNull(floor.takeIf { it.isNotBlank() }, unit.takeIf { it.isNotBlank() })
            .joinToString(" · "))
    }
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
