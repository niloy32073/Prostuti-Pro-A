package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.ui.screen.playground.graph.GraphSettings
import com.prostuti.student.ui.screen.playground.graph.space.SpaceSettings
import com.prostuti.student.ui.theme.ProstutiTypography

/** 3D view settings: box size, what to show, gyroscope, and the shared angle unit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceSettingsSheet(
    settings: SpaceSettings,
    graphSettings: GraphSettings,
    onSettings: (SpaceSettings) -> Unit,
    onGraphSettings: (GraphSettings) -> Unit,
    onRangeText: (String) -> Unit,
    onResetCamera: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("space_settings_sheet")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.graph_space_settings), style = ProstutiTypography.bnTitleMedium)
            Text(stringResource(R.string.graph_angle_unit), style = ProstutiTypography.bnLabelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val units = listOf(AngleUnit.RAD to R.string.graph_radian, AngleUnit.DEG to R.string.graph_degree)
                units.forEachIndexed { i, (unit, label) ->
                    SegmentedButton(
                        selected = graphSettings.angleUnit == unit,
                        onClick = { onGraphSettings(graphSettings.copy(angleUnit = unit)) },
                        shape = SegmentedButtonDefaults.itemShape(i, units.size),
                    ) { Text(stringResource(label)) }
                }
            }
            SwitchRow(stringResource(R.string.graph_space_box), settings.showBox, "toggle_box") { onSettings(settings.copy(showBox = it)) }
            SwitchRow(stringResource(R.string.graph_show_axes), settings.showAxes, "toggle_space_axes") { onSettings(settings.copy(showAxes = it)) }
            SwitchRow(stringResource(R.string.graph_space_grid), settings.showGrid, "toggle_space_grid") { onSettings(settings.copy(showGrid = it)) }
            SwitchRow(stringResource(R.string.graph_show_numbers), settings.showNumbers, "toggle_space_numbers") { onSettings(settings.copy(showNumbers = it)) }
            SwitchRow(stringResource(R.string.graph_gyroscope), settings.gyroscope, "toggle_gyroscope") { onSettings(settings.copy(gyroscope = it)) }

            var range by rememberSaveable { mutableStateOf(GraphNumberFormat.format(settings.range, 4)) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = range, onValueChange = { range = it }, singleLine = true,
                    label = { Text(stringResource(R.string.graph_space_range)) },
                    modifier = Modifier.weight(1f).testTag("space_range"),
                )
                Button(onClick = { onRangeText(range) }, modifier = Modifier.testTag("apply_space_range")) {
                    Text(stringResource(R.string.graph_apply))
                }
            }
            OutlinedButton(onClick = onResetCamera, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.graph_reset_camera)) }
        }
    }
}
