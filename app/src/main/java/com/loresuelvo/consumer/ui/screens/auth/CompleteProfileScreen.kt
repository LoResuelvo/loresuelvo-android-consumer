package com.loresuelvo.consumer.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loresuelvo.consumer.ui.components.branding.AppLogo
import com.loresuelvo.consumer.ui.components.buttons.PrimaryButton
import com.loresuelvo.consumer.ui.components.cards.AuthCard
import com.loresuelvo.consumer.ui.components.inputs.PrimaryTextField
import com.loresuelvo.consumer.ui.theme.AppBackgroundBottom
import com.loresuelvo.consumer.ui.theme.AppBackgroundMiddle
import com.loresuelvo.consumer.ui.theme.AppBackgroundTop
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.theme.TextWhite

@Composable
fun CompleteProfileScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppBackgroundTop,
                        AppBackgroundMiddle,
                        AppBackgroundBottom
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        AppLogo()

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Completa tu perfil",
            style = MaterialTheme.typography.displaySmall,
            color = TextWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Necesitamos algunos datos para continuar.",
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        AuthCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            PrimaryTextField(
                value = "",
                label = "Nombre",
                placeholder = "Ej. Juan",
                onValueChange = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryTextField(
                value = "",
                label = "Apellido",
                placeholder = "Ej. Pérez",
                onValueChange = {}
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Continuar",
                onClick = {}
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tus datos serán visibles para los profesionales que acepten tus solicitudes.",
                style = MaterialTheme.typography.bodySmall,
                color = SubtitleGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}