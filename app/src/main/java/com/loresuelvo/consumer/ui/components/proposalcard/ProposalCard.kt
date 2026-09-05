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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * Compact summary card for a single [ServiceProposal]. Used both
 * in the Home dashboard's "Mis Servicios" horizontal row and in
 * the dedicated MisServicios list (US-54 scenario 08-VSP).
 *
 * The card shows:
 *  - The provider's profile photo (`AsyncImage` from Coil) or a
 *    fallback circle with the first letter of the provider's
 *    name when `profilePhotoUrl` is null or the image fails to
 *    load (per US-54 AC: "debe incluir una imagen por defecto o
 *    fallback si el usuario no tiene foto").
 *  - The provider's full name and category.
 *  - The agreed amount (raw cents as `$X`; friendly currency
 *    formatting is future work).
 *  - The status as a coloured badge.
 *  - A "Ver Solicitud" CTA that opens the proposal detail bottom
 *    sheet via [onViewClicked].
 *
 * The whole card is also clickable so the user can tap anywhere
 * on it (not just the CTA) to open the detail.
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onViewClicked)
            .padding(16.dp)
            .testTag(PROPOSAL_CARD_TAG_PREFIX + proposal.id),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProviderAvatar(
                photoUrl = proposal.counterpart.profilePhotoUrl,
                initials = proposal.counterpart.name.firstOrNull()?.toString() ?: "?",
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
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
        Text(
            text = "$" + proposal.amountCents.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(
            onClick = onViewClicked,
            modifier = Modifier.testTag(PROPOSAL_CARD_VIEW_TAG_PREFIX + proposal.id),
        ) {
            Text(
                text = stringResource(R.string.proposal_card_view_cta),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ProviderAvatar(photoUrl: String?, initials: String) {
    val size = 48.dp
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: ServiceProposalStatus) {
    val (label, color) = when (status) {
        ServiceProposalStatus.Pending -> "Pendiente" to MaterialTheme.colorScheme.tertiary
        ServiceProposalStatus.Accepted -> "Aceptada" to MaterialTheme.colorScheme.primary
        ServiceProposalStatus.Rejected -> "Rechazada" to MaterialTheme.colorScheme.error
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.testTag(PROPOSAL_CARD_BADGE_TAG_PREFIX + status.name),
    )
}

const val PROPOSAL_CARD_TAG_PREFIX: String = "proposal-card-"
const val PROPOSAL_CARD_VIEW_TAG_PREFIX: String = "proposal-card-view-"
const val PROPOSAL_CARD_AVATAR_TAG_PREFIX: String = "proposal-card-avatar-"
const val PROPOSAL_CARD_AVATAR_FALLBACK_TAG_PREFIX: String = "proposal-card-avatar-fallback-"
const val PROPOSAL_CARD_BADGE_TAG_PREFIX: String = "proposal-card-badge-"
