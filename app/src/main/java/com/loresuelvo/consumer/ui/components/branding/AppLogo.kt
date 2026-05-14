package com.loresuelvo.consumer.ui.components.branding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.ui.theme.PrimaryBlue

@Composable
fun AppLogo() {

    Box(
        modifier = Modifier
            .size(72.dp)
            .background(
                color = PrimaryBlue,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {

        Icon(
            imageVector = Icons.Default.Work,
            contentDescription = "LoResuelvo Logo",
            tint = Color.White,
            modifier = Modifier.size(34.dp)
        )
    }
}