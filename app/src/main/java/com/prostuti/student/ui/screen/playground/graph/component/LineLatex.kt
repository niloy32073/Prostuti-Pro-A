package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.font.MathFont
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.prostuti.student.ui.theme.MonoFontFamily

/**
 * One line's formula, left-aligned on a single row. A formula wider than the card scrolls
 * sideways instead of being cut to "…", so long equations stay readable.
 */
@Composable
fun LineLatex(latex: String, color: Color, fontSize: TextUnit, modifier: Modifier = Modifier) {
    val config = remember(fontSize, color) {
        LatexConfig(fontSize = fontSize, color = color, darkColor = color, mathFont = MathFont.KaTeXTTF)
    }
    val measurer = rememberLatexMeasurer(config)
    val dims = remember(latex, config, measurer) { measurer.measure(latex, config) }
    val density = LocalDensity.current
    Box(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (dims == null) {
            Text(latex, color = color, fontSize = fontSize, fontFamily = MonoFontFamily)
        } else {
            Latex(
                latex = latex,
                config = config,
                modifier = Modifier.size(with(density) { dims.widthPx.toDp() }, with(density) { dims.heightPx.toDp() }),
            )
        }
    }
}
