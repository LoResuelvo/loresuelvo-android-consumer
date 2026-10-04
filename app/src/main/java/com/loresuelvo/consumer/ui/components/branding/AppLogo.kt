package com.loresuelvo.consumer.ui.components.branding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

@Composable
fun AppLogo(
    size: Dp = 96.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clipToBounds(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_sin_letras),
            contentDescription = stringResource(id = R.string.app_logo_content_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .matchParentSize()
                .scale(LOGO_SCALE)
                .offset(y = LOGO_VERTICAL_OFFSET),
        )
    }
}

private const val LOGO_SCALE = 2.4f
private val LOGO_VERTICAL_OFFSET = 6.dp
