package com.loresuelvo.consumer.ui.components.turnocard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.util.CurrencyFormatter
import com.loresuelvo.consumer.ui.util.ScheduledDateFormatter

/**
 * Compact card for a single [Turno] (visualize-turns.feature
 * scenario 04-VT).
 *
 * Layout (top to bottom):
 *  - Row 1: avatar (circular, 56dp) + name / category column
 *    + status badge (pill, top-right corner).
 *  - Row 2: description + amount + scheduled date, left-aligned.
 *
 * The whole card is clickable so the consumer can tap anywhere
 * on it; tapping fires [onCardClicked]. The detail screen and
 * the "Contactar" CTA land with their own scenarios (post-MVP
 * today).
 *
 * Status labels are pulled from `R.string.turno_status_*` so the
 * i18n contract is honoured (unlike `ProposalCard.StatusBadge`
 * which has hard-coded Spanish literals).
 */
@Composable
fun TurnoCard(
    turno: Turno,
    onCardClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onCardClicked)
            .padding(8.dp)
            .testTag(TURNO_CARD_TAG_PREFIX + turno.id),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ProviderAvatar(
                name = turno.counterpart.name,
                profilePhotoUrl = turno.counterpart.profilePhotoUrl,
                size = 56.dp,
                testTag = TURNO_CARD_AVATAR_TAG_PREFIX + turno.counterpart.id,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${turno.counterpart.name} ${turno.counterpart.surname}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = turno.counterpart.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtitleGray,
                )
            }
            TurnoStatusBadge(status = turno.status)
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = turno.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag(TURNO_CARD_DESCRIPTION_TAG + turno.id),
            )
            Text(
                text = CurrencyFormatter.formatAmount(turno.amountCents),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag(TURNO_CARD_AMOUNT_TAG + turno.id),
            )
            Text(
                text = ScheduledDateFormatter.formatScheduled(turno.scheduledOnEpochMillis),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag(TURNO_CARD_DATE_TAG + turno.id),
            )
        }
    }
}

/**
 * Pill-shaped status badge for [TurnoStatus]. Picks semantic
 * colours from the brand palette and pulls the label from
 * `R.string.turno_status_*`.
 */
@Composable
private fun TurnoStatusBadge(status: TurnoStatus) {
    val (labelResId, containerColor, contentColor) = when (status) {
        TurnoStatus.Pending -> Triple(
            R.string.turno_status_pending,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
        )
        TurnoStatus.Confirmed -> Triple(
            R.string.turno_status_confirmed,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        TurnoStatus.Finished -> Triple(
            R.string.turno_status_finished,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TurnoStatus.Cancelled -> Triple(
            R.string.turno_status_cancelled,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
        )
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
    ) {
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * Compose testTags for [TurnoCard]. Exported with the
 * `turno-card-` prefix so the instrumented suite can target
 * each slot independently without depending on the localised
 * copy.
 */
const val TURNO_CARD_TAG_PREFIX: String = "turno-card-"
const val TURNO_CARD_AVATAR_TAG_PREFIX: String = "turno-card-avatar-"
const val TURNO_CARD_DESCRIPTION_TAG: String = "turno-card-description-"
const val TURNO_CARD_AMOUNT_TAG: String = "turno-card-amount-"
const val TURNO_CARD_DATE_TAG: String = "turno-card-date-"
