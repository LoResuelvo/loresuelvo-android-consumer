package com.loresuelvo.consumer.ui.screens.profile.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileAction
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileUiState

@Composable
fun ProfilePhotoPicker(
    state: CompleteProfileUiState,
    onAction: (CompleteProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(state.profilePhoto) {
        state.profilePhoto?.bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.asImageBitmap()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.complete_profile_photo_preview),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(112.dp).testTag("profile-photo-preview"),
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = stringResource(R.string.complete_profile_photo_preview),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(112.dp),
            )
        }

        OutlinedButton(
            onClick = { onAction(CompleteProfileAction.PickPhotoClicked) },
            enabled = !state.loading && !state.photoLoading,
            modifier = Modifier.testTag("profile-photo-picker"),
        ) {
            Text(
                text = stringResource(
                    if (state.profilePhoto == null) {
                        R.string.complete_profile_photo_add
                    } else {
                        R.string.complete_profile_photo_change
                    },
                ),
            )
        }

        if (state.profilePhoto != null) {
            OutlinedButton(
                onClick = { onAction(CompleteProfileAction.RemovePhotoClicked) },
                enabled = !state.loading && !state.photoLoading,
                modifier = Modifier.testTag("profile-photo-remove"),
            ) {
                Text(stringResource(R.string.complete_profile_photo_remove))
            }
        }
    }
}
