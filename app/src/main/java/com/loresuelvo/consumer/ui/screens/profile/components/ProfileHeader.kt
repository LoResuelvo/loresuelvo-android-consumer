package com.loresuelvo.consumer.ui.screens.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.components.branding.AppLogo

/**
 * Header block for the CompleteProfile screen: a 96dp logo and
 * the hero copy (title + subtitle) that frames the form.
 *
 * Stateless: every value comes from `strings.xml` (es + en). Colors
 * follow the active MaterialTheme so the header reads correctly on
 * the light surface that the screen uses after the redesign.
 */
@Composable
fun ProfileHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLogo(size = 96.dp)

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.complete_profile_hero_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.complete_profile_hero_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}