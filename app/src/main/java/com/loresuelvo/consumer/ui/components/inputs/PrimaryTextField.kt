package com.loresuelvo.consumer.ui.components.inputs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.ui.theme.DividerGray

/**
 * Single-line outlined text field with a Material 3 floating label.
 *
 * The label renders inline with the field; once the user types it
 * collapses to the top edge of the border, which keeps the field
 * compact and removes the need for a separate `placeholder`. The
 * `testTag` parameter is propagated so instrumented tests can target
 * the input without relying on localized copy.
 */
@Composable
fun PrimaryTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = DividerGray,
            focusedBorderColor = DividerGray,
        ),
    )
}