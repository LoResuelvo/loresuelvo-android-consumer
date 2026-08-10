package com.loresuelvo.consumer.ui.screens.professional

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Stateless screen for the single-category provider list. The shape
 * is dictated entirely by [ProfessionalsUiState]; no business logic
 * lives here.
 *
 *   Loading -> circular progress
 *   Ready   -> LazyColumn of [ProviderCard] (each row exposes a
 *             "Contactar" button that opens the contact form
 *             ModalBottomSheet)
 *   Empty   -> explanatory copy + back action
 *   Error   -> retry copy + [onRetryClick] callback
 *
 * The contact form is wired through the modal sheet rendered at
 * the bottom of the screen whenever [contactFormState] is
 * [ContactProviderUiState.Open]. The host (`LoResuelvoNav.ProfessionalsRoute`)
 * owns the [ContactProviderViewModel] and forwards the navigation
 * event when the form submits successfully.
 *
 * Navigation args (categoryId, categoryName) are owned by
 * `LoResuelvoNav.ProfessionalsRoute`; this composable receives only
 * the [state] slice + retry callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalsScreen(
    state: ProfessionalsUiState,
    contactFormState: ContactProviderUiState,
    onRetryClick: () -> Unit,
    onContactarClick: (Provider) -> Unit,
    onContactTitleChange: (String) -> Unit,
    onContactDescriptionChange: (String) -> Unit,
    onContactSubmit: () -> Unit,
    onContactCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Edge-to-edge: respect the status-bar inset (the OS no
            // longer pre-reserves it since
            // `MainActivity.setDecorFitsSystemWindows(false)`).
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = state.categoryName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(16.dp))

        when (state) {
            is ProfessionalsUiState.Loading -> LoadingView()
            is ProfessionalsUiState.Ready ->
                ReadyList(
                    providers = state.providers,
                    onContactarClick = onContactarClick,
                )
            is ProfessionalsUiState.Empty -> EmptyView(state.categoryName)
            is ProfessionalsUiState.Error -> ErrorView(onRetryClick)
        }
    }

    // Contact-provider bottom sheet. Renders only when the VM
    // exposes an Open state (the modal is dismissed by transitioning
    // back to Closed). The sheet's own onDismissRequest calls
    // onContactCancel so swiping the sheet down also clears the
    // VM state.
    val openState = contactFormState as? ContactProviderUiState.Open
    if (openState != null) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
        ModalBottomSheet(
            onDismissRequest = onContactCancel,
            sheetState = sheetState,
        ) {
            ContactProviderBottomSheet(
                provider = openState.provider,
                title = openState.title,
                description = openState.description,
                canSubmit = openState.canSubmit,
                isSubmitting = openState.isSubmitting,
                error = openState.error,
                onTitleChange = onContactTitleChange,
                onDescriptionChange = onContactDescriptionChange,
                onSubmit = onContactSubmit,
                onCancel = onContactCancel,
            )
        }
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ReadyList(
    providers: List<Provider>,
    onContactarClick: (Provider) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items = providers, key = { it.id }) { provider ->
            ProviderCard(
                provider = provider,
                onContactarClick = { onContactarClick(provider) },
            )
        }
    }
}

@Composable
private fun EmptyView(categoryName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.professionals_empty_title, categoryName),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.professionals_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun ErrorView(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.professionals_error_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.professionals_error_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = SubtitleGray,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onRetryClick) {
            Text(text = stringResource(R.string.professionals_retry))
        }
    }
}

@Composable
private fun ProviderCard(
    provider: Provider,
    onContactarClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderAvatar(
                name = provider.name,
                profilePhotoUrl = provider.profilePhotoUrl,
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${provider.name} ${provider.surname}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = provider.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SubtitleGray,
                )
            }
            TextButton(
                onClick = onContactarClick,
                modifier = Modifier.testTag(CONTACT_PROVIDER_CARD_BUTTON_TAG),
            ) {
                Text(
                    text = stringResource(R.string.contact_provider_button_contactar),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Compose testTag for the "Contactar" button on each provider
 * card. Pinning the locator (rather than the literal Spanish
 * label) keeps the existing acceptance tests in
 * `src/androidTest/.../acceptance/professional/` localisation-
 * independent.
 */
const val CONTACT_PROVIDER_CARD_BUTTON_TAG: String = "provider-card-contactar"

/**
 * Circular avatar for a service provider.
 *
 * Renders in one of two modes:
 *
 *  - **Photo**: when [profilePhotoUrl] is non-blank, [coil3]'s
 *    [coil3.compose.AsyncImage] loads the photo. While the request
 *    is in flight (or on error: 404, DNS, timeout, …) the photo
 *    paints nothing and the initial-letter fallback underneath
 *    stays visible. When the photo resolves, it paints over the
 *    fallback and fills the circle (with [ContentScale.Crop]).
 *  - **Fallback only**: when [profilePhotoUrl] is null/blank the
 *    composable skips Coil entirely and renders the initial on the
 *    brand-coloured circle — no doomed network round-trip is fired.
 *
 * The initial is the LAST child in the [Box], so the Box's own
 * [Alignment.Center] guarantees it is always centred on the circle
 * regardless of the photo state. The photo paints **above** the
 * fallback (later children win the draw order in a Compose [Box]),
 * so a successful load cleanly covers the initial.
 *
 * Decoupled from the [Provider] domain type: the screen passes the
 * two primitives it needs (`name`, `profilePhotoUrl`) so this stays
 * trivially unit-testable without Hilt or a real [Provider].
 *
 * `testTag = PROVIDER_AVATAR_TAG` is exposed so Compose tests can
 * locate the avatar regardless of the rendered state.
 */
@Composable
fun ProviderAvatar(
    name: String,
    profilePhotoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    testTag: String = PROVIDER_AVATAR_TAG,
) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    val description = stringResource(
        R.string.provider_photo_content_description,
        name,
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        if (!profilePhotoUrl.isNullOrBlank()) {
            // Painted FIRST so the initial painted LAST covers it
            // when Coil returns nothing (loading / error). When the
            // photo resolves, it covers the initial underneath.
            coil3.compose.AsyncImage(
                model = profilePhotoUrl,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .testTag(PROVIDER_AVATAR_IMAGE_TAG),
            )
        }
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Compose testTag for the [ProviderAvatar] slot (the outer
 * circle). Always present when the avatar is rendered, regardless
 * of whether the photo loaded.
 */
const val PROVIDER_AVATAR_TAG: String = "provider-avatar"

/**
 * Compose testTag for the inner image painted by Coil when a photo
 * URL is provided. Asserting this node exists proves the avatar
 * attempted to load a remote photo (rather than going straight to
 * the initial-letter fallback).
 */
const val PROVIDER_AVATAR_IMAGE_TAG: String = "provider-avatar-image"

@Preview(showBackground = true, name = "Professionals · Ready")
@Composable
private fun ProfessionalsReadyPreview() {
    LoresuelvoTheme {
        ProfessionalsScreen(
            state = ProfessionalsUiState.Ready(
                categoryName = "Electricidad",
                providers = listOf(
                    Provider(
                        id = 92,
                        name = "Agustina",
                        surname = "Molina",
                        categoryId = 2,
                        categoryName = "Electricidad",
                        // First row carries a photo URL so the preview
                        // exercises the Coil path; the second row
                        // exercises the initial-letter fallback.
                        profilePhotoUrl = "https://example.com/p.webp",
                    ),
                    Provider(
                        id = 32,
                        name = "Agustina",
                        surname = "Ruiz",
                        categoryId = 2,
                        categoryName = "Electricidad",
                        profilePhotoUrl = null,
                    ),
                ),
            ),
            contactFormState = ContactProviderUiState.Closed,
            onRetryClick = {},
            onContactarClick = {},
            onContactTitleChange = {},
            onContactDescriptionChange = {},
            onContactSubmit = {},
            onContactCancel = {},
        )
    }
}

@Preview(showBackground = true, name = "ProviderAvatar · with photo URL")
@Composable
private fun ProviderAvatarWithPhotoPreview() {
    LoresuelvoTheme {
        Row {
            ProviderAvatar(
                name = "Agustina",
                profilePhotoUrl = "https://example.com/p.webp",
            )
        }
    }
}

@Preview(showBackground = true, name = "ProviderAvatar · fallback")
@Composable
private fun ProviderAvatarFallbackPreview() {
    LoresuelvoTheme {
        Row {
            ProviderAvatar(
                name = "Agustina",
                profilePhotoUrl = null,
            )
        }
    }
}

@Preview(showBackground = true, name = "Professionals · Empty")
@Composable
private fun ProfessionalsEmptyPreview() {
    LoresuelvoTheme {
        ProfessionalsScreen(
            state = ProfessionalsUiState.Empty(categoryName = "Gas"),
            contactFormState = ContactProviderUiState.Closed,
            onRetryClick = {},
            onContactarClick = {},
            onContactTitleChange = {},
            onContactDescriptionChange = {},
            onContactSubmit = {},
            onContactCancel = {},
        )
    }
}

@Preview(showBackground = true, name = "Professionals · Error")
@Composable
private fun ProfessionalsErrorPreview() {
    LoresuelvoTheme {
        ProfessionalsScreen(
            state = ProfessionalsUiState.Error(categoryName = "Electricidad"),
            contactFormState = ContactProviderUiState.Closed,
            onRetryClick = {},
            onContactarClick = {},
            onContactTitleChange = {},
            onContactDescriptionChange = {},
            onContactSubmit = {},
            onContactCancel = {},
        )
    }
}

@Preview(showBackground = true, name = "Professionals · Loading")
@Composable
private fun ProfessionalsLoadingPreview() {
    LoresuelvoTheme {
        ProfessionalsScreen(
            state = ProfessionalsUiState.Loading(categoryName = "Electricidad"),
            contactFormState = ContactProviderUiState.Closed,
            onRetryClick = {},
            onContactarClick = {},
            onContactTitleChange = {},
            onContactDescriptionChange = {},
            onContactSubmit = {},
            onContactCancel = {},
        )
    }
}
