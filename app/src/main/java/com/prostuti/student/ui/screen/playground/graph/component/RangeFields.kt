package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography

/** Two small fields "from … to …". Values may be expressions such as -2π. */
@Composable
internal fun RangeFields(
    title: String,
    min: String,
    max: String,
    onMin: (String) -> Unit,
    onMax: (String) -> Unit,
    tag: String,
    minLabel: String = stringResource(R.string.graph_range_min),
    maxLabel: String = stringResource(R.string.graph_range_max),
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = ProstutiTypography.bnLabelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = min, onValueChange = onMin, label = { Text(minLabel) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.weight(1f).testTag("${tag}_min"),
            )
            OutlinedTextField(
                value = max, onValueChange = onMax, label = { Text(maxLabel) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.weight(1f).testTag("${tag}_max"),
            )
        }
    }
}
