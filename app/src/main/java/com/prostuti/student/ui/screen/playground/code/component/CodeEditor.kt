package com.prostuti.student.ui.screen.playground.code.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.R
import com.prostuti.student.domain.code.CodeHighlighter
import com.prostuti.student.domain.code.CodeLanguage
import com.prostuti.student.domain.code.CodeSpanKind
import com.prostuti.student.ui.theme.MonoFontFamily
import com.prostuti.student.ui.theme.extendedColors

private const val INDENT = "    "

/**
 * A code editor: line numbers, syntax colours, a symbol bar for keys phone keyboards hide, and
 * auto-indent on Enter (one level more after `:` in Python or `{` in C). [highlightLine] and
 * [errorLine] get a band behind them; [scrollToLine] scrolls there when it changes.
 */
@Composable
fun CodeEditor(
    code: String,
    language: CodeLanguage,
    highlightLine: Int?,
    errorLine: Int?,
    scrollToLine: Int?,
    onCodeChange: (code: String, cursorLine: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(code)) }
    LaunchedEffect(code) { if (value.text != code) value = TextFieldValue(code) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val vScroll = rememberScrollState()
    val style = TextStyle(
        fontFamily = MonoFontFamily, fontSize = 14.sp, lineHeight = 21.sp,
        color = MaterialTheme.colorScheme.onSurface, fontFeatureSettings = NO_LIGATURES,
    )
    val colors = MaterialTheme.colorScheme
    val spanStyles = mapOf(
        CodeSpanKind.KEYWORD to SpanStyle(color = colors.primary, fontWeight = FontWeight.Medium),
        CodeSpanKind.TYPE to SpanStyle(color = colors.secondary, fontWeight = FontWeight.Medium),
        CodeSpanKind.STRING to SpanStyle(color = MaterialTheme.extendedColors.success),
        CodeSpanKind.NUMBER to SpanStyle(color = colors.tertiary),
        CodeSpanKind.CALL to SpanStyle(fontWeight = FontWeight.Medium),
        CodeSpanKind.COMMENT to SpanStyle(color = colors.onSurfaceVariant, fontStyle = FontStyle.Italic),
        CodeSpanKind.DIRECTIVE to SpanStyle(color = colors.secondary),
    )
    val highlighting = remember(language, spanStyles) { SyntaxColors(language, spanStyles) }
    val lineCount = value.text.count { it == '\n' } + 1
    val selectedBand = colors.primary.copy(alpha = 0.12f)
    val errorBand = colors.error.copy(alpha = 0.14f)
    val editorDescription = stringResource(R.string.code_editor_desc)

    LaunchedEffect(scrollToLine, layout) {
        val l = layout ?: return@LaunchedEffect
        val line = scrollToLine ?: return@LaunchedEffect
        if (line - 1 !in 0 until l.lineCount) return@LaunchedEffect
        vScroll.animateScrollTo((l.getLineTop(line - 1) - 3 * l.size.height / lineCount.coerceAtLeast(1)).toInt().coerceAtLeast(0))
    }

    fun update(next: TextFieldValue) {
        val indented = autoIndent(value, next, language)
        value = indented
        onCodeChange(indented.text, lineOf(indented.text, indented.selection.start))
    }

    Column(modifier) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(vScroll)
                .drawBehind {
                    val l = layout ?: return@drawBehind
                    fun band(line: Int?, color: Color) {
                        if (line == null || line - 1 !in 0 until l.lineCount) return
                        val top = l.getLineTop(line - 1) + 8.dp.toPx()
                        drawRect(color, Offset(0f, top), Size(size.width, l.getLineBottom(line - 1) - l.getLineTop(line - 1)))
                    }
                    band(highlightLine, selectedBand)
                    band(errorLine, errorBand)
                },
        ) {
            Text(
                text = (1..lineCount).joinToString("\n"),
                style = style.copy(color = colors.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.End),
                modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp).widthIn(min = 24.dp),
            )
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                BasicTextField(
                    value = value,
                    onValueChange = ::update,
                    textStyle = style,
                    cursorBrush = SolidColor(colors.primary),
                    visualTransformation = highlighting,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                    ),
                    onTextLayout = { layout = it },
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 8.dp, end = 24.dp)
                        .widthIn(min = 240.dp)
                        .semantics { contentDescription = editorDescription }
                        .testTag("code_editor"),
                )
            }
        }
        SymbolBar(language) { insert(value, it).let(::update) }
    }
}

/** Keys phone keyboards bury: Tab (four spaces), brackets, operators. */
@Composable
private fun SymbolBar(language: CodeLanguage, onKey: (String) -> Unit) {
    val tab = stringResource(R.string.code_key_tab)
    val keys = remember(language) {
        val common = listOf("(", ")", "[", "]", "=", "<", ">", "+", "-", "*", "/", "%", "\"", "'", "!", "&", "|", ",", ".", "_")
        if (language == CodeLanguage.PYTHON) listOf(":", "#") + common else listOf("{", "}", ";", "#", "&&", "||") + common
    }
    LazyRow(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 4.dp).testTag("symbol_bar"),
    ) {
        // The keys must not take focus, or the keyboard would close on every tap.
        item(key = "tab") {
            SuggestionChip(onClick = { onKey(INDENT) }, label = { Text(tab) }, modifier = Modifier.focusProperties { canFocus = false }.testTag("key_tab"))
        }
        items(keys, key = { it }) { key ->
            SuggestionChip(
                onClick = { onKey(key) },
                label = { Text(key, style = TextStyle(fontFamily = MonoFontFamily, fontFeatureSettings = NO_LIGATURES)) },
                modifier = Modifier.focusProperties { canFocus = false }.testTag("key_$key"),
            )
        }
    }
}

private class SyntaxColors(val language: CodeLanguage, val styles: Map<CodeSpanKind, SpanStyle>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        for (span in CodeHighlighter.spans(text.text, language)) {
            styles[span.kind]?.let { builder.addStyle(it, span.start, span.end) }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    override fun equals(other: Any?) = other is SyntaxColors && other.language == language && other.styles == styles
    override fun hashCode() = language.hashCode() * 31 + styles.hashCode()
}

/** Replaces the selection with [text]. */
private fun insert(value: TextFieldValue, text: String): TextFieldValue {
    val s = value.selection
    val out = value.text.replaceRange(s.min, s.max, text)
    return TextFieldValue(out, TextRange(s.min + text.length))
}

/** When exactly one newline was typed, carries the previous line's indent (plus one level after `:` / `{`). */
private fun autoIndent(old: TextFieldValue, next: TextFieldValue, language: CodeLanguage): TextFieldValue {
    val cursor = next.selection.start
    if (!next.selection.collapsed || next.text.length != old.text.length + 1 || cursor == 0 || next.text[cursor - 1] != '\n') return next
    if (next.text.removeRange(cursor - 1, cursor) != old.text) return next
    val lineStart = next.text.lastIndexOf('\n', cursor - 2) + 1
    val previous = next.text.substring(lineStart, cursor - 1)
    val indent = previous.takeWhile { it == ' ' || it == '\t' }
    val opener = if (language == CodeLanguage.PYTHON) ':' else '{'
    val extra = if (previous.trimEnd().endsWith(opener)) INDENT else ""
    val add = indent + extra
    if (add.isEmpty()) return next
    return TextFieldValue(next.text.substring(0, cursor) + add + next.text.substring(cursor), TextRange(cursor + add.length))
}

/** 1-based line of [offset]. */
private fun lineOf(text: String, offset: Int): Int {
    var line = 1
    for (i in 0 until offset.coerceAtMost(text.length)) if (text[i] == '\n') line++
    return line
}
