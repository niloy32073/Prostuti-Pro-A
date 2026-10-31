package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.domain.graph.GraphLineKind
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.domain.graph.plot.LineGeometry
import com.prostuti.student.domain.graph.plot.LineOptions
import com.prostuti.student.domain.graph.plot.PlotViewport
import com.prostuti.student.domain.graph.plot.RiemannMode
import com.prostuti.student.ui.screen.playground.graph.GraphColors
import com.prostuti.student.ui.screen.playground.graph.GraphLineState
import com.prostuti.student.ui.screen.playground.graph.LineStatus

/** Per-line settings: colour, calculus extras for its kind, ranges, order and delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineSettingsSheet(
    line: GraphLineState,
    status: LineStatus,
    geometry: LineGeometry?,
    viewport: PlotViewport?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onColor: (Int) -> Unit,
    onOptions: (LineOptions) -> Unit,
    onRangeTexts: (min: String, max: String) -> Unit,
    onSliderRangeTexts: (min: String, max: String, step: String) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val options = line.options
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("line_settings_sheet")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.graph_color), style = ProstutiTypography.bnLabelLarge)
            val palette = GraphColors.palette()
            val colorLabel = stringResource(R.string.graph_color)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                palette.forEachIndexed { i, c ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(3.dp, if (i == line.colorIndex) MaterialTheme.colorScheme.onSurface else c, CircleShape)
                            .semantics { contentDescription = "$colorLabel ${i + 1}" }
                            .clickable { onColor(i) }
                            .testTag("color_$i"),
                    )
                }
            }

            if (status.isExplicit) {
                SwitchRow(stringResource(R.string.graph_show_derivative), options.showDerivative, "toggle_derivative") {
                    onOptions(options.copy(showDerivative = it))
                }
                val center = viewport?.let { (it.xMin + it.xMax) / 2 } ?: 0.0
                SwitchRow(stringResource(R.string.graph_show_tangent), options.tangentAt != null, "toggle_tangent") {
                    onOptions(options.copy(tangentAt = if (it) center else null))
                }
                options.tangentAt?.let { x0 ->
                    val lo = viewport?.xMin ?: (x0 - 10)
                    val hi = viewport?.xMax ?: (x0 + 10)
                    Text(stringResource(R.string.graph_tangent_at, GraphNumberFormat.format(x0, 4)), style = ProstutiTypography.bnBodyMedium)
                    Slider(
                        value = x0.coerceIn(lo, hi).toFloat(),
                        onValueChange = { onOptions(options.copy(tangentAt = it.toDouble())) },
                        valueRange = lo.toFloat()..hi.toFloat(),
                        modifier = Modifier.testTag("tangent_slider"),
                    )
                    geometry?.tangent?.let { t ->
                        Text(
                            stringResource(R.string.graph_tangent_slope, GraphNumberFormat.format(t.slope, 6)),
                            style = ProstutiTypography.bnBodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("tangent_slope"),
                        )
                    }
                }
            }

            if (status.kind == GraphLineKind.INTEGRAL) {
                Text(stringResource(R.string.graph_riemann), style = ProstutiTypography.bnLabelLarge)
                val modes = listOf(
                    RiemannMode.NONE to R.string.graph_riemann_off,
                    RiemannMode.LEFT to R.string.graph_riemann_left,
                    RiemannMode.RIGHT to R.string.graph_riemann_right,
                    RiemannMode.MIDPOINT to R.string.graph_riemann_mid,
                    RiemannMode.TRAPEZOID to R.string.graph_riemann_trap,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(
                            selected = options.riemann == mode,
                            onClick = { onOptions(options.copy(riemann = mode)) },
                            shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                            modifier = Modifier.testTag("riemann_${mode.name}"),
                            label = { Text(stringResource(label), maxLines = 1) },
                        )
                    }
                }
                if (options.riemann != RiemannMode.NONE) {
                    Text(stringResource(R.string.graph_riemann_count, options.riemannCount), style = ProstutiTypography.bnBodyMedium)
                    Slider(
                        value = options.riemannCount.toFloat(),
                        onValueChange = { onOptions(options.copy(riemannCount = it.toInt().coerceIn(1, 100))) },
                        valueRange = 1f..100f,
                        modifier = Modifier.testTag("riemann_slider"),
                    )
                }
            }

            if (status.kind == GraphLineKind.POLAR || status.kind == GraphLineKind.PARAMETRIC) {
                var min by rememberSaveable(line.id) { mutableStateOf(options.rangeMin?.let { GraphNumberFormat.format(it) } ?: "0") }
                var max by rememberSaveable(line.id) { mutableStateOf(options.rangeMax?.let { GraphNumberFormat.format(it) } ?: "2π") }
                RangeFields(stringResource(R.string.graph_range), min, max, { min = it }, { max = it }, "param_range")
                Button(onClick = { onRangeTexts(min, max) }, modifier = Modifier.testTag("apply_param_range")) {
                    Text(stringResource(R.string.graph_apply))
                }
            }

            if (status.isSlider) {
                var min by rememberSaveable(line.id) { mutableStateOf(GraphNumberFormat.format(line.slider.min)) }
                var max by rememberSaveable(line.id) { mutableStateOf(GraphNumberFormat.format(line.slider.max)) }
                var step by rememberSaveable(line.id) { mutableStateOf(GraphNumberFormat.format(line.slider.step)) }
                RangeFields(
                    stringResource(R.string.graph_range), min, max, { min = it }, { max = it }, "slider_range",
                    minLabel = stringResource(R.string.graph_slider_min), maxLabel = stringResource(R.string.graph_slider_max),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = step, onValueChange = { step = it }, singleLine = true,
                        label = { Text(stringResource(R.string.graph_slider_step)) },
                        modifier = Modifier.weight(1f).testTag("slider_step"),
                    )
                    Button(onClick = { onSliderRangeTexts(min, max, step) }, modifier = Modifier.testTag("apply_slider_range")) {
                        Text(stringResource(R.string.graph_apply))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = null)
                    Text(stringResource(R.string.graph_move_up), modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = null)
                    Text(stringResource(R.string.graph_move_down), modifier = Modifier.padding(start = 4.dp))
                }
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().testTag("delete_line")) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.graph_delete_line), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}
