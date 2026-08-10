package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar

/**
 * Compose testTags for the diagnosis summary block. Kept verbatim
 * from the previous versions so the acceptance tests and the BDD
 * fixtures continue to locate the same surfaces.
 */
const val CHAT_DIAGNOSIS_SUMMARY_TAG: String = "chat-diagnosis-summary"
const val CHAT_DIAGNOSIS_CATEGORY_TAG: String = "chat-diagnosis-category"
const val CHAT_DIAGNOSIS_PROVIDERS_CAROUSEL_TAG: String = "chat-diagnosis-providers-carousel"
const val CHAT_DIAGNOSIS_PROVIDER_ROW_TAG: String = "chat-diagnosis-provider-row"

/**
 * Reserved for backward-compat with any caller/test that pinned
 * the per-provider category Text. The category is no longer
 * echoed inside each tile (it lives in the section header
 * `R.string.chat_diagnosis_category_format` above the carousel)
 * so this constant is currently unused inside the card. Kept
 * declared to honor "no elimines testTags existentes" — a
 * future rev that re-adds the per-card category line (for
 * instance in a richer category list) can re-use it without
 * re-shipping the test surface.
 */
@Suppress("unused")
const val CHAT_DIAGNOSIS_PROVIDER_CATEGORY_TAG: String = "chat-diagnosis-provider-category"

/**
 * Per-provider name testTag. Applied to the "Name Surname" Text
 * inside each carousel tile.
 */
const val CHAT_DIAGNOSIS_PROVIDER_NAME_TAG: String = "chat-diagnosis-provider-name"

/**
 * Prefix for the per-provider "Contactar" button testTag. The
 * composed tag becomes
 * `"$CHAT_DIAGNOSIS_PROVIDER_CONTACT_TAG_PREFIX-<provider.id>"`,
 * matching the rest of the per-id tags in the carousel so a click
 * targeter can resolve the row without `findAllNodes` filtering.
 */
const val CHAT_DIAGNOSIS_PROVIDER_CONTACT_TAG_PREFIX: String = "chat-diagnosis-provider-contact"

/**
 * Fixed width of each carousel tile (168dp). Sized so on the
 * narrowest supported screen (≈360dp viewport − 32dp of outer
 * card padding − 8dp spacing) about 1.5 tiles render at once,
 * giving the visual cue that the carousel is horizontally
 * scrollable without growing the row to three tiles on a typical
 * phone. Wider screens fold naturally — Compose lays out N tiles
 * in the available space without overflow.
 */
private val ProviderCardWidth: androidx.compose.ui.unit.Dp = 168.dp

/**
 * Theme tokens. The emerald-tinted surface for each provider tile
 * is built on the same `secondaryContainer` / `onSecondaryContainer`
 * slots used in the previous iteration; pinned by
 * `DiagnosisSummaryCardTokensTest` so a revert to Material's
 * default `surfaceVariant` (lilac) cannot ship undetected.
 */
internal val diagnosisSummaryContainerColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.secondaryContainer

internal val diagnosisSummaryContentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSecondaryContainer

/**
 * Inline section rendered just below the chat once the AI
 * concludes the diagnosis. Shaped as part of the conversation
 * rather than its own screen:
 *
 *  ```
 *  Rubro detectado: Plomería
 *  Prestadores recomendados
 *  [card] [card] [next partial]
 *  ```
 *
 * The container used to be a single big `Card` with emerald
 * tint; that visual weight made the section feel like a separate
 * surface. It's now plain text headers flowing straight into a
 * `LazyRow` of compact tiles, so the section reads as another
 * paragraph of the AI's reply.
 *
 * [onContactClick] is the hook for the upcoming US-39 ("Enviar
 * solicitud a prestador"). For now it has a no-op default so
 * callers don't have to thread it before navigation lands.
 */
@Composable
fun DiagnosisSummaryCard(
    categoryName: String?,
    providers: List<Provider>,
    modifier: Modifier = Modifier,
    onContactClick: (Provider) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CHAT_DIAGNOSIS_SUMMARY_TAG),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (categoryName != null) {
            Text(
                text = stringResource(
                    R.string.chat_diagnosis_category_format,
                    categoryName,
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag(CHAT_DIAGNOSIS_CATEGORY_TAG),
            )
        }
        Text(
            text = stringResource(R.string.chat_diagnosis_providers_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (providers.isNotEmpty()) {
            ProvidersCarousel(
                providers = providers,
                onContactClick = onContactClick,
            )
        }
    }
}

/**
 * Horizontal `LazyRow` of provider tiles. Mirrors the
 * `CategoryChipRow` pattern used on WelcomeScreen
 * (`Arrangement.spacedBy(8.dp)`, no outer height constraint) plus
 * a small horizontal `contentPadding` so the first tile isn't
 * flush with the chat's edge and the next tile peeks to advertise
 * the scroll affordance.
 *
 * `key = { it.id }` keeps the carousel scroll state stable across
 * recompositions when the AI appends a new provider to the list.
 */
@Composable
private fun ProvidersCarousel(
    providers: List<Provider>,
    onContactClick: (Provider) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CHAT_DIAGNOSIS_PROVIDERS_CAROUSEL_TAG),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = providers, key = { it.id }) { provider ->
            RecommendedProviderCard(
                provider = provider,
                onContactClick = onContactClick,
            )
        }
    }
}

/**
 * Compact carousel tile. Vertical layout — avatar on top, name
 * under it, the category line lives in the section header above
 * the carousel so it isn't echoed here — and a single "Contactar"
 * button to drive the contact-this-provider flow.
 *
 * Sized to fit ~1.5 tiles on a 360dp-class viewport; the height
 * adapts to the inner content (avatar + name + button).
 */
@Composable
private fun RecommendedProviderCard(
    provider: Provider,
    onContactClick: (Provider) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(ProviderCardWidth)
            .testTag("$CHAT_DIAGNOSIS_PROVIDER_ROW_TAG-${provider.id}"),
        colors = CardDefaults.cardColors(
            containerColor = diagnosisSummaryContainerColor,
            contentColor = diagnosisSummaryContentColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProviderAvatar(
                name = provider.name,
                profilePhotoUrl = provider.profilePhotoUrl,
                testTag = "$CHAT_DIAGNOSIS_PROVIDER_ROW_TAG-${provider.id}-avatar",
                size = 40.dp,
            )
            Text(
                text = "${provider.name} ${provider.surname}".trim(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(
                    "$CHAT_DIAGNOSIS_PROVIDER_NAME_TAG-${provider.id}",
                ),
            )
            Spacer(Modifier.height(2.dp))
            Button(
                onClick = { onContactClick(provider) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(
                        "$CHAT_DIAGNOSIS_PROVIDER_CONTACT_TAG_PREFIX-${provider.id}",
                    ),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 6.dp,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.contact_provider_button_contactar),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}
