package com.loresuelvo.consumer.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.ui.components.branding.AppLogo
import com.loresuelvo.consumer.ui.components.branding.VerifiedProfessionalsStrip
import com.loresuelvo.consumer.ui.components.buttons.GoogleButton
import com.loresuelvo.consumer.ui.components.buttons.PrimaryButton
import com.loresuelvo.consumer.ui.components.buttons.SecondaryButton
import com.loresuelvo.consumer.ui.components.cards.AuthCard
import com.loresuelvo.consumer.ui.theme.AppBackgroundBottom
import com.loresuelvo.consumer.ui.theme.AppBackgroundMiddle
import com.loresuelvo.consumer.ui.theme.AppBackgroundTop
import com.loresuelvo.consumer.ui.theme.DividerGray
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.theme.TextWhite

@Composable
fun WelcomeScreen(
    authSession: AuthSession? = null,
    onRegisterClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onGoogleClick: () -> Unit = {}
) {

    Box(
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
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(12.dp)
        )

        if (authSession != null) {
            AuthenticatedHeader(
                displayName = authSession.user.displayName,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                AppLogo()

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "LoResuelvo",
                    style = MaterialTheme.typography.displaySmall,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Expertos en el cuidado de tu hogar, a un toque de distancia.",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                AuthCard(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    PrimaryButton(
                        text = "Registrarse",
                        onClick = onRegisterClick
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SecondaryButton(
                        text = "Iniciar Sesión",
                        onClick = onLoginClick
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    DividerWithLabel(label = "o")

                    Spacer(modifier = Modifier.height(22.dp))

                    GoogleButton(
                        text = "Continuar con Google",
                        onClick = onGoogleClick
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Al continuar, aceptas nuestros Términos de Servicio y Política de Privacidad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SubtitleGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                VerifiedProfessionalsStrip()
            }
        }
    }
}

@Composable
private fun AuthenticatedHeader(
    displayName: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {

        Text(
            text = "Hola,",
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite.copy(alpha = 0.82f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DividerWithLabel(
    label: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = DividerGray
        )

        Text(
            text = "  $label  ",
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = DividerGray
        )
    }
}
