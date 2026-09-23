package com.loresuelvo.consumer.ui.components.turnocard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
 * on it; the card body is non-clickable (avoids accidental
 * taps while scrolling) and the only action surface is the
 * trailing "Ver detalles" / "View details" TextButton at the
 * bottom of the card. Tapping the CTA fires [onDetailsClick].
 *
 * Status labels are pulled from `R.string.turno_status_*` so the
 * i18n contract is honoured (unlike `ProposalCard.StatusBadge`
 * which has hard-coded Spanish literals).
 */
@Composable
fun TurnoCard(
    turno: Turno,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
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
            TurnoStatusBadge(status = turno.status, scheduledOnEpochMillis = turno.scheduledOnEpochMillis)
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
            // Date on the left + "Ver detalles" CTA aligned to the
            // right — `Arrangement.SpaceBetween` parks the CTA in the
            // bottom-right corner of the card without forcing a
            // full-width button row (matches the
            // [ProposalCard] bottom row pattern).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = ScheduledDateFormatter.formatScheduled(turno.scheduledOnEpochMillis),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(TURNO_CARD_DATE_TAG + turno.id),
                )
                ViewRequestCta(
                    onClick = onDetailsClick,
                    turnId = turno.id,
                )
            }
        }
    }
}

/**
 * Pill-shaped status badge for [TurnoStatus]. Picks semantic
 * colours from the brand palette and pulls the label from
 * `R.string.turno_status_*`.
 */
@Composable
private fun TurnoStatusBadge(
    status: TurnoStatus,
    scheduledOnEpochMillis: Long,
) {
    // When the turno is scheduled for today AND status is
    // Confirmed, render the "(hoy)" pill in light green instead
    // of the regular "Confirmado" pill. UTC consistency matches
    // `ScheduledDateFormatter` (which formats in UTC too).
    val isToday = run {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = scheduledOnEpochMillis
        val now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
    }
    val showTodayLabel = status == TurnoStatus.Confirmed && isToday
    val (labelResId, containerColor, contentColor) = if (showTodayLabel) {
        Triple(
            R.string.turno_today_label,
            // Light green surfaces onMaterial3 are `secondaryContainer`
            // in the default light theme — visually distinct from
            // the primary teal the regular "Confirmado" pill uses.
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    } else when (status) {
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
        TurnoStatus.AwaitingPayment -> Triple(
            R.string.turno_status_awaiting_payment,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
        )
        TurnoStatus.Paid -> Triple(
            R.string.turno_status_paid,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
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

// turno_details_cta
@Composable
private fun ViewRequestCta(
    onClick: () -> Unit,
    turnId: String,
) {
    val shape = RoundedCornerShape(percent = 50)

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(TURNO_CARD_DETAILS_CTA_TAG + turnId),
    ) {
        Text(
            text = stringResource(R.string.turno_details_cta),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 8.dp,
            ),
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
const val TURNO_CARD_DETAILS_CTA_TAG: String = "turno-card-details-"
