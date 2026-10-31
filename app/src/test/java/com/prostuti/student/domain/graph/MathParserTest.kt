package com.prostuti.student.domain.graph

import com.prostuti.student.domain.graph.GraphTestSupport.assertClose
import com.prostuti.student.domain.graph.GraphTestSupport.explicit
import com.prostuti.student.domain.graph.GraphTestSupport.value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

class MathParserTest {

    private fun f(text: String, x: Double) = explicit("y = $text").f.evaluate(x)

    @Test fun precedenceFollowsSchoolRules() {
        assertClose(14.0, value("2 + 3 × 4"))
        assertClose(20.0, value("(2 + 3) × 4"))
        assertClose(-9.0, value("-3^2")) // −(3²)
        assertClose(512.0, value("2^3^2")) // 2^(3^2)
        assertClose(0.25, value("2^-2"))
        assertClose(4.5, value("9/2"))
        assertClose(6.0, value("3!"))
        assertClose(64.0, value("2^3!"))
    }

    @Test fun implicitMultiplication() {
        assertClose(6.0, f("2x", 3.0))
        assertClose(3 * sin(3.0), f("xsinx", 3.0))
        assertClose(2 * PI, value("2π"))
        assertClose(8.0, f("(x+1)(x-1)", 3.0))
        assertClose(12.0, value("a = 3", "b = 4", "a b"))
        assertClose(1.5, f("1/2x", 3.0)) // (1/2)·x, left to right
    }

    @Test fun functionsWithoutBrackets() {
        assertClose(sin(2 * 1.3), f("sin 2x", 1.3))
        assertClose(sin(1.3) * cos(1.3), f("sin x cos x", 1.3))
        assertClose(ln(2.0) + 1, f("ln x + 1", 2.0))
        assertClose(sin(1.3 * 1.3), f("sin x^2", 1.3))
        assertClose(sqrt(2.0) * 5, f("√2 x", 5.0)) // √ takes one factor
    }

    @Test fun trigPowerAndInverseNotation() {
        val x = 0.7
        assertClose(sin(x) * sin(x), f("sin^2 x", x))
        assertClose(sin(x) * sin(x), f("sin²(x)", x))
        assertClose(kotlin.math.asin(0.3), f("sin^-1(x)", 0.3))
        assertClose(kotlin.math.asin(0.3), f("sin⁻¹x", 0.3))
        assertClose(kotlin.math.acos(0.3), f("arccos x", 0.3))
        assertClose(sin(x) * sin(x) + cos(x) * cos(x), f("sin^2x + cos^2x", x))
    }

    @Test fun logarithmBases() {
        assertClose(3.0, value("log(1000)"))
        assertClose(5.0, value("log_2(32)"))
        assertClose(2.0, value("log_{3}(9)"))
        assertClose(1.0, value("ln e"))
    }

    @Test fun absoluteValueBars() {
        assertClose(3.0, f("|x|", -3.0))
        assertClose(6.0, value("|−2| |3|"))
        assertClose(1.0, value("||−3| − 4|"))
    }

    @Test fun keyboardSymbolsAndAsciiAreEquivalent() {
        assertClose(value("2*3-4/2"), value("2×3−4÷2"))
        assertClose(value("sqrt(16)+pi"), value("√(16)+π"))
        assertClose(f("x^2", 1.7), f("x²", 1.7))
    }

    @Test fun subscriptedParameters() {
        assertClose(7.0, value("a_1 = 3", "a_{2} = 4", "a_1 + a_2"))
    }

    @Test fun piecewiseAndRestrictions() {
        val abs = "{x<0: -x, x}"
        assertClose(2.0, f(abs, -2.0))
        assertClose(5.0, f(abs, 5.0))
        val restricted = explicit("y = x^2 {0 ≤ x ≤ 2}").f
        assertClose(1.0, restricted.evaluate(1.0))
        assertTrue(restricted.evaluate(3.0).isNaN())
        assertTrue(f("{x > 0: 1}", -1.0).isNaN()) // no "otherwise" → undefined
    }

    @Test fun missingClosingBracketAtEndIsForgiven() {
        assertClose(sin(1.0), f("sin(x", 1.0))
    }

    @Test fun errorsArePositionedAndTyped() {
        fun err(text: String) = (MathParser.parse(text) as ParseResult.Failure).error
        assertEquals(GraphErrorKind.UNBALANCED_BRACKETS, err("x + 1)").kind)
        assertEquals(GraphErrorKind.UNKNOWN_CHARACTER, err("x # 2").kind)
        assertEquals(GraphErrorKind.SYNTAX, err("2 + ").kind)
        assertEquals(GraphErrorKind.MISSING_INTEGRAL_DIFFERENTIAL, err("∫_0^1 x^2").kind)
        assertEquals(GraphErrorKind.SYNTAX, err("").kind)
        assertEquals(2, err("x # 2").position)
    }

    @Test fun relationChainsParse() {
        val s = (MathParser.parse("-1 < y ≤ x^2") as ParseResult.Success).statement
        assertEquals(listOf(Relation.LT, Relation.LE), s.relations)
        assertEquals(3, s.operands.size)
    }

    @Test fun tuplesParseAsPoints() {
        val s = (MathParser.parse("(1, 2)") as ParseResult.Success).statement
        assertTrue(s.single is MathNode.Tuple)
    }
}
