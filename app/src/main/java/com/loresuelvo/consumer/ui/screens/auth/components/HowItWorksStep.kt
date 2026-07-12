package com.loresuelvo.consumer.ui.screens.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.theme.BrandSecondary
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * A single "how it works" row: a numbered teal badge plus a title
 * and a supporting description. Stateless; used to explain the
 * consumer journey (describe -> diagnose -> connect) before asking
 * the user to register.
 */
@Composable
fun HowItWorksStep(
    number: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(BrandSecondary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandSecondary,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = SubtitleGray,
                lineHeight = 18.sp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HowItWorksStepPreview() {
    LoresuelvoTheme {
        HowItWorksStep(
            number = 1,
            title = stringResource(R.string.welcome_step1_title),
            description = stringResource(R.string.welcome_step1_description),
        )
    }
}
