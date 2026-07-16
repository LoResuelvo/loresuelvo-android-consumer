package com.loresuelvo.consumer.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * Educational empty-state for the "Diagnósticos recientes" section.
 *
 * Mirrors [ActiveRequestsEmpty]: a short title that frames why the
 * list is empty, a one-line body that tells the user what to do,
 * and a pill-shaped CTA that opens the AI diagnosis flow. The
 * section title (`home_section_diagnoses`) already lives in the
 * parent `SectionTitle`, so we don't repeat it here.
 *
 * Stateless: the parent (`HomeScreen`) wires the click to its
 * existing `onAiSendClick` so the user lands in the AI flow.
 */
@Composable
fun RecentDiagnosesEmpty(
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.home_diagnoses_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_diagnoses_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = SubtitleGray,
            )
            OutlinedButton(
                onClick = onCtaClick,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.home_diagnoses_empty_cta),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(18.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentDiagnosesEmptyPreview() {
    LoresuelvoTheme {
        Column(modifier = Modifier.padding(24.dp)) {
            RecentDiagnosesEmpty(onCtaClick = {})
        }
    }
}