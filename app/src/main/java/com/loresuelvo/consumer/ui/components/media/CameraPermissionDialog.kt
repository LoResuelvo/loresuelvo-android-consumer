package com.loresuelvo.consumer.ui.components.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.loresuelvo.consumer.R

@Composable
fun CameraPermissionDialog(
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.camera_permission_title)) },
        text = { Text(stringResource(R.string.camera_permission_message)) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.camera_permission_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.camera_permission_settings))
            }
        },
    )
}

fun openCameraPermissionSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ),
    )
}
