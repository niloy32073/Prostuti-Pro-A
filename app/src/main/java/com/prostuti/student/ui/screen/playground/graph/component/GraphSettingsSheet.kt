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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.domain.graph.plot.PlotViewport
import com.prostuti.student.ui.screen.playground.graph.GraphSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphSettingsSheet(
    settings: GraphSettings,
    viewport: PlotViewport?,
    onSettings: (GraphSettings) -> Unit,
    onApplyRanges: (xMin: String, xMax: String, yMin: String, yMax: String) -> Unit,
    onResetView: () -> Unit,
    onSquareAxes: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("graph_settings_sheet")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.graph_settings), style = ProstutiTypography.bnTitleMedium)
            Text(stringResource(R.string.graph_angle_unit), style = ProstutiTypography.bnLabelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val units = listOf(AngleUnit.RAD to R.string.graph_radian, AngleUnit.DEG to R.string.graph_degree)
                units.forEachIndexed { i, (unit, label) ->
                    SegmentedButton(
                        selected = settings.angleUnit == unit,
                        onClick = { onSettings(settings.copy(angleUnit = unit)) },
                        shape = SegmentedButtonDefaults.itemShape(i, units.size),
                        modifier = Modifier.testTag("angle_${unit.name}"),
                    ) { Text(stringResource(label)) }
                }
            }
            SwitchRow(stringResource(R.string.graph_show_grid), settings.showGrid, "toggle_grid") { onSettings(settings.copy(showGrid = it)) }
            SwitchRow(stringResource(R.string.graph_show_axes), settings.showAxes, "toggle_axes") { onSettings(settings.copy(showAxes = it)) }
            SwitchRow(stringResource(R.string.graph_show_numbers), settings.showNumbers, "toggle_numbers") { onSettings(settings.copy(showNumbers = it)) }
            SwitchRow(stringResource(R.string.graph_pi_axis), settings.piXAxis, "toggle_pi") { onSettings(settings.copy(piXAxis = it)) }

            var xMin by rememberSaveable { mutableStateOf(viewport?.xMin?.let { GraphNumberFormat.format(it, 4) }.orEmpty()) }
            var xMax by rememberSaveable { mutableStateOf(viewport?.xMax?.let { GraphNumberFormat.format(it, 4) }.orEmpty()) }
            var yMin by rememberSaveable { mutableStateOf(viewport?.yMin?.let { GraphNumberFormat.format(it, 4) }.orEmpty()) }
            var yMax by rememberSaveable { mutableStateOf(viewport?.yMax?.let { GraphNumberFormat.format(it, 4) }.orEmpty()) }
            RangeFields(stringResource(R.string.graph_x_range), xMin, xMax, { xMin = it }, { xMax = it }, "x")
            RangeFields(stringResource(R.string.graph_y_range), yMin, yMax, { yMin = it }, { yMax = it }, "y")
            Button(onClick = { onApplyRanges(xMin, xMax, yMin, yMax) }, modifier = Modifier.fillMaxWidth().testTag("apply_ranges")) {
                Text(stringResource(R.string.graph_apply))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onResetView, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.graph_reset_view)) }
                OutlinedButton(onClick = onSquareAxes, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.graph_square_axes)) }
            }
        }
    }
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = ProstutiTypography.bnBodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}
