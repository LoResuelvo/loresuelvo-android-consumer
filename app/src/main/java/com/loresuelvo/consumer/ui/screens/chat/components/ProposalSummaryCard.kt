package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.util.CurrencyFormatter
import com.loresuelvo.consumer.ui.util.ScheduledDateFormatter

@Composable
fun ProposalSummaryCard(
    proposal: ServiceProposal,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag(PROPOSAL_SUMMARY_CARD_TAG),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.conversation_proposal_summary_title),
                style = MaterialTheme.typography.labelMedium,
                color = SubtitleGray,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag(PROPOSAL_SUMMARY_TITLE_TAG),
            )
            SummaryRow(
                label = stringResource(R.string.conversation_proposal_summary_amount),
                value = CurrencyFormatter.formatAmount(proposal.amountCents),
                valueTestTag = PROPOSAL_SUMMARY_AMOUNT_TAG,
            )
            SummaryRow(
                label = stringResource(R.string.conversation_proposal_summary_date),
                value = ScheduledDateFormatter.formatScheduled(proposal.scheduledOnEpochMillis),
                valueTestTag = PROPOSAL_SUMMARY_DATE_TAG,
            )
            SummaryRow(
                label = stringResource(R.string.conversation_proposal_summary_description),
                value = proposal.description,
                valueTestTag = PROPOSAL_SUMMARY_DESCRIPTION_TAG,
            )
            SummaryRow(
                label = stringResource(R.string.conversation_proposal_summary_status),
                value = statusLabel(proposal.status),
                valueTestTag = PROPOSAL_SUMMARY_STATUS_TAG,
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueTestTag: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SubtitleGray,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag(valueTestTag),
        )
    }
}

@Composable
private fun statusLabel(status: ServiceProposalStatus): String = when (status) {
    ServiceProposalStatus.Pending ->
        stringResource(R.string.conversation_proposal_summary_status_pending)
    ServiceProposalStatus.Accepted ->
        stringResource(R.string.conversation_proposal_summary_status_accepted)
    ServiceProposalStatus.Rejected ->
        stringResource(R.string.conversation_proposal_summary_status_rejected)
}

const val PROPOSAL_SUMMARY_CARD_TAG: String = "conversation-proposal-summary-card"
const val PROPOSAL_SUMMARY_TITLE_TAG: String = "conversation-proposal-summary-title"
const val PROPOSAL_SUMMARY_AMOUNT_TAG: String = "conversation-proposal-summary-amount"
const val PROPOSAL_SUMMARY_DATE_TAG: String = "conversation-proposal-summary-date"
const val PROPOSAL_SUMMARY_DESCRIPTION_TAG: String = "conversation-proposal-summary-description"
const val PROPOSAL_SUMMARY_STATUS_TAG: String = "conversation-proposal-summary-status"