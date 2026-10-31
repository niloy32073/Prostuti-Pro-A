package com.prostuti.student.ui.screen.examengine.calculator

import com.prostuti.student.domain.calculator.CalcErrorKind
import com.prostuti.student.domain.calculator.CalcNumberFormat
import com.prostuti.student.domain.calculator.CalcNumberFormatter
import com.prostuti.student.domain.calculator.CalcToken
import com.prostuti.student.domain.calculator.Complex
import com.prostuti.student.domain.calculator.FormattedNumber

/** Pure mapping from [CalculatorUiState] to the LCD picture. */
object CalculatorLcdMapper {

    private val formatter = CalcNumberFormatter()

    fun map(state: CalculatorUiState): LcdContent {
        val equation = state.equation
        val roots = equation?.roots
        val body = when (val display = state.display) {
            is CalcDisplay.Menu -> LcdContent.Body.Lines(menuLines(display.menu))
            is CalcDisplay.ConfirmClear -> LcdContent.Body.Lines(listOf(display.target.question, "[=] :Yes", "[AC] :Cancel"))
            is CalcDisplay.Message -> LcdContent.Body.Lines(listOf(display.text, "", "[AC]"))
            is CalcDisplay.Error -> LcdContent.Body.Lines(errorLines(display.kind))
            is CalcDisplay.Result -> resultBody(state, display)
            CalcDisplay.Input -> when {
                equation != null && roots != null -> rootBody(roots[equation.rootIndex], equation.rootsAsDecimal, state.numberFormat)
                equation != null -> coefficientBody(state, equation)
                state.prompt != null -> promptBody(state, state.prompt)
                else -> inputBody(state.editor)
            }
        }
        val (up, down) = if (state.display == CalcDisplay.Input && equation != null && roots != null) {
            (equation.rootIndex > 0) to (equation.rootIndex < roots.lastIndex)
        } else {
            false to false
        }
        return LcdContent(indicators = indicators(state), showUp = up, showDown = down, body = body)
    }

    fun format(value: Double, format: CalcNumberFormat): String = formatter.format(value, format).asText()

    private fun indicators(state: CalculatorUiState): List<String> = buildList {
        if (state.shift) add("S")
        if (state.alpha) add("A")
        if (state.memoryInUse) add("M")
        state.memoryCommand?.let { add(it.indicator) }
        if (state.hyp) add("hyp")
        add(state.angleUnit.indicator)
        when (state.numberFormat) {
            is CalcNumberFormat.Fix -> add("FIX")
            is CalcNumberFormat.Sci -> add("SCI")
            is CalcNumberFormat.Norm -> Unit
        }
        if (state.mode == CalcMode.EQN) add("EQN")
    }

    private fun inputBody(editor: ExpressionEditor): LcdContent.Body.Calculation =
        LcdContent.Body.Calculation(input = CalcToken.display(editor.tokens), cursor = cursorOffset(editor))

    private fun promptBody(state: CalculatorUiState, prompt: VariablePrompt): LcdContent.Body.Calculation {
        val label = if (prompt.isSolveTarget) "Solve for ${prompt.current} " else "${prompt.current}? "
        val typed = CalcToken.display(state.editor.tokens)
        return LcdContent.Body.Calculation(
            input = label + typed,
            cursor = label.length + cursorOffset(state.editor),
            result = number(state.variables[prompt.current] ?: 0.0, state.numberFormat, allowFraction = true),
        )
    }

    private fun resultBody(state: CalculatorUiState, r: CalcDisplay.Result): LcdContent.Body.Calculation {
        val value = when {
            r.sexagesimal -> LcdContent.Value.Text(formatter.sexagesimal(r.value))
            r.engineeringStep != null -> formatter.formatEngineering(r.value, r.engineeringStep).toValue()
            else -> number(r.value, state.numberFormat, allowFraction = !r.asDecimal)
        }
        return LcdContent.Body.Calculation(
            input = CalcToken.display(r.expression) + r.suffix,
            resultLabel = r.label,
            result = value,
            footnote = r.residual?.let { "L−R=" + format(it, CalcNumberFormat.Norm(1)) },
        )
    }

    private fun rootBody(root: EquationRoot, asDecimal: Boolean, format: CalcNumberFormat): LcdContent.Body.Calculation {
        val value = if (root.value.isReal) number(root.value.re, format, allowFraction = !asDecimal) else complexValue(root.value, format)
        return LcdContent.Body.Calculation(input = root.label, result = value)
    }

    private fun coefficientBody(state: CalculatorUiState, session: EquationSession): LcdContent.Body.Coefficients {
        val type = session.type
        val editing = !state.editor.isEmpty
        return LcdContent.Body.Coefficients(
            headers = type.columnLabels,
            rowLabels = if (type.isLinear) (1..type.rows).map { it.toString() } else listOf(""),
            cells = session.coefficients.map { row -> row.map { shortCell(it) } },
            selectedRow = session.row,
            selectedColumn = session.column,
            entry = if (editing) CalcToken.display(state.editor.tokens) else format(session.selectedValue, state.numberFormat),
            entryCursor = if (editing) cursorOffset(state.editor) else null,
        )
    }

    private fun number(value: Double, format: CalcNumberFormat, allowFraction: Boolean): LcdContent.Value {
        if (allowFraction) {
            formatter.fraction(value)?.let { return LcdContent.Value.Fraction(it.negative, it.numerator, it.denominator) }
        }
        return formatter.format(value, format).toValue()
    }

    private fun complexValue(z: Complex, format: CalcNumberFormat): LcdContent.Value {
        val re = formatter.format(z.re, format).asText()
        val imMagnitude = formatter.format(kotlin.math.abs(z.im), format).asText()
        val sign = if (z.im < 0) "-" else "+"
        val text = if (z.re == 0.0) "${if (z.im < 0) "-" else ""}${imMagnitude}i" else "$re$sign${imMagnitude}i"
        return LcdContent.Value.Text(text)
    }

    /** Cells are narrow; long values get the compact "1.2E5" form and are cut to 7 characters. */
    private fun shortCell(value: Double): String {
        val text = formatter.format(value, CalcNumberFormat.Norm(1)).asText()
        return if (text.length <= 7) text else formatter.format(value, CalcNumberFormat.Sci(3)).asText().take(9)
    }

    private fun cursorOffset(editor: ExpressionEditor): Int = CalcToken.display(editor.tokens.take(editor.cursor)).length

    private fun FormattedNumber.toValue() = LcdContent.Value.Number(mantissa, exponent)

    private fun FormattedNumber.asText(): String = if (exponent == null) mantissa else "${mantissa}E$exponent"

    private fun errorLines(kind: CalcErrorKind): List<String> = buildList {
        add(kind.label)
        add("")
        add("[AC]  :Cancel")
        if (kind == CalcErrorKind.MATH || kind == CalcErrorKind.SYNTAX) add("[◀][▶]:Goto")
    }

    private fun menuLines(menu: CalcMenu): List<String> = when (menu) {
        CalcMenu.MODE -> listOf("1:COMP", "2:EQN")
        CalcMenu.EQUATION_TYPE -> EquationType.entries.mapIndexed { i, type -> "${i + 1}:${type.menuLabel}" }
        CalcMenu.SETUP -> listOf("1:Deg    2:Rad", "3:Gra    4:Fix", "5:Sci    6:Norm")
        CalcMenu.FIX -> listOf("Fix 0~9?")
        CalcMenu.SCI -> listOf("Sci 0~9?")
        CalcMenu.NORM -> listOf("Norm 1~2?")
        CalcMenu.DRG -> listOf("1:°   2:ʳ   3:ᵍ")
        CalcMenu.CLEAR -> listOf("Clear?", "1:Setup  2:Memory", "3:All")
    }
}
