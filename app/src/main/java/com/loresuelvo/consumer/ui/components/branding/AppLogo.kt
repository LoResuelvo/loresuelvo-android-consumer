package com.loresuelvo.consumer.ui.components.branding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

@Composable
fun AppLogo(
    size: Dp = 96.dp,
) {
    val density = LocalDensity.current

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
                .graphicsLayer {
                    scaleX = LOGO_SCALE
                    scaleY = LOGO_SCALE
                    translationY = with(density) {
                        size.toPx() * LOGO_VERTICAL_CENTER_CORRECTION
                    }
                },
        )
    }
}

private const val LOGO_SCALE = 2.4f
private const val LOGO_VERTICAL_CENTER_CORRECTION = 0.066f
