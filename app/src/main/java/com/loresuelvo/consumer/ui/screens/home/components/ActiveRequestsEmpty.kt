package com.loresuelvo.consumer.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Empty-state row for the "Solicitudes en curso" section. Shown
 * when the consumer has no in-flight requests. Distinct from
 * [RecentDiagnosesEmpty] because the copy is different and the
 * source of truth will be a different backend resource.
 */
@Composable
fun ActiveRequestsEmpty(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "\uD83D\uDEE0",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_requests_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = SubtitleGray,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveRequestsEmptyPreview() {
    LoresuelvoTheme {
        Column(modifier = Modifier.padding(24.dp)) {
            ActiveRequestsEmpty()
        }
    }
}
