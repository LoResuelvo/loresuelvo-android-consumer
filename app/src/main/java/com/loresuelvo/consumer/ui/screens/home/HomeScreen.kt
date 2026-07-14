package com.loresuelvo.consumer.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.session.SessionError

@Composable
fun HomeScreen(
    authSession: AuthSession,
    signingOut: Boolean = false,
    logoutError: SessionError? = null,
    onLogoutClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.titleMedium
        )

        authSession.user.firstName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Button(
            onClick = onLogoutClick,
            enabled = !signingOut,
        ) {
            if (signingOut) {
                CircularProgressIndicator()
            } else {
                Text(stringResource(R.string.home_logout))
            }
        }

        if (logoutError != null) {
            Text(
                text = stringResource(R.string.home_logout_error),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
