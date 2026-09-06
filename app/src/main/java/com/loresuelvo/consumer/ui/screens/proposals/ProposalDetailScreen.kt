package com.loresuelvo.consumer.ui.screens.proposals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Modal bottom sheet that renders the full [ServiceProposal]
 * (US-54 scenario 08-VSP). Mounted as the detail surface that
 * `ProposalCard` opens from the Home dashboard or from
 * MisServicios. Stateless: every visible value is sourced from
 * [ProposalDetailUiState] and the only user action (the "Ver
 * conversación" CTA) is delegated via [onViewConversation].
 *
 * State rendering:
 *  - [ProposalDetailUiState.Loading] → centred spinner.
 *  - [ProposalDetailUiState.Ready] → full proposal layout (foto +
 *    nombre + rubro + monto + fecha + descripción + estado + CTA).
 *  - [ProposalDetailUiState.Error] → typed copy + retry button.
 *
 * The CTA is hidden when `proposal.conversationId == null` so the
 * screen never offers a navigation that would later crash.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalDetailScreen(
    state: ProposalDetailUiState,
    onRetry: () -> Unit,
    onViewConversation: (conversationId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(PROPOSAL_DETAIL_SHEET_TAG),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (state) {
                is ProposalDetailUiState.Loading -> LoadingState()
                is ProposalDetailUiState.Ready -> ReadyState(
                    proposal = state.proposal,
                    onViewConversation = { onViewConversation(state.proposal.conversationId ?: "") },
                )
                is ProposalDetailUiState.Error -> ErrorState(
                    failure = state.failure,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(PROPOSAL_DETAIL_LOADING_TAG),
        )
        Text(
            text = stringResource(R.string.proposal_detail_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun ReadyState(
    proposal: ServiceProposal,
    onViewConversation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(PROPOSAL_DETAIL_READY_TAG),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderAvatar(
                name = proposal.counterpart.name,
                profilePhotoUrl = proposal.counterpart.profilePhotoUrl,
                size = 56.dp,
                testTag = PROPOSAL_DETAIL_AVATAR_TAG,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${proposal.counterpart.name} ${proposal.counterpart.surname}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = proposal.counterpart.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SubtitleGray,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        DetailRow(
            label = stringResource(R.string.proposal_detail_amount),
            value = "$" + proposal.amountCents.toString(),
        )
        DetailRow(
            label = stringResource(R.string.proposal_detail_reason),
            value = proposal.description,
        )
        DetailRow(
            label = stringResource(R.string.proposal_detail_status),
            value = statusLabel(proposal.status),
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (proposal.conversationId != null) {
            Button(
                onClick = onViewConversation,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PROPOSAL_DETAIL_VIEW_CONVERSATION_TAG),
            ) {
                Text(
                    text = stringResource(R.string.proposal_detail_view_conversation),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    failure: ServiceProposalsOutcome.Failure,
    onRetry: () -> Unit,
) {
    val message = when (failure) {
        is ServiceProposalsOutcome.Failure.Network ->
            stringResource(R.string.proposal_detail_error_network)
        is ServiceProposalsOutcome.Failure.Server ->
            stringResource(R.string.proposal_detail_error_server)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .testTag(PROPOSAL_DETAIL_ERROR_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag(PROPOSAL_DETAIL_ERROR_RETRY_TAG),
        ) {
            Text(stringResource(R.string.proposal_detail_error_retry))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = SubtitleGray,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun statusLabel(status: ServiceProposalStatus): String = when (status) {
    ServiceProposalStatus.Pending -> "Pendiente"
    ServiceProposalStatus.Accepted -> "Aceptada"
    ServiceProposalStatus.Rejected -> "Rechazada"
}

const val PROPOSAL_DETAIL_SHEET_TAG: String = "proposal-detail-sheet"
const val PROPOSAL_DETAIL_LOADING_TAG: String = "proposal-detail-loading"
const val PROPOSAL_DETAIL_READY_TAG: String = "proposal-detail-ready"
const val PROPOSAL_DETAIL_ERROR_TAG: String = "proposal-detail-error"
const val PROPOSAL_DETAIL_ERROR_RETRY_TAG: String = "proposal-detail-error-retry"
const val PROPOSAL_DETAIL_VIEW_CONVERSATION_TAG: String = "proposal-detail-view-conversation"
const val PROPOSAL_DETAIL_AVATAR_TAG: String = "proposal-detail-avatar"
