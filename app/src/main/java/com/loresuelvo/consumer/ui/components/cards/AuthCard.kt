package com.loresuelvo.consumer.ui.components.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AuthCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    content: @Composable () -> Unit
) {

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {

        Column(
            modifier = Modifier.padding(28.dp)
        ) {

            content()
        }
    }
}