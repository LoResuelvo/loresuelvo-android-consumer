package com.loresuelvo.consumer.ui.components.buttons

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.ui.theme.PrimaryBlue

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit
) {

    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {

        Text(
            text = text,
            color = PrimaryBlue
        )
    }
}