package com.prostuti.student.ui.screen.examengine.calculator

import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.calculator.CalcErrorKind
import com.prostuti.student.domain.calculator.Complex
import com.prostuti.student.domain.calculator.EquationSolver
import com.prostuti.student.domain.calculator.ExpressionEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Key presses are synchronous state updates, so the state is read straight after each press. */
class CalculatorViewModelTest {

    private val vm = CalculatorViewModel(ExpressionEvaluator(), EquationSolver())
    private val state get() = vm.state.value
    private val lcd get() = CalculatorLcdMapper.map(state)

    private fun press(vararg keys: CalcKey) = keys.forEach(vm::onKey)

    private fun resultValue(): Double = (state.display as CalcDisplay.Result).value

    private fun digits(text: String) = text.forEach { c ->
        press(if (c == '.') CalcKey.DOT else CalcKey.entries.first { it.digit == c - '0' })
    }

    @Test fun `simple sum`() {
        digits("12"); press(CalcKey.PLUS); digits("30"); press(CalcKey.EQUALS)
        assertEquals(42.0, resultValue(), 0.0)
        assertEquals(42.0, state.ans, 0.0)
    }

    @Test fun `one third shows as a fraction and S-D toggles to decimal`() {
        press(CalcKey.D1, CalcKey.DIVIDE, CalcKey.D3, CalcKey.EQUALS)
        val fraction = (lcd.body as LcdContent.Body.Calculation).result
        assertEquals(LcdContent.Value.Fraction(false, 1, 3), fraction)

        press(CalcKey.S_D)
        val decimal = (lcd.body as LcdContent.Body.Calculation).result
        assertEquals(LcdContent.Value.Number("0.3333333333", null), decimal)
    }

    @Test fun `operator after a result continues from Ans`() {
        press(CalcKey.D5, CalcKey.EQUALS, CalcKey.MULTIPLY, CalcKey.D2, CalcKey.EQUALS)
        assertEquals(10.0, resultValue(), 0.0)
        // = again repeats "Ans×2"
        press(CalcKey.EQUALS)
        assertEquals(20.0, resultValue(), 0.0)
    }

    @Test fun `digit after a result starts a new expression`() {
        press(CalcKey.D5, CalcKey.EQUALS, CalcKey.D7, CalcKey.EQUALS)
        assertEquals(7.0, resultValue(), 0.0)
    }

    @Test fun `shift gives the second function and clears itself`() {
        press(CalcKey.SHIFT)
        assertTrue(state.shift)
        press(CalcKey.SIN, CalcKey.D1, CalcKey.EQUALS)   // sin⁻¹(1) = 90°
        assertEquals(90.0, resultValue(), 0.0)
        assertTrue(!state.shift)
    }

    @Test fun `hyp then sin is sinh`() {
        press(CalcKey.HYP, CalcKey.SIN, CalcKey.D0, CalcKey.EQUALS)
        assertEquals(0.0, resultValue(), 0.0)
        assertTrue(!state.hyp)
    }

    @Test fun `setup switches to radians`() {
        press(CalcKey.SHIFT, CalcKey.MODE, CalcKey.D2)
        assertEquals(AngleUnit.RAD, state.angleUnit)
        assertEquals(CalcDisplay.Input, state.display)
    }

    @Test fun `syntax error then goto puts cursor at the error`() {
        press(CalcKey.D5, CalcKey.MULTIPLY, CalcKey.EQUALS)
        assertEquals(CalcErrorKind.SYNTAX, (state.display as CalcDisplay.Error).kind)
        press(CalcKey.LEFT)
        assertEquals(CalcDisplay.Input, state.display)
        assertEquals(2, state.editor.cursor)
    }

    @Test fun `AC clears an error`() {
        press(CalcKey.D1, CalcKey.DIVIDE, CalcKey.D0, CalcKey.EQUALS)
        assertEquals(CalcErrorKind.MATH, (state.display as CalcDisplay.Error).kind)
        press(CalcKey.AC)
        assertEquals(CalcDisplay.Input, state.display)
        assertTrue(state.editor.isEmpty)
    }

    @Test fun `store and recall a variable`() {
        press(CalcKey.D7, CalcKey.EQUALS, CalcKey.SHIFT, CalcKey.RCL, CalcKey.NEGATIVE)   // 7 → A
        assertEquals(7.0, state.variables["A"]!!, 0.0)
        press(CalcKey.AC, CalcKey.ALPHA, CalcKey.NEGATIVE, CalcKey.SQUARE, CalcKey.EQUALS) // A²
        assertEquals(49.0, resultValue(), 0.0)
    }

    @Test fun `memory plus sets the M indicator`() {
        press(CalcKey.D4, CalcKey.M_PLUS)
        assertEquals(4.0, state.variables["M"]!!, 0.0)
        assertTrue("M" in lcd.indicators)
    }

    @Test fun `up arrow recalls the previous calculation`() {
        press(CalcKey.D2, CalcKey.EQUALS, CalcKey.D3, CalcKey.EQUALS, CalcKey.UP)
        assertEquals(2.0, resultValue(), 0.0)
        press(CalcKey.DOWN)
        assertEquals(3.0, resultValue(), 0.0)
    }

    @Test fun `solve x squared minus 4 starting at 1`() {
        // X² − 4  (no "=" means = 0)
        press(CalcKey.ALPHA, CalcKey.CLOSE, CalcKey.SQUARE, CalcKey.MINUS, CalcKey.D4)
        press(CalcKey.SHIFT, CalcKey.CALC)
        assertEquals("X", state.prompt?.current)
        press(CalcKey.D1, CalcKey.EQUALS)
        val result = state.display as CalcDisplay.Result
        assertEquals("X=", result.label)
        assertEquals(2.0, result.value, 1e-12)
        assertEquals(2.0, state.variables["X"]!!, 1e-12)
    }

    @Test fun `calc prompts each variable then evaluates`() {
        // A + B with A = 2, B = 5
        press(CalcKey.ALPHA, CalcKey.NEGATIVE, CalcKey.PLUS, CalcKey.ALPHA, CalcKey.DMS, CalcKey.CALC)
        assertEquals("A", state.prompt?.current)
        press(CalcKey.D2, CalcKey.EQUALS)
        assertEquals("B", state.prompt?.current)
        press(CalcKey.D5, CalcKey.EQUALS)
        assertNull(state.prompt)
        assertEquals(7.0, resultValue(), 0.0)
    }

    @Test fun `eqn quadratic through the keys`() {
        press(CalcKey.MODE, CalcKey.D2, CalcKey.D4)          // EQN → aX²+bX+c=0
        assertEquals(EquationType.QUADRATIC, state.equation?.type)
        press(CalcKey.D1, CalcKey.EQUALS)                     // a = 1
        press(CalcKey.NEGATIVE, CalcKey.D5, CalcKey.EQUALS)   // b = −5
        press(CalcKey.D6, CalcKey.EQUALS)                     // c = 6
        press(CalcKey.EQUALS)                                 // solve
        val roots = state.equation?.roots!!
        assertEquals(listOf(Complex(3.0), Complex(2.0)), roots.map { it.value })
        assertEquals("X1=", (lcd.body as LcdContent.Body.Calculation).input)
        assertTrue(lcd.showDown)
        press(CalcKey.EQUALS)
        assertEquals(1, state.equation?.rootIndex)
        press(CalcKey.EQUALS)                                 // past the last root → back to coefficients
        assertNull(state.equation?.roots)
    }

    @Test fun `eqn four unknowns`() {
        press(CalcKey.MODE, CalcKey.D2, CalcKey.D3)
        val rows = listOf(
            listOf("1", "1", "1", "1", "10"),
            listOf("1", "-1", "1", "-1", "-2"),
            listOf("1", "2", "0", "1", "9"),
            listOf("2", "0", "-1", "1", "3"),
        )
        rows.flatten().forEach { cell ->
            if (cell.startsWith("-")) { press(CalcKey.NEGATIVE); digits(cell.drop(1)) } else digits(cell)
            press(CalcKey.EQUALS)
        }
        press(CalcKey.EQUALS)
        val roots = state.equation?.roots!!
        assertEquals(listOf("X=", "Y=", "Z=", "T="), roots.map { it.label })
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), roots.map { it.value.re })
    }

    @Test fun `eqn parallel lines show no solution`() {
        press(CalcKey.MODE, CalcKey.D2, CalcKey.D1)
        listOf("1", "1", "1", "2", "2", "5").forEach { digits(it); press(CalcKey.EQUALS) }
        press(CalcKey.EQUALS)
        assertEquals(CalcErrorKind.NO_SOLUTION, (state.display as CalcDisplay.Error).kind)
        press(CalcKey.AC)
        assertEquals(CalcDisplay.Input, state.display)
    }

    @Test fun `clear all resets everything after confirmation`() {
        press(CalcKey.D9, CalcKey.SHIFT, CalcKey.RCL, CalcKey.NEGATIVE)    // A = 9
        press(CalcKey.SHIFT, CalcKey.D9, CalcKey.D3)
        assertEquals(CalcDisplay.ConfirmClear(ClearTarget.ALL), state.display)
        press(CalcKey.EQUALS)
        assertEquals(0.0, state.variables["A"]!!, 0.0)
        press(CalcKey.AC)
        assertEquals(CalcDisplay.Input, state.display)
    }

    @Test fun `lcd window keeps the cursor visible`() {
        val (slice, cursor) = lcdWindow("123456789012345678901234", cursor = 24, columns = 10)
        assertEquals("345678901234".takeLast(9), slice)
        assertEquals(9, cursor)
    }
}
