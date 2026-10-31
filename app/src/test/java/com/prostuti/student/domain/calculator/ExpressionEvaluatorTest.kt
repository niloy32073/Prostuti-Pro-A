package com.prostuti.student.domain.calculator

import com.prostuti.student.domain.calculator.CalcToken as T
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExpressionEvaluatorTest {

    private val evaluator = ExpressionEvaluator()
    private val deg = CalcContext(angleUnit = AngleUnit.DEG)

    /** "12" → ["1","2"]; other tokens pass through. */
    private fun t(vararg parts: String): List<String> = parts.flatMap { p ->
        if (p.all { it.isDigit() || it == '.' }) p.map { it.toString() } else listOf(p)
    }

    private fun eval(vararg parts: String, ctx: CalcContext = deg) = evaluator.evaluate(t(*parts), ctx)

    private fun assertError(kind: CalcErrorKind, vararg parts: String) {
        try {
            eval(*parts)
            fail("Expected $kind")
        } catch (e: CalcException) {
            assertEquals(kind, e.kind)
        }
    }

    @Test fun `multiplication binds tighter than addition`() = assertEquals(14.0, eval("2", T.PLUS, "3", T.TIMES, "4"), 0.0)

    @Test fun `decimal sums are clean`() = assertEquals(0.3, eval("0.1", T.PLUS, "0.2"), 0.0)

    @Test fun `implicit multiplication with pi and brackets`() {
        assertEquals(2 * Math.PI, eval("2", T.PI), 1e-12)
        assertEquals(20.0, eval("2", T.OPEN, "3", T.PLUS, "7", T.CLOSE), 0.0)
    }

    @Test fun `negative sign binds looser than square`() = assertEquals(-4.0, eval(T.NEG, "2", T.SQUARE), 0.0)

    @Test fun `power with open exponent bracket`() = assertEquals(1024.0, eval("2", T.POWER, "10"), 0.0)

    @Test fun `trailing brackets may be left out`() = assertEquals(5.0, eval(T.SQRT, "25"), 0.0)

    @Test fun `degree trig gives exact values`() {
        assertEquals(0.0, eval(T.SIN, "180"), 0.0)
        assertEquals(0.5, eval(T.SIN, "30"), 0.0)
        assertEquals(0.0, eval(T.COS, "90"), 0.0)
        assertEquals(30.0, eval(T.ASIN, "0.5"), 0.0)
    }

    @Test fun `radian sin of pi is zero`() =
        assertEquals(0.0, evaluator.evaluate(listOf(T.SIN, T.PI, T.CLOSE), CalcContext(angleUnit = AngleUnit.RAD)), 0.0)

    @Test fun `tan of 90 degrees is a math error`() = assertError(CalcErrorKind.MATH, T.TAN, "90")

    @Test fun `division by zero is a math error`() = assertError(CalcErrorKind.MATH, "1", T.DIVIDE, "0")

    @Test fun `square root of negative is a math error`() = assertError(CalcErrorKind.MATH, T.SQRT, T.NEG, "4")

    @Test fun `dangling operator is a syntax error at its position`() {
        try {
            eval("5", T.TIMES)
            fail()
        } catch (e: CalcException) {
            assertEquals(CalcErrorKind.SYNTAX, e.kind)
            assertEquals(2, e.position)
        }
    }

    @Test fun `cube root of negative via power`() =
        assertEquals(-2.0, eval(T.OPEN, T.NEG, "8", T.CLOSE, T.POWER, "1", T.DIVIDE, "3", T.CLOSE), 1e-12)

    @Test fun `x root`() = assertEquals(3.0, eval("4", T.XROOT, "81"), 1e-12)

    @Test fun `log with base and natural log`() {
        assertEquals(3.0, eval(T.LOG, "2", T.COMMA, "8"), 1e-12)
        assertEquals(2.0, eval(T.LOG, "100"), 0.0)
        assertEquals(1.0, eval(T.LN, T.E), 1e-12)
    }

    @Test fun `factorial permutations combinations`() {
        assertEquals(120.0, eval("5", T.FACTORIAL), 0.0)
        assertEquals(20.0, eval("5", T.NPR, "2"), 0.0)
        assertEquals(10.0, eval("5", T.NCR, "2"), 0.0)
    }

    @Test fun `scientific notation input`() = assertEquals(2500.0, eval("2.5", T.EXP10, "3"), 0.0)

    @Test fun `fraction key and percent`() {
        assertEquals(0.75, eval("3", T.FRACTION, "4"), 0.0)
        assertEquals(0.25, eval("25", T.PERCENT), 0.0)
    }

    @Test fun `sexagesimal input`() = assertEquals(12.5, eval("12", T.DMS, "30", T.DMS), 1e-12)

    @Test fun `variables and Ans`() {
        val ctx = deg.copy(variables = mapOf("A" to 4.0), ans = 10.0)
        assertEquals(14.0, evaluator.evaluate(listOf(T.variable("A"), T.PLUS, T.ANS), ctx), 0.0)
        assertEquals(40.0, evaluator.evaluate(listOf(T.variable("A"), T.ANS), ctx), 0.0)
    }

    @Test fun `integral of x squared`() {
        val tokens = listOf(T.INTEGRAL, T.variable("X"), T.SQUARE, T.COMMA, "0", T.COMMA, "3", T.CLOSE)
        assertEquals(9.0, evaluator.evaluate(tokens, deg), 1e-9)
    }

    @Test fun `derivative of x cubed at 2`() {
        val tokens = listOf(T.DERIVATIVE, T.variable("X"), T.CUBE, T.COMMA, "2", T.CLOSE)
        assertEquals(12.0, evaluator.evaluate(tokens, deg), 1e-8)
    }

    @Test fun `sum of 1 to 100`() {
        val tokens = listOf(T.SUM, T.variable("X"), T.COMMA, "1", T.COMMA, "1", "0", "0", T.CLOSE)
        assertEquals(5050.0, evaluator.evaluate(tokens, deg), 0.0)
    }

    @Test fun `equation is a syntax error outside SOLVE`() = assertError(CalcErrorKind.SYNTAX, "1", T.EQUALS, "1")

    @Test fun `solve x squared equals 2`() {
        val x = T.variable("X")
        val outcome = evaluator.solve(listOf(x, T.SQUARE, T.EQUALS, "2"), "X", 1.0, deg)
        assertEquals(Math.sqrt(2.0), outcome.value, 1e-12)
        assertEquals(0.0, outcome.leftMinusRight, 1e-12)
    }

    @Test fun `solve finds root away from a bad guess`() {
        val x = T.variable("X")
        // X³ − 2X − 5 = 0 → 2.0945514815
        val tokens = listOf(x, T.CUBE, T.MINUS, "2", x, T.MINUS, "5")
        val outcome = evaluator.solve(tokens, "X", 0.0, deg)
        assertEquals(2.0945514815423265, outcome.value, 1e-9)
    }

    @Test fun `solve with no root reports cant solve`() {
        val x = T.variable("X")
        try {
            evaluator.solve(listOf(x, T.SQUARE, T.PLUS, "1"), "X", 0.0, deg)
            fail()
        } catch (e: CalcException) {
            assertEquals(CalcErrorKind.CANT_SOLVE, e.kind)
        }
    }

    @Test fun `variablesIn lists in prompt order`() {
        val tokens = listOf(T.variable("X"), T.PLUS, T.variable("A"))
        assertTrue(evaluator.variablesIn(tokens) == listOf("A", "X"))
    }
}
