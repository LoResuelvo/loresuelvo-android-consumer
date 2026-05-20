package com.loresuelvo.consumer.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.loresuelvo.consumer.ui.components.buttons.PrimaryButton
import com.loresuelvo.consumer.ui.components.inputs.PrimaryTextField

@Composable
fun CompleteProfileScreen() {

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Te damos la bienvenida a LoResuelvo"
        )

        PrimaryTextField(
            value = "",
            label = "Nombre",
            placeholder = "Ej. Juan",
            onValueChange = {}
        )

        PrimaryTextField(
            value = "",
            label = "Apellido",
            placeholder = "Ej. Pérez",
            onValueChange = {}
        )

        PrimaryButton(
            text = "Continuar",
            onClick = {}
        )
    }
}