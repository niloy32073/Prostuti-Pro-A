package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.domain.graph.GraphErrorKind
import com.prostuti.student.domain.graph.GraphLineKind
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.screen.playground.graph.GraphLineState
import com.prostuti.student.ui.screen.playground.graph.LineStatus
import com.prostuti.student.ui.theme.MonoFontFamily
import kotlinx.coroutines.awaitCancellation

/** One expression-list row: colour/visibility dot, the editor (or typeset math), and its result. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExpressionCard(
    index: Int,
    line: GraphLineState,
    status: LineStatus,
    color: Color,
    focused: Boolean,
    useSystemKeyboard: Boolean,
    playing: Boolean,
    onFocus: () -> Unit,
    onTextChange: (text: String, selectionStart: Int, selectionEnd: Int) -> Unit,
    onToggleVisible: () -> Unit,
    onOptions: () -> Unit,
    onDelete: () -> Unit,
    onSliderChange: (Double) -> Unit,
    onTogglePlay: () -> Unit,
    onAddSlider: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) scheme.surfaceContainerHigh else scheme.surfaceContainerLow)
            .border(if (focused) 1.5.dp else 0.dp, if (focused) scheme.primary else Color.Transparent, shape)
            .testTag("expression_card_$index")
            .padding(start = 6.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
    ) {
        VisibilityDot(color, line.visible, status.drawsSomething, index, onToggleVisible)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
                .heightIn(min = 40.dp),
        ) {
            if (focused) {
                Editor(line, useSystemKeyboard, onTextChange)
                if (status.latex != null && line.text.isNotBlank()) {
                    MathText(
                        text = "$" + status.latex + "$",
                        color = scheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        horizontalPadding = 0.dp,
                        modifier = Modifier.padding(top = 4.dp).testTag("expression_preview"),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .clickable(onClick = onFocus)
                        .testTag("expression_display_$index"),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    when {
                        line.text.isBlank() -> Text(
                            stringResource(R.string.graph_expression_hint),
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = ProstutiTypography.bnBodyMedium,
                        )
                        status.latex != null -> MathText(
                            text = "$" + status.latex + "$",
                            color = scheme.onSurface,
                            fontSize = 18.sp,
                            horizontalPadding = 0.dp,
                        )
                        else -> Text(line.text, fontFamily = MonoFontFamily, color = scheme.onSurface)
                    }
                }
            }
            StatusArea(line, status, playing, onSliderChange, onTogglePlay, onAddSlider)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp).testTag("expression_delete_$index")) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.graph_delete_line), tint = scheme.onSurfaceVariant)
            }
            IconButton(onClick = onOptions, modifier = Modifier.size(40.dp).testTag("expression_options_$index")) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.graph_line_options), tint = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VisibilityDot(color: Color, visible: Boolean, drawable: Boolean, index: Int, onClick: () -> Unit) {
    val description = stringResource(if (visible) R.string.graph_hide_line else R.string.graph_show_line)
    IconButton(
        onClick = onClick,
        enabled = drawable,
        modifier = Modifier.size(40.dp).semantics { contentDescription = description }.testTag("expression_visibility_$index"),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (visible && drawable) color else Color.Transparent)
                .border(2.dp, if (drawable) color else MaterialTheme.colorScheme.outlineVariant, CircleShape),
        ) {
            Text(
                text = "${index + 1}",
                color = if (visible && drawable) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Editor(line: GraphLineState, useSystemKeyboard: Boolean, onTextChange: (String, Int, Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var value by remember(line.id) {
        mutableStateOf(TextFieldValue(line.text, TextRange(line.selectionStart, line.selectionEnd)))
    }
    // Keys from the math keyboard change the line in the ViewModel; mirror them here.
    LaunchedEffect(line.text, line.selectionStart, line.selectionEnd) {
        val wanted = TextRange(line.selectionStart.coerceIn(0, line.text.length), line.selectionEnd.coerceIn(0, line.text.length))
        if (value.text != line.text || value.selection != wanted) value = TextFieldValue(line.text, wanted)
    }
    val focusRequester = remember { FocusRequester() }
    val blockIme by rememberUpdatedState(!useSystemKeyboard)
    // Re-create the field when the keyboard mode flips, so the new input session picks it up.
    androidx.compose.runtime.key(useSystemKeyboard) {
        InterceptPlatformTextInput(
            interceptor = { request, nextHandler ->
                if (blockIme) awaitCancellation() else nextHandler.startInputMethod(request)
            },
        ) {
            BasicTextField(
                value = value,
                onValueChange = {
                    value = it
                    onTextChange(it.text, it.selection.start, it.selection.end)
                },
                textStyle = TextStyle(fontFamily = MonoFontFamily, fontSize = 17.sp, color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .focusRequester(focusRequester)
                    .testTag("expression_editor"),
            )
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
private fun StatusArea(
    line: GraphLineState,
    status: LineStatus,
    playing: Boolean,
    onSliderChange: (Double) -> Unit,
    onTogglePlay: () -> Unit,
    onAddSlider: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val error = status.error
    if (error != null && line.text.isNotBlank()) {
        Text(
            graphErrorText(error),
            color = if (error.kind == GraphErrorKind.NEEDS_3D) scheme.tertiary else scheme.error,
            style = ProstutiTypography.bnBodyMedium,
            modifier = Modifier.padding(top = 4.dp).testTag("expression_error"),
        )
        if (error.kind == GraphErrorKind.UNDEFINED_VARIABLE) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                for (name in error.names) {
                    AssistChip(
                        onClick = { onAddSlider(name) },
                        label = { Text(stringResource(R.string.graph_add_slider, name)) },
                        modifier = Modifier.testTag("add_slider_$name"),
                    )
                }
            }
        }
        return
    }
    when (status.kind) {
        GraphLineKind.PARAMETER -> if (status.isSlider) {
            val range = line.slider
            val v = (status.value ?: range.min).coerceIn(range.min, range.max)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(GraphNumberFormat.format(range.min, 4), style = ProstutiTypography.bnLabelMedium, color = scheme.onSurfaceVariant)
                Slider(
                    value = v.toFloat(),
                    onValueChange = { onSliderChange(it.toDouble()) },
                    valueRange = range.min.toFloat()..range.max.toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp).testTag("parameter_slider"),
                )
                Text(GraphNumberFormat.format(range.max, 4), style = ProstutiTypography.bnLabelMedium, color = scheme.onSurfaceVariant)
                PlayButton(playing, onTogglePlay)
            }
        } else {
            ValueText(status)
        }
        GraphLineKind.VALUE -> ValueText(status)
        GraphLineKind.INTEGRAL -> {
            ValueText(status)
            val note = if (status.integralBetweenCurves) R.string.graph_between_note else R.string.graph_area_note
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(note), style = ProstutiTypography.bnBodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                PlayButton(playing, onTogglePlay)
            }
            if (!status.integralConverged) {
                Text(stringResource(R.string.graph_not_converged), style = ProstutiTypography.bnBodyMedium, color = scheme.error)
            }
            status.riemannSum?.let {
                Text(
                    stringResource(R.string.graph_riemann_value, line.options.riemannCount, GraphNumberFormat.format(it)),
                    style = ProstutiTypography.bnBodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.testTag("riemann_value"),
                )
            }
        }
        else -> Unit
    }
}

@Composable
private fun ValueText(status: LineStatus) {
    val value = status.value ?: return
    val text = if (value.isNaN()) {
        "= " + stringResource(R.string.graph_value_undefined)
    } else {
        val approx = if (status.kind == GraphLineKind.INTEGRAL) "≈ " else "= "
        approx + GraphNumberFormat.format(value) + (status.exact?.let { "  (= $it)" } ?: "")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(2.dp))
        Text(
            text,
            fontFamily = MonoFontFamily,
            style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp).testTag("expression_value"),
        )
    }
}

@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp).testTag("play_button")) {
        Icon(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(if (playing) R.string.graph_pause else R.string.graph_animate),
        )
    }
}
