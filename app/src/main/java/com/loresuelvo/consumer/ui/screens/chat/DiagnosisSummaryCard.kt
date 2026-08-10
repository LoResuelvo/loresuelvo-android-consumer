package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar

const val CHAT_DIAGNOSIS_SUMMARY_TAG: String = "chat-diagnosis-summary"
const val CHAT_DIAGNOSIS_ASSESSMENT_TAG: String = "chat-diagnosis-assessment"
const val CHAT_DIAGNOSIS_PROVIDER_ROW_TAG: String = "chat-diagnosis-provider-row"
const val CHAT_DIAGNOSIS_PROVIDER_CATEGORY_TAG: String = "chat-diagnosis-provider-category"

@Composable
fun DiagnosisSummaryCard(
    assessment: String,
    providers: List<Provider>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CHAT_DIAGNOSIS_SUMMARY_TAG),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.chat_diagnosis_assessment_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = assessment,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(CHAT_DIAGNOSIS_ASSESSMENT_TAG),
                )
            }
            if (providers.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.chat_diagnosis_providers_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    providers.forEach { provider ->
                        RecommendedProviderRow(provider = provider)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedProviderRow(provider: Provider) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CHAT_DIAGNOSIS_PROVIDER_ROW_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderAvatar(
            name = provider.name,
            profilePhotoUrl = provider.profilePhotoUrl,
            testTag = "$CHAT_DIAGNOSIS_PROVIDER_ROW_TAG-${provider.id}-avatar",
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "${provider.name} ${provider.surname}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = provider.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(CHAT_DIAGNOSIS_PROVIDER_CATEGORY_TAG),
            )
        }
    }
}
