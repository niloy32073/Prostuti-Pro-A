package com.prostuti.student.domain.graph

import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.graph.GraphTestSupport.assertClose
import com.prostuti.student.domain.graph.GraphTestSupport.explicit
import com.prostuti.student.domain.graph.GraphTestSupport.value
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.sqrt

class ExpressionEvaluationTest {

    @Test fun realRootsOfNegativeNumbers() {
        val cubeRoot = explicit("y = x^(1/3)").f
        assertClose(-2.0, cubeRoot.evaluate(-8.0))
        assertClose(4.0, explicit("y = x^(2/3)").f.evaluate(-8.0))
        assertTrue(explicit("y = x^(1/2)").f.evaluate(-4.0).isNaN())
        assertClose(-2.0, value("cbrt(-8)"))
        assertClose(-2.0, value("nthroot(-32, 5)"))
        assertTrue(value("nthroot(-16, 4)").isNaN())
    }

    @Test fun degreeModeConvertsTrigInAndOut() {
        assertClose(0.5, value("sin(30)", angle = AngleUnit.DEG))
        assertClose(1.0, value("tan(45)", angle = AngleUnit.DEG))
        assertClose(60.0, value("acos(0.5)", angle = AngleUnit.DEG))
        assertClose(0.5, value("sin(π/6)", angle = AngleUnit.RAD))
    }

    @Test fun reciprocalAndHyperbolicTrig() {
        assertClose(2.0, value("sec(π/3)"))
        assertClose(2.0, value("csc(π/6)"))
        assertClose(1.0, value("cot(π/4)"))
        assertClose(PI / 4, value("acot(1)"))
        assertClose((E - 1 / E) / 2, value("sinh(1)"))
        assertClose(1.0, value("cosh(0)"))
    }

    @Test fun integerHelpers() {
        assertClose(10.0, value("nCr(5, 2)"))
        assertClose(20.0, value("nPr(5, 2)"))
        assertClose(120.0, value("5!"))
        assertClose(sqrt(PI), value("gamma(0.5)"), 1e-12)
        assertClose(2.0, value("mod(-1, 3)"))
        assertClose(6.0, value("gcd(12, 18)"))
        assertClose(36.0, value("lcm(12, 18)"))
        assertClose(3.0, value("round(2.5)"))
        assertClose(-3.0, value("floor(-2.5)"))
        assertClose(-2.0, value("ceil(-2.5)"))
        assertClose(5.0, value("max(1, 5, 3)"))
    }

    @Test fun seriesAndProducts() {
        assertClose(385.0, value("Σ_(n=1)^10 n^2"))
        assertClose(120.0, value("∏_(k=1)^5 k"))
        assertClose(PI * PI / 6, value("Σ_(n=1)^(100000) 1/n^2"), 1e-4)
    }

    @Test fun parametersAndDerivedParameters() {
        assertClose(14.0, value("a = 2", "b = 3a + 1", "a b"))
        val line = GraphTestSupport.compileLines("a = 2", "y = a x^2").lines.last()
        assertClose(18.0, (line.plot as GraphPlot.ExplicitY).f.evaluate(3.0))
    }

    @Test fun userFunctionsInlineAndCompose() {
        assertClose(9.0, value("f(x) = x^2", "f(3)"))
        assertClose(26.0, value("f(x) = x^2", "g(x) = f(x) + 1", "g(5)"))
        assertClose(13.0, value("h(a, b) = a^2 + b^2", "h(2, 3)"))
        // The argument named like the integral's own variable must not be captured.
        assertClose(4.5, value("F(x) = ∫_0^x t dt", "F(3)"))
        assertClose(7.5, value("F(x) = ∫_0^x t dt", "g(t) = F(t) + t", "g(3)"))
    }

    @Test fun constantFoldingKeepsUndefinedUndefined() {
        assertTrue(value("ln(-1)").isNaN())
        assertTrue(value("0/0").isNaN())
        assertTrue(value("sqrt(-1)").isNaN())
    }
}
