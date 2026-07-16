package com.loresuelvo.consumer.ui.screens.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.loresuelvo.consumer.ui.theme.TextWhite

/**
 * Header block for the CompleteProfile screen: a small logo at the
 * top and the hero copy (title + subtitle) that frames the form.
 *
 * Stateless: every value comes from `strings.xml` (es + en).
 *
 * Visual contract is identical to the inline header that used to
 * live in `CompleteProfileScreen`: same colors, same typography,
 * same spacing. The split is purely structural — separating the
 * header from the form makes the upcoming redesign easier to land
 * without rewriting the screen at the same time.
 */
@Composable
fun ProfileHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLogo()

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.complete_profile_title),
            style = MaterialTheme.typography.displaySmall,
            color = TextWhite,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.complete_profile_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
    }
}