package com.loresuelvo.consumer.ui.components.branding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loresuelvo.consumer.ui.theme.AvatarBlue
import com.loresuelvo.consumer.ui.theme.AvatarDarkBlue
import com.loresuelvo.consumer.ui.theme.AvatarGreen
import com.loresuelvo.consumer.ui.theme.TextWhite

@Composable
fun VerifiedProfessionalsStrip() {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        MiniAvatarCluster()

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = "MÁS DE 1.000 PROFESIONALES VERIFICADOS",
            color = TextWhite.copy(alpha = 0.82f),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun MiniAvatarCluster() {

    Row {
        MiniAvatar(
            color = AvatarBlue,
            label = "A"
        )

        MiniAvatar(
            color = AvatarGreen,
            label = "B"
        )

        MiniAvatar(
            color = AvatarDarkBlue,
            label = "✓"
        )
    }
}

@Composable
private fun MiniAvatar(
    color: Color,
    label: String
) {

    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(28.dp)
            .background(
                color = color,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = label,
            color = TextWhite,
            style = MaterialTheme.typography.labelSmall
        )
    }
}