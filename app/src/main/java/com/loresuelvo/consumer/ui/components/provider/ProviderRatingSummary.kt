package com.loresuelvo.consumer.ui.components.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.ui.theme.SubtitleGray

@Composable
fun ProviderRatingSummary(
    provider: Provider,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Row(
        modifier = modifier.then(
            if (testTag != null) Modifier.testTag(testTag) else Modifier,
        ),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (provider.ratingCount == 0) {
            Text(
                text = stringResource(R.string.professionals_provider_no_rating),
                color = SubtitleGray,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            val rating = provider.ratingAverage.coerceIn(0.0, 5.0)
            repeat(5) { index ->
                RatingStar(
                    fillFraction = (rating - index).coerceIn(0.0, 1.0).toFloat(),
                )
            }
            Text(
                text = stringResource(
                    R.string.professionals_provider_rating,
                    provider.ratingAverage,
                    provider.ratingCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = SubtitleGray,
            )
        }
    }
}

@Composable
private fun RatingStar(fillFraction: Float) {
    Box(modifier = Modifier.size(14.dp)) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            modifier = Modifier.size(14.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(14.dp * fillFraction)
                .clipToBounds(),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
