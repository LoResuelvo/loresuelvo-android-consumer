package com.loresuelvo.consumer.ui.components.branding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

@Composable
fun AppLogo() {

    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = stringResource(id = R.string.app_logo_content_description),
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(96.dp)
    )
}
