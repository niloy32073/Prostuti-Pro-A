package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.R
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.domain.graph.RegressionModel
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.screen.playground.graph.LineStatus
import com.prostuti.student.ui.screen.playground.graph.TableState
import com.prostuti.student.ui.theme.MonoFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography

/**
 * A data table inside an expression card: x and y columns (each cell may be a number or a
 * constant expression like 2π), rows that can be added or removed, the best-fit family, and the
 * fitted equation with its r and R².
 */
@Composable
fun TableEditor(table: TableState, status: LineStatus, onChange: (TableState) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Header(stringResource(R.string.graph_table_x), Modifier.weight(1f))
            Header(stringResource(R.string.graph_table_y), Modifier.weight(1f))
        }
        val rows = maxOf(table.xs.size, table.ys.size)
        for (i in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Cell(table.xs.getOrElse(i) { "" }, "table_x_$i", Modifier.weight(1f)) { onChange(table.copy(xs = table.xs.set(i, it, rows))) }
                Cell(table.ys.getOrElse(i) { "" }, "table_y_$i", Modifier.weight(1f)) { onChange(table.copy(ys = table.ys.set(i, it, rows))) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onChange(table.copy(xs = table.xs + "", ys = table.ys + "")) }, modifier = Modifier.testTag("table_add_row")) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.graph_table_add_row), modifier = Modifier.padding(start = 4.dp))
            }
            if (rows > 1) {
                TextButton(onClick = { onChange(table.copy(xs = table.xs.dropLast(1), ys = table.ys.dropLast(1))) }, modifier = Modifier.testTag("table_remove_row")) {
                    Icon(Icons.Filled.Remove, contentDescription = null)
                    Text(stringResource(R.string.graph_table_remove_row), modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        Text(stringResource(R.string.graph_fit), style = ProstutiTypography.bnLabelMedium, color = scheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            for ((model, label) in FIT_LABELS) {
                FilterChip(
                    selected = table.model == model,
                    onClick = { onChange(table.copy(model = model)) },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.testTag("fit_${model.name}"),
                )
            }
        }
        if (table.model != RegressionModel.NONE) {
            val latex = status.latex
            if (latex != null) {
                MathText(text = "$$latex$", fontSize = 16.sp, horizontalPadding = 0.dp, color = scheme.onSurface, modifier = Modifier.testTag("fit_equation"))
                val r2 = status.fitRSquared?.let { GraphNumberFormat.format(it, 5) }
                val stats = when {
                    r2 == null -> null
                    status.fitR != null -> stringResource(R.string.graph_fit_stats_r, GraphNumberFormat.format(status.fitR, 5), r2)
                    else -> stringResource(R.string.graph_fit_stats, r2)
                }
                stats?.let { Text(it, fontFamily = MonoFontFamily, style = ProstutiTypography.bnBodyMedium, color = scheme.primary, modifier = Modifier.testTag("fit_stats")) }
            } else if (status.pointCount >= 2) {
                Text(stringResource(R.string.graph_fit_impossible), style = ProstutiTypography.bnBodyMedium, color = scheme.error)
            }
        }
    }
}

private fun List<String>.set(index: Int, value: String, size: Int): List<String> {
    val out = MutableList(maxOf(size, this.size)) { getOrElse(it) { "" } }
    out[index] = value
    return out
}

@Composable
private fun Header(text: String, modifier: Modifier) {
    Text(text, fontFamily = MonoFontFamily, style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.padding(start = 8.dp))
}

@Composable
private fun Cell(value: String, tag: String, modifier: Modifier, onValue: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(40.dp)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            textStyle = TextStyle(fontFamily = MonoFontFamily, fontSize = 15.sp, color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }
}

private val FIT_LABELS = listOf(
    RegressionModel.NONE to R.string.graph_fit_none,
    RegressionModel.LINEAR to R.string.graph_fit_linear,
    RegressionModel.QUADRATIC to R.string.graph_fit_quadratic,
    RegressionModel.CUBIC to R.string.graph_fit_cubic,
    RegressionModel.EXPONENTIAL to R.string.graph_fit_exponential,
    RegressionModel.POWER to R.string.graph_fit_power,
    RegressionModel.LOGARITHMIC to R.string.graph_fit_logarithmic,
)
