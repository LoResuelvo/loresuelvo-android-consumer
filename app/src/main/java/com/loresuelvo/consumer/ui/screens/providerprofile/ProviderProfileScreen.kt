package com.loresuelvo.consumer.ui.screens.providerprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.provider.ProviderProfile
import com.loresuelvo.consumer.domain.provider.ProviderReview
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrder
import com.loresuelvo.consumer.ui.components.provider.ProviderVerificationBadge
import com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.util.ScheduledDateFormatter
import java.util.Locale

const val PROVIDER_PROFILE_RATING_STARS_TAG: String = "provider-profile-rating-stars"
const val PROVIDER_PROFILE_WORK_ORDER_LIST_TAG: String = "provider-profile-work-order-list"
const val PROVIDER_PROFILE_AMOUNT_TAG: String = "provider-profile-private-amount"
const val PROVIDER_PROFILE_CLIENT_TAG: String = "provider-profile-private-client"
const val PROVIDER_PROFILE_EVIDENCE_PHOTOS_TAG: String = "provider-profile-private-evidence-photos"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderProfileScreen(
    state: ProviderProfileUiState,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.provider_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(
                                R.string.provider_profile_back_content_description,
                            ),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        when (val current = state) {
            ProviderProfileUiState.Loading -> LoadingState(Modifier.padding(padding))
            is ProviderProfileUiState.Error -> ErrorState(
                modifier = Modifier.padding(padding),
                failure = current.failure,
                onRetryClick = onRetryClick,
            )
            is ProviderProfileUiState.Ready -> ReadyState(
                modifier = Modifier.padding(padding),
                profile = current.profile,
            )
        }
    }
}

@Composable
private fun ReadyState(
    profile: ProviderProfile,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ProviderHeader(profile) }
        item { ProviderReputation(profile) }
        item {
            Text(
                text = stringResource(R.string.provider_profile_history_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        if (profile.workOrders.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.provider_profile_history_empty),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("provider-profile-history-empty"),
                    color = SubtitleGray,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(
                items = profile.workOrders,
                key = { it.id },
            ) { workOrder ->
                ProviderWorkOrderCard(workOrder)
            }
        }
    }
}

@Composable
private fun ProviderHeader(profile: ProviderProfile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderAvatar(
            name = profile.name,
            profilePhotoUrl = profile.profilePhotoUrl,
            size = 72.dp,
            testTag = "provider-profile-avatar",
        )
        Spacer(Modifier.size(16.dp))
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "${profile.name} ${profile.surname}".trim(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (profile.identityVerified) {
                    ProviderVerificationBadge(showLabel = true)
                }
            }
            Text(
                text = profile.category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = SubtitleGray,
            )
        }
    }
}

@Composable
private fun ProviderReputation(profile: ProviderProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.provider_profile_reputation_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            RatingStars(
                rating = profile.ratingAverage,
                modifier = Modifier.testTag(PROVIDER_PROFILE_RATING_STARS_TAG),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = formatRating(profile.ratingAverage),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        R.string.provider_profile_review_count,
                        profile.ratingCount,
                    ),
                    color = SubtitleGray,
                )
            }
        }
    }
}

@Composable
private fun ProviderWorkOrderCard(workOrder: ProviderWorkOrder) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider-profile-work-order-${workOrder.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.provider_profile_scheduled_date,
                    ScheduledDateFormatter.formatScheduled(workOrder.scheduledOnEpochMillis),
                ),
                style = MaterialTheme.typography.labelLarge,
                color = SubtitleGray,
            )
            LabelledText(
                label = stringResource(R.string.provider_profile_work_description),
                value = workOrder.description,
            )
            workOrder.completionReport?.let { report ->
                LabelledText(
                    label = stringResource(R.string.provider_profile_completion_report),
                    value = report.description,
                )
            }
            workOrder.review?.let { review ->
                ProviderReviewBlock(workOrder.id, review)
            }
        }
    }
}

@Composable
private fun ProviderReviewBlock(workOrderId: String, review: ProviderReview) {
    Column(
        modifier = Modifier.testTag("provider-profile-review-$workOrderId"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.provider_profile_review_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        RatingStars(
            rating = review.rating.toDouble(),
            modifier = Modifier.testTag("provider-profile-review-stars-$workOrderId"),
        )
        Text(text = review.description)
    }
}

@Composable
private fun LabelledText(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = SubtitleGray,
        )
        Text(text = value)
    }
}

@Composable
private fun RatingStars(
    rating: Double,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    failure: com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome.Failure,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (failure) {
        is com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome.Failure.Network ->
            stringResource(R.string.provider_profile_error_network)
        is com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome.Failure.Server ->
            stringResource(R.string.provider_profile_error_server)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetryClick) {
            Text(stringResource(R.string.provider_profile_retry))
        }
    }
}

private fun formatRating(rating: Double): String =
    String.format(Locale.getDefault(), "%.1f", rating)
