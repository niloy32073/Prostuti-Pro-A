package com.prostuti.student.ui.screen.examengine.calculator

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry. [koinViewModel] resolves against the exam's navigation entry, so the calculator
 * keeps its own ViewModel for the whole attempt (tab switches and panel hide/show included)
 * without touching the exam ViewModel.
 */
@Composable
fun CalculatorPanel(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalculatorContent(state = state, onKey = viewModel::onKey, modifier = modifier)
}

@Composable
fun CalculatorContent(
    state: CalculatorUiState,
    onKey: (CalcKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lcd = remember(state) { CalculatorLcdMapper.map(state) }
    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Palette.BodyTop, Palette.BodyBottom)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BrandStrip()
        Lcd(lcd)
        Spacer(Modifier.height(4.dp))
        TopCluster(onKey)
        FunctionRow(listOf(CalcKey.FRACTION, CalcKey.SQRT, CalcKey.SQUARE, CalcKey.POWER, CalcKey.LOG, CalcKey.LN), onKey)
        FunctionRow(listOf(CalcKey.NEGATIVE, CalcKey.DMS, CalcKey.HYP, CalcKey.SIN, CalcKey.COS, CalcKey.TAN), onKey)
        FunctionRow(listOf(CalcKey.RCL, CalcKey.ENG, CalcKey.OPEN, CalcKey.CLOSE, CalcKey.S_D, CalcKey.M_PLUS), onKey)
        Spacer(Modifier.height(6.dp))
        NumberRow(listOf(CalcKey.D7, CalcKey.D8, CalcKey.D9, CalcKey.DEL, CalcKey.AC), onKey)
        NumberRow(listOf(CalcKey.D4, CalcKey.D5, CalcKey.D6, CalcKey.MULTIPLY, CalcKey.DIVIDE), onKey)
        NumberRow(listOf(CalcKey.D1, CalcKey.D2, CalcKey.D3, CalcKey.PLUS, CalcKey.MINUS), onKey)
        NumberRow(listOf(CalcKey.D0, CalcKey.DOT, CalcKey.EXP10, CalcKey.ANS, CalcKey.EQUALS), onKey)
    }
}

// ── Case details ─────────────────────────────────────────────────────────────

@Composable
private fun BrandStrip() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("PROSTUTI", color = Palette.Brand, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("SCIENTIFIC · EQN 4", color = Palette.BrandDim, fontSize = 8.sp, letterSpacing = 0.5.sp)
        }
        // Solar panel
        Row(
            modifier = Modifier
                .width(64.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Palette.Solar)
                .padding(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            repeat(4) { Box(Modifier.weight(1f).fillMaxSize().background(Palette.SolarCell)) }
        }
    }
}

// ── LCD ──────────────────────────────────────────────────────────────────────

@Composable
private fun Lcd(content: LcdContent) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Palette.LcdFrame)
            .padding(5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 118.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Palette.Lcd)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag(TAG_LCD),
        ) {
            Indicators(content)
            when (val body = content.body) {
                is LcdContent.Body.Calculation -> CalculationBody(body)
                is LcdContent.Body.Lines -> body.lines.forEach { line ->
                    Text(line, style = lcdStyle(14.sp), maxLines = 1, softWrap = false)
                }
                is LcdContent.Body.Coefficients -> CoefficientBody(body)
            }
        }
    }
}

@Composable
private fun Indicators(content: LcdContent) {
    Row(modifier = Modifier.fillMaxWidth().height(13.dp), verticalAlignment = Alignment.CenterVertically) {
        content.indicators.forEach { Text(it, style = lcdStyle(9.sp, bold = true), modifier = Modifier.padding(end = 6.dp)) }
        Spacer(Modifier.weight(1f))
        if (content.showUp) Text("▲", style = lcdStyle(9.sp))
        if (content.showDown) Text("▼", style = lcdStyle(9.sp))
    }
}

@Composable
private fun CalculationBody(body: LcdContent.Body.Calculation) {
    LcdLine(text = body.input, cursor = body.cursor, fontSize = 17.sp)
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        body.resultLabel?.let { Text(it, style = lcdStyle(17.sp)) }
        Spacer(Modifier.weight(1f))
        body.result?.let { LcdValue(it) }
    }
    body.footnote?.let { Text(it, style = lcdStyle(12.sp), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) }
}

/** One monospace LCD line that scrolls sideways to keep the cursor in view. */
@Composable
private fun LcdLine(text: String, cursor: Int?, fontSize: TextUnit) {
    val style = lcdStyle(fontSize)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val charWidth = remember(style) { measurer.measure("0", style).size.width }
    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(keyframes { durationMillis = 1000; 1f at 0; 1f at 499; 0f at 500 }, RepeatMode.Restart),
        label = "cursorBlink",
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Sizing only: how many monospace cells fit on this line.
        val columns = with(density) { (maxWidth.toPx() / charWidth.coerceAtLeast(1)).toInt() }.coerceAtLeast(4)
        val (window, windowCursor) = lcdWindow(text, cursor, columns)
        val annotated = buildAnnotatedString {
            if (windowCursor == null) {
                append(window)
            } else {
                append(window.substring(0, windowCursor))
                withStyle(SpanStyle(color = Palette.LcdInk.copy(alpha = blink))) { append("▏") }
                append(window.substring(windowCursor))
            }
        }
        Text(annotated, style = style, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun LcdValue(value: LcdContent.Value) {
    when (value) {
        is LcdContent.Value.Fraction -> Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.negative) Text("-", style = lcdStyle(20.sp), modifier = Modifier.padding(end = 2.dp))
            Column(Modifier.width(IntrinsicSize.Max), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value.numerator.toString(), style = lcdStyle(17.sp))
                Box(Modifier.fillMaxWidth().height(1.5.dp).background(Palette.LcdInk))
                Text(value.denominator.toString(), style = lcdStyle(17.sp))
            }
        }
        is LcdContent.Value.Number -> AutoSizeValue(buildAnnotatedString {
            append(value.mantissa)
            value.exponent?.let { exponent ->
                withStyle(SpanStyle(fontSize = 0.7.em)) { append("×10") }
                withStyle(SpanStyle(fontSize = 0.6.em, baselineShift = BaselineShift.Superscript)) { append(exponent.toString()) }
            }
        })
        is LcdContent.Value.Text -> AutoSizeValue(AnnotatedString(value.text))
    }
}

@Composable
private fun AutoSizeValue(text: AnnotatedString) {
    BasicText(
        text = text,
        style = lcdStyle(24.sp).copy(textAlign = TextAlign.End),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 24.sp),
        modifier = Modifier.wrapContentWidth(Alignment.End),
    )
}

@Composable
private fun CoefficientBody(body: LcdContent.Body.Coefficients) {
    val cellStyle = lcdStyle(10.sp)
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(12.dp))
        body.headers.forEach { Text(it, style = cellStyle, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
    }
    body.cells.forEachIndexed { r, row ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(body.rowLabels.getOrElse(r) { "" }, style = cellStyle, modifier = Modifier.width(12.dp))
            row.forEachIndexed { c, cell ->
                val selected = r == body.selectedRow && c == body.selectedColumn
                Text(
                    text = cell,
                    style = cellStyle.copy(color = if (selected) Palette.Lcd else Palette.LcdInk),
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.dp, vertical = 1.dp)
                        .background(if (selected) Palette.LcdInk else Color.Transparent),
                )
            }
        }
    }
    Spacer(Modifier.height(2.dp))
    if (body.entryCursor == null) {
        Text(body.entry, style = lcdStyle(15.sp), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, maxLines = 1)
    } else {
        LcdLine(text = body.entry, cursor = body.entryCursor, fontSize = 15.sp)
    }
}

/**
 * Picks the visible slice of an LCD line ([columns] characters, one reserved for the cursor).
 * Returns the slice and the cursor position inside it.
 */
internal fun lcdWindow(text: String, cursor: Int?, columns: Int): Pair<String, Int?> {
    val room = if (cursor == null) columns else columns - 1
    if (text.length <= room) return text to cursor
    if (cursor == null) return text.take(room) to null
    val start = (cursor - room).coerceAtLeast(0).coerceAtMost(text.length - room)
    val slice = text.substring(start, start + room)
    return slice to (cursor - start).coerceIn(0, slice.length)
}

// ── Keys ─────────────────────────────────────────────────────────────────────

@Composable
private fun TopCluster(onKey: (CalcKey) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CalcButton(CalcKey.SHIFT, KeyStyle.Shift, onKey)
                CalcButton(CalcKey.ALPHA, KeyStyle.Alpha, onKey)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CalcButton(CalcKey.CALC, KeyStyle.Function, onKey)
                CalcButton(CalcKey.INTEGRAL, KeyStyle.Function, onKey)
            }
        }
        ReplayPad(onKey, Modifier.weight(2f))
        Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CalcButton(CalcKey.MODE, KeyStyle.Function, onKey)
                CalcButton(CalcKey.ON, KeyStyle.Function, onKey)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CalcButton(CalcKey.INVERSE, KeyStyle.Function, onKey)
                CalcButton(CalcKey.LOG_AB, KeyStyle.Function, onKey)
            }
        }
    }
}

@Composable
private fun ReplayPad(onKey: (CalcKey) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = LABEL_HEIGHT)
            .height(FUNCTION_KEY_HEIGHT * 2 + LABEL_HEIGHT + 4.dp)
            .clip(RoundedCornerShape(50))
            .background(Palette.Pad)
            .border(1.dp, Palette.PadRim, RoundedCornerShape(50)),
    ) {
        Text("REPLAY", color = Palette.PadText, fontSize = 7.sp, modifier = Modifier.align(Alignment.Center))
        PadArrow(CalcKey.UP, Alignment.TopCenter, onKey)
        PadArrow(CalcKey.DOWN, Alignment.BottomCenter, onKey)
        PadArrow(CalcKey.LEFT, Alignment.CenterStart, onKey)
        PadArrow(CalcKey.RIGHT, Alignment.CenterEnd, onKey)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.PadArrow(key: CalcKey, alignment: Alignment, onKey: (CalcKey) -> Unit) {
    Box(
        modifier = Modifier
            .align(alignment)
            .size(width = 34.dp, height = 26.dp)
            .clip(RoundedCornerShape(50))
            .clickable { onKey(key) }
            .testTag(keyTag(key)),
        contentAlignment = Alignment.Center,
    ) {
        Text(key.label, color = Palette.PadText, fontSize = 11.sp)
    }
}

@Composable
private fun FunctionRow(keys: List<CalcKey>, onKey: (CalcKey) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        keys.forEach { CalcButton(it, KeyStyle.Function, onKey) }
    }
}

@Composable
private fun NumberRow(keys: List<CalcKey>, onKey: (CalcKey) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.forEach { key ->
            val style = if (key == CalcKey.DEL || key == CalcKey.AC) KeyStyle.Control else KeyStyle.Number
            CalcButton(key, style, onKey)
        }
    }
}

private enum class KeyStyle(val height: Dp, val face: Color, val text: Color, val fontSize: TextUnit) {
    Shift(FUNCTION_KEY_HEIGHT, Color(0xFF3A3E45), Color(0xFFF2B544), 9.sp),
    Alpha(FUNCTION_KEY_HEIGHT, Color(0xFF3A3E45), Color(0xFFF06A80), 9.sp),
    Function(FUNCTION_KEY_HEIGHT, Color(0xFF474C54), Color(0xFFF4F4F4), 11.sp),
    Number(NUMBER_KEY_HEIGHT, Color(0xFFE9EAEC), Color(0xFF1C1E21), 17.sp),
    Control(NUMBER_KEY_HEIGHT, Color(0xFF2F76D2), Color.White, 14.sp),
}

/** A key with its SHIFT (amber, left) and ALPHA (pink, right) labels printed above it. */
@Composable
private fun RowScope.CalcButton(key: CalcKey, style: KeyStyle, onKey: (CalcKey) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(interactionSource = interaction, indication = null) { onKey(key) }
            .testTag(keyTag(key)),
    ) {
        Row(Modifier.fillMaxWidth().height(LABEL_HEIGHT), verticalAlignment = Alignment.Bottom) {
            Text(key.shiftLabel, color = Palette.ShiftLabel, fontSize = 8.sp, lineHeight = 10.sp, maxLines = 1, softWrap = false)
            Spacer(Modifier.weight(1f))
            Text(key.alphaLabel, color = Palette.AlphaLabel, fontSize = 8.sp, lineHeight = 10.sp, maxLines = 1, softWrap = false)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(style.height)
                .clip(RoundedCornerShape(if (style.height == NUMBER_KEY_HEIGHT) 7.dp else 5.dp))
                .background(Brush.verticalGradient(listOf(style.face, style.face.copy(alpha = 0.85f))))
                .indication(interaction, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = key.label,
                color = style.text,
                fontSize = style.fontSize,
                fontWeight = if (style == KeyStyle.Number) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private fun lcdStyle(size: TextUnit, bold: Boolean = false) = TextStyle(
    color = Palette.LcdInk,
    fontSize = size,
    fontFamily = FontFamily.Monospace,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
)

internal fun keyTag(key: CalcKey) = "calc_key_${key.name}"
internal const val TAG_LCD = "calc_lcd"

private val LABEL_HEIGHT = 13.dp
private val FUNCTION_KEY_HEIGHT = 26.dp
private val NUMBER_KEY_HEIGHT = 36.dp

/** The calculator is a physical object: its colours stay the same in light and dark themes. */
private object Palette {
    val BodyTop = Color(0xFF30343A)
    val BodyBottom = Color(0xFF1E2125)
    val Brand = Color(0xFFD9DCE1)
    val BrandDim = Color(0xFF8C9199)
    val Solar = Color(0xFF3B2A22)
    val SolarCell = Color(0xFF5A3E30)
    val LcdFrame = Color(0xFF121417)
    val Lcd = Color(0xFFC6D3BC)
    val LcdInk = Color(0xFF1B2214)
    val Pad = Color(0xFF3A3E45)
    val PadRim = Color(0xFF555A62)
    val PadText = Color(0xFFC9CDD3)
    val ShiftLabel = Color(0xFFF2B544)
    val AlphaLabel = Color(0xFFF06A80)
}

@Preview(widthDp = 300, heightDp = 640)
@Composable
private fun CalculatorPreview() {
    CalculatorContent(state = CalculatorUiState(), onKey = {})
}
