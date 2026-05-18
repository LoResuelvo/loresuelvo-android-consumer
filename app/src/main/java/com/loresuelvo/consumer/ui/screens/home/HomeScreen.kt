package com.loresuelvo.consumer.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.domain.auth.AuthSession

@Composable
fun HomeScreen(
    authSession: AuthSession,
    onLogoutClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "Hola,",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = authSession.user.displayName,
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            onClick = onLogoutClick
        ) {
            Text("Cerrar sesión")
        }
    }
}