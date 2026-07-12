package com.loresuelvo.consumer.ui.screens.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.components.buttons.GoogleButton
import com.loresuelvo.consumer.ui.components.buttons.PrimaryButton
import com.loresuelvo.consumer.ui.screens.auth.components.CategoryChipRow
import com.loresuelvo.consumer.ui.screens.auth.components.HeroSection
import com.loresuelvo.consumer.ui.screens.auth.components.HowItWorksStep
import com.loresuelvo.consumer.ui.screens.auth.components.WelcomeScaffold
import com.loresuelvo.consumer.ui.screens.auth.components.WelcomeTopBar
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Welcome screen for the consumer app. Value-first layout: social
 * proof and the product journey are communicated before any
 * authentication prompt, so the user understands what LoResuelvo is
 * within the first seconds.
 *
 * Stateless: every visible value comes from resources and every user
 * action is delegated to a callback. The host ([com.loresuelvo.consumer.ui.navigation.LoResuelvoNav])
 * wires these callbacks to [com.loresuelvo.consumer.ui.auth.WelcomeViewModel].
 */
@Composable
fun WelcomeScreen(
    errorMessage: String? = null,
    onRegisterClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onGoogleClick: () -> Unit = {},
) {
    WelcomeScaffold {
        WelcomeTopBar(onLoginClick = onLoginClick)

        Spacer(Modifier.height(24.dp))

        HeroSection()

        Spacer(Modifier.height(28.dp))

        HowItWorksStep(
            number = 1,
            title = stringResource(R.string.welcome_step1_title),
            description = stringResource(R.string.welcome_step1_description),
        )
        Spacer(Modifier.height(14.dp))
        HowItWorksStep(
            number = 2,
            title = stringResource(R.string.welcome_step2_title),
            description = stringResource(R.string.welcome_step2_description),
        )
        Spacer(Modifier.height(14.dp))
        HowItWorksStep(
            number = 3,
            title = stringResource(R.string.welcome_step3_title),
            description = stringResource(R.string.welcome_step3_description),
        )

        Spacer(Modifier.height(24.dp))

        CategoryChipRow(
            categories = stringArrayResource(R.array.welcome_categories).toList(),
        )

        Spacer(Modifier.height(28.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }

        PrimaryButton(
            text = stringResource(R.string.welcome_register),
            onClick = onRegisterClick,
        )

        Spacer(Modifier.height(14.dp))

        GoogleButton(
            text = stringResource(R.string.welcome_google),
            onClick = onGoogleClick,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_legal),
            style = MaterialTheme.typography.bodySmall,
            color = SubtitleGray,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, name = "Welcome · phone")
@Preview(showBackground = true, name = "Welcome · small", heightDp = 640, widthDp = 320)
@Composable
private fun WelcomeScreenPreview() {
    LoresuelvoTheme {
        WelcomeScreen()
    }
}
