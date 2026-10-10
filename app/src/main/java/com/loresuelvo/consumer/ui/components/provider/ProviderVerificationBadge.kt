package com.loresuelvo.consumer.ui.components.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

const val PROVIDER_VERIFICATION_BADGE_TAG = "provider-verification-badge"

@Composable
fun ProviderVerificationBadge(
    showLabel: Boolean = false,
    modifier: Modifier = Modifier,
    testTag: String = PROVIDER_VERIFICATION_BADGE_TAG,
) {
    val badgeDescription = stringResource(
        R.string.provider_verified_badge_content_description,
    )
    Surface(
        modifier = modifier
            .testTag(testTag)
            .semantics {
                contentDescription = badgeDescription
            },
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (showLabel) 8.dp else 6.dp,
                vertical = 4.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            if (showLabel) {
                Text(
                    text = stringResource(R.string.provider_verified_badge_label),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
