package com.prostuti.student.ui.component

import android.util.LruCache
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.font.MathFont
import com.hrm.latex.renderer.measure.LatexDimensions
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LineBreakingConfig
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Renders a mixed plain-text + LaTeX string.
 *
 *  - `$...$`   → inline math, flows with the surrounding text
 *  - `$$...$$` → display block on its own line, centred
 *
 * Sizing is exact — no width/height multipliers:
 *  - Every formula is measured with the SAME [LatexConfig] it is drawn with,
 *    so the placeholder is exactly as big as the drawing.
 *  - Inline formulas are centred on the math axis, so fractions sit evenly
 *    above and below the text line.
 *  - If any inline formula is taller than the style's fixed line height, that
 *    paragraph switches to natural line height so the line grows instead of
 *    the formula overlapping the lines around it.
 *  - Display blocks are separate composables (not text placeholders) and use the
 *    library's own line breaking when they are wider than the screen.
 *  - Brace-less shorthand like `\dfrac12` is rewritten by [LatexNormalizer].
 *
 * Plain text with no `$` renders exactly like a normal [Text], so it is safe to use
 * MathText for every question, option and explanation.
 *
 * With a finite [maxLines] (e.g. a collapsed row) only the first block is shown,
 * cut with an ellipsis.
 */
@Composable
fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = ProstutiTypography.bnBodyLarge,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = style.fontSize,
    horizontalPadding: Dp = 32.dp,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val availableWidthPx = with(density) { (maxWidth - horizontalPadding).toPx() }.coerceAtLeast(1f)
        val resolvedColor = color.takeOrElse { LocalContentColor.current }

        val config = remember(fontSize, resolvedColor) {
            LatexConfig(
                fontSize = fontSize,
                color = resolvedColor,
                darkColor = resolvedColor,
                mathFont = MathFont.KaTeXTTF
            )
        }
        val measurer = rememberLatexMeasurer(config)

        val blocks = remember(text, availableWidthPx, config, measurer, density) {
            buildBlocks(text, availableWidthPx, config, measurer, density)
        }

        val textStyle = remember(style, fontSize, textAlign) {
            // Keep the style's line spacing ratio when fontSize differs from it.
            val lh = style.lineHeight
            val scaledLineHeight = if (lh.isSp && style.fontSize.isSp && fontSize.isSp) {
                (lh.value * fontSize.value / style.fontSize.value).sp
            } else lh
            style.copy(
                fontSize = fontSize,
                lineHeight = scaledLineHeight,
                textAlign = textAlign,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
            )
        }
        val fixedLineHeightPx = with(density) {
            val lh = textStyle.lineHeight
            when {
                lh.isSp -> lh.toPx()
                lh.isEm -> lh.value * fontSize.toPx()
                else -> Float.MAX_VALUE // already natural height
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            val visibleBlocks = if (maxLines == Int.MAX_VALUE) blocks else blocks.take(1)
            visibleBlocks.forEach { block ->
                when (block) {
                    is Block.Paragraph -> Text(
                        text = block.text,
                        inlineContent = block.inlineContent,
                        color = resolvedColor,
                        style = if (block.tallestPlaceholderPx > fixedLineHeightPx) {
                            textStyle.copy(lineHeight = TextUnit.Unspecified)
                        } else {
                            textStyle
                        }.let { if (block.centered) it.copy(textAlign = TextAlign.Center) else it },
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (block.centered) Modifier.padding(vertical = 4.dp) else Modifier)
                    )

                    is Block.Display -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Latex(
                            latex = block.formula,
                            config = block.config,
                            // Fixed size up front so the list doesn't jump while the
                            // library parses in the background.
                            modifier = Modifier.size(
                                with(density) { block.dims.widthPx.toDp() },
                                with(density) { block.dims.heightPx.toDp() }
                            )
                        )
                    }
                }
            }
        }
    }
}

// ── Model ─────────────────────────────────────────────────────────────────

private sealed interface Block {
    class Paragraph(
        val text: AnnotatedString,
        val inlineContent: Map<String, InlineTextContent>,
        val tallestPlaceholderPx: Float,
        val centered: Boolean
    ) : Block

    class Display(
        val formula: String,
        val config: LatexConfig,
        val dims: LatexDimensions
    ) : Block
}

private val mathRegex = """(\$\$|\$)(.+?)\1""".toRegex(RegexOption.DOT_MATCHES_ALL)

/** Gap so a tall formula never touches the text line above/below it. */
private const val INLINE_VERTICAL_GAP_EM = 0.15f

// ── Layout builder ────────────────────────────────────────────────────────

private fun buildBlocks(
    text: String,
    availableWidthPx: Float,
    baseConfig: LatexConfig,
    measurer: LatexMeasurerState,
    density: Density
): List<Block> {
    val fontPx = with(density) { baseConfig.fontSize.toPx() }
    // Line breaking works on content width; the library adds this padding on each side.
    val contentMaxWidth = (availableWidthPx - 2 * fontPx * CANVAS_H_PADDING).coerceAtLeast(fontPx)
    val config = baseConfig.copy(
        lineBreaking = LineBreakingConfig(enabled = true, maxWidth = contentMaxWidth)
    )

    val blocks = mutableListOf<Block>()
    var paragraph = ParagraphBuilder(density, fontPx, config)
    var lastIndex = 0

    // A literal `%` can't live inside a formula (see LatexNormalizer.splitOnPercent),
    // so render the pieces as separate formulas with a plain "%" between them.
    fun appendPieces(target: ParagraphBuilder, pieces: List<String>, display: Boolean, wrap: (String) -> String) {
        for ((index, rawPiece) in pieces.withIndex()) {
            if (index > 0) target.appendText("%")
            val piece = rawPiece.trim()
            if (piece.isEmpty()) continue
            if (rawPiece.first().isWhitespace()) target.appendText(" ")

            val formula = LatexNormalizer.normalize(piece, display)
            if (formula.isNotEmpty()) {
                val wrapped = wrap(formula)
                val dims = measureCached(measurer, wrapped, config, fontPx, contentMaxWidth)
                if (dims == null) target.appendText(piece) else target.appendFormula(wrapped, dims)
            }

            if (rawPiece.last().isWhitespace()) target.appendText(" ")
        }
    }

    for (match in mathRegex.findAll(text)) {
        paragraph.appendText(text.substring(lastIndex, match.range.first))
        lastIndex = match.range.last + 1

        val isDisplay = match.groupValues[1].length == 2
        val pieces = LatexNormalizer.splitOnPercent(match.groupValues[2].trim())

        if (!isDisplay) {
            appendPieces(paragraph, pieces, display = false) { it }
            continue
        }

        if (pieces.size > 1) {
            // A Display block draws one formula, which can't hold the "%", so lay
            // the pieces out as their own centred line, still at display size.
            paragraph.build()?.let(blocks::add)
            val line = ParagraphBuilder(density, fontPx, config, centered = true)
            appendPieces(line, pieces, display = true) { "\\displaystyle{$it}" }
            line.build()?.let(blocks::add)
            paragraph = ParagraphBuilder(density, fontPx, config)
            continue
        }

        val formula = LatexNormalizer.normalize(pieces[0], display = true)
        if (formula.isEmpty()) continue
        val dims = measureCached(measurer, formula, config, fontPx, contentMaxWidth)
        if (dims == null) {
            paragraph.appendText(match.value) // unparseable: show source
        } else {
            paragraph.build()?.let(blocks::add)
            paragraph = ParagraphBuilder(density, fontPx, config)
            blocks.add(Block.Display(formula, config, dims))
        }
    }
    paragraph.appendText(text.substring(lastIndex))
    paragraph.build()?.let(blocks::add)
    return blocks
}

private class ParagraphBuilder(
    private val density: Density,
    private val fontPx: Float,
    private val config: LatexConfig,
    private val centered: Boolean = false
) {
    private val builder = AnnotatedString.Builder()
    private val inline = mutableMapOf<String, InlineTextContent>()
    private var tallest = 0f
    private var hasContent = false

    fun appendText(s: String) {
        // Drop line breaks that only separated this paragraph from a display block.
        val t = if (hasContent) s else s.trimStart('\n', '\r', ' ')
        if (t.isEmpty()) return
        builder.append(t)
        hasContent = true
    }

    fun appendFormula(formula: String, dims: LatexDimensions) {
        // Centre the placeholder on the formula's math axis (0.25em above the
        // baseline). With TextCenter alignment that axis lands on the middle of
        // the text line, so x², ½ and ∑ all sit naturally next to the words.
        val axisFromTop = dims.baselinePx - fontPx * MATH_AXIS_RATIO
        val half = max(axisFromTop, dims.heightPx - axisFromTop) + fontPx * INLINE_VERTICAL_GAP_EM
        val heightPx = ceil(2 * half)
        val topOffset = (half - axisFromTop).roundToInt()
        val widthPx = ceil(dims.widthPx)
        tallest = max(tallest, heightPx)

        val id = "ltx${inline.size}"
        inline[id] = InlineTextContent(
            Placeholder(
                width = with(density) { widthPx.toSp() },
                height = with(density) { heightPx.toSp() },
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Box(Modifier.fillMaxSize()) {
                Latex(
                    latex = formula,
                    config = config,
                    modifier = Modifier.offset { IntOffset(0, topOffset) }
                )
            }
        }
        builder.appendInlineContent(id, formula)
        hasContent = true
    }

    fun build(): Block.Paragraph? {
        if (!hasContent) return null
        var annotated = builder.toAnnotatedString()
        // Trailing newline before a display block would render as an empty line.
        val trimmedEnd = annotated.text.trimEnd('\n', '\r', ' ').length
        if (trimmedEnd < annotated.length) annotated = annotated.subSequence(0, trimmedEnd)
        if (annotated.isEmpty()) return null
        return Block.Paragraph(annotated, inline, tallest, centered)
    }
}

// ── Measurement cache ─────────────────────────────────────────────────────
// Lazy lists dispose off-screen items, which drops `remember`. Caching the
// measured size means scrolling back to a question doesn't re-measure it.

private const val CANVAS_H_PADDING = 0.15f   // matches library MathConstants
private const val MATH_AXIS_RATIO = 0.25f    // KaTeX axis height in em

private val dimsCache = LruCache<String, LatexDimensions>(1024)

private fun measureCached(
    measurer: LatexMeasurerState,
    formula: String,
    config: LatexConfig,
    fontPx: Float,
    maxWidth: Float
): LatexDimensions? {
    val key = "${fontPx.roundToInt()}|${maxWidth.roundToInt()}|$formula"
    dimsCache.get(key)?.let { return it }
    return measurer.measure(formula, config)?.also { dimsCache.put(key, it) }
}
