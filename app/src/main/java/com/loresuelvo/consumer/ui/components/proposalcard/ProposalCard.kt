package com.loresuelvo.consumer.ui.components.proposalcard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import coil3.compose.AsyncImage
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Compact horizontal summary card for a single [ServiceProposal].
 * Used both in the Home dashboard's "Mis Servicios" row and in the
 * dedicated MisServicios list (US-54 scenario 08-VSP).
 *
 * Layout (top to bottom):
 *  - Row 1: avatar (circular, 56dp) + name / category column
 *    + status badge (pill, top-right corner).
 *  - Row 2: amount (formatted with thousands separator) on the
 *    left, "Ver solicitud →" CTA on the right.
 *
 * The whole card is clickable so the consumer can tap anywhere
 * on it (not just the CTA) to open the detail bottom sheet.
 *
 * Colours come from [MaterialTheme.colorScheme] so the card
 * follows the LoResuelvo brand (primary = teal-dark, tertiary =
 * amber for "Pendiente", etc.) without hard-coding hex values.
 */
@Composable
fun ProposalCard(
    proposal: ServiceProposal,
    onViewClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onViewClicked)
            .padding(16.dp)
            .testTag(PROPOSAL_CARD_TAG_PREFIX + proposal.id),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProviderAvatar(
                photoUrl = proposal.counterpart.profilePhotoUrl,
                initials = proposal.counterpart.name.firstOrNull()?.toString() ?: "?",
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${proposal.counterpart.name} ${proposal.counterpart.surname}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = proposal.counterpart.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtitleGray,
                )
            }
            StatusBadge(status = proposal.status)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatAmount(proposal.amountCents),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            ViewRequestCta(onClick = onViewClicked, proposalId = proposal.id)
        }
    }
}

@Composable
private fun ProviderAvatar(photoUrl: String?, initials: String) {
    val size = 56.dp
    if (photoUrl != null) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .testTag(PROPOSAL_CARD_AVATAR_TAG_PREFIX + initials),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .testTag(PROPOSAL_CARD_AVATAR_FALLBACK_TAG_PREFIX + initials),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Pill-shaped status badge that picks semantic colours from the
 * brand palette: "Pendiente" on amber (`tertiary`), "Aceptada"
 * on teal-dark (`primary`), "Rechazada" on red (`error`).
 * Compact — fits in the top-right corner of the card without
 * competing with the name.
 */
@Composable
private fun StatusBadge(status: ServiceProposalStatus) {
    val (label, containerColor, contentColor) = when (status) {
        ServiceProposalStatus.Pending -> Triple(
            "Pendiente",
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
        )
        ServiceProposalStatus.Accepted -> Triple(
            "Aceptada",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        ServiceProposalStatus.Rejected -> Triple(
            "Rechazada",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
        )
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = Modifier.testTag(PROPOSAL_CARD_BADGE_TAG_PREFIX + status.name),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * Compact right-aligned CTA: "Ver solicitud →" rendered as a
 * clickable text instead of a full-width button so it does not
 * dominate the card. The trailing arrow is part of the string
 * (no Material icon dependency) so the visual weight stays
 * minimal.
 */
@Composable
private fun ViewRequestCta(onClick: () -> Unit, proposalId: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(PROPOSAL_CARD_VIEW_TAG_PREFIX + proposalId),
    ) {
        Text(
            text = stringResource(R.string.proposal_card_view_cta),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "→",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Formats `amountCents` as a peso string with thousands
 * separator (`$100.000`, `$1.500.000`). Inline helper to avoid a
 * dependency on `java.text.NumberFormat` (whose default locale
 * uses comma separators).
 */
private fun formatAmount(amountCents: Long): String {
    val whole = amountCents / 100
    val digits = whole.toString()
    val reversed = digits.reversed()
    val withDots = buildString(reversed.length + reversed.length / 3) {
        reversed.forEachIndexed { index, c ->
            if (index > 0 && index % 3 == 0) append('.')
            append(c)
        }
    }.reversed()
    return "$$withDots"
}

const val PROPOSAL_CARD_TAG_PREFIX: String = "proposal-card-"
const val PROPOSAL_CARD_VIEW_TAG_PREFIX: String = "proposal-card-view-"
const val PROPOSAL_CARD_AVATAR_TAG_PREFIX: String = "proposal-card-avatar-"
const val PROPOSAL_CARD_AVATAR_FALLBACK_TAG_PREFIX: String = "proposal-card-avatar-fallback-"
const val PROPOSAL_CARD_BADGE_TAG_PREFIX: String = "proposal-card-badge-"
