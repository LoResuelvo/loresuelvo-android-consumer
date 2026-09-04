package com.loresuelvo.consumer.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight

/**
 * Reusable section header: title on the left, optional "See all"
 * (or similar) link on the right. Used across the Home screen for
 * the categories / active requests / recent diagnoses sections.
 *
 * [linkTestTag] is opt-in: when the same link text appears on
 * multiple sections of the Home (e.g. "Ver todas" is reused by
 * the categories section AND the new Mis Servicios entry) the
 * caller can tag the link so the instrumented / Compose tests
 * can target a specific instance via
 * `onNodeWithTag(linkTestTag).performClick()` rather than relying
 * on the ambiguous text label.
 */
@Composable
fun SectionTitle(
    text: String,
    link: String? = null,
    linkTestTag: String? = null,
    onLinkClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (link != null) {
            TextButton(
                onClick = onLinkClick,
                modifier = if (linkTestTag != null) Modifier.testTag(linkTestTag) else Modifier,
            ) {
                Text(
                    text = link,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Canonical testTag for the Home "Mis Servicios" entry link. Keep
 * the value in sync with the consumer (`MisServiciosScreenInstrumentedTest`).
 */
const val HOME_MIS_SERVICIOS_LINK_TAG: String = "home-mis-servicios-link"
