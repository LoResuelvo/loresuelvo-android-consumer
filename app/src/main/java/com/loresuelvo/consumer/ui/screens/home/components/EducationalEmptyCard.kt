package com.loresuelvo.consumer.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Educational empty-state card used by the Home dashboard
 * sections that share the same "title + items | title + body +
 * CTA" pattern: Diagnósticos recientes, Mis Servicios (Home
 * row) and Mis Turnos (andamiaje until the endpoint lands).
 *
 * Layout: short title (why the list is empty), one-line body
 * (what to do about it), and a pill-shaped CTA that opens the
 * conversion flow tied to that section (AI diagnosis, category
 * grid, etc.). The card carries a `Surface` so the empty state
 * reads as a discrete affordance on the dashboard rather than
 * floating text.
 *
 * Stateless: the parent (`HomeScreen`) wires the click to the
 * appropriate callback (`onAiSendClick`, `onSeeAllCategoriesClick`,
 * etc.) — the card only renders.
 *
 * [modifier] lets the caller apply a `testTag` (e.g. the Home
 * MisServicios empty-state tag) without this composable having
 * to know about each consumer's test contract.
 */
@Composable
fun EducationalEmptyCard(
    title: String,
    body: String,
    ctaText: String,
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
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
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
                    text = ctaText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EducationalEmptyCardPreview() {
    LoresuelvoTheme {
        Column(modifier = Modifier.padding(24.dp)) {
            EducationalEmptyCard(
                title = "Probá el diagnóstico con IA",
                body = "Contale qué te pasa y te decimos qué tipo de profesional necesitás. Tarda 30 segundos.",
                ctaText = "Iniciar diagnóstico",
                onCtaClick = {},
            )
        }
    }
}
