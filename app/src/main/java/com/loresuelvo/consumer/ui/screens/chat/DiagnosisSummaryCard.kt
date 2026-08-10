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
const val CHAT_DIAGNOSIS_CATEGORY_TAG: String = "chat-diagnosis-category"
const val CHAT_DIAGNOSIS_PROVIDER_ROW_TAG: String = "chat-diagnosis-provider-row"
const val CHAT_DIAGNOSIS_PROVIDER_CATEGORY_TAG: String = "chat-diagnosis-provider-category"

/**
 * Summary card rendered below the chat history once the AI
 * concludes the diagnosis. Shows the matched rubro and the list of
 * providers the AI recommends for it. The AI's free-text
 * explanation is intentionally NOT echoed here because the same
 * content already lives in the last assistant chat bubble above
 * this card — duplicating it would split the narrative across the
 * two surfaces.
 */
@Composable
fun DiagnosisSummaryCard(
    categoryName: String?,
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
            if (categoryName != null) {
                Text(
                    text = stringResource(
                        R.string.chat_diagnosis_category_format,
                        categoryName,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(CHAT_DIAGNOSIS_CATEGORY_TAG),
                )
            }
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
                text = "${provider.name} ${provider.surname}".trim(),
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
