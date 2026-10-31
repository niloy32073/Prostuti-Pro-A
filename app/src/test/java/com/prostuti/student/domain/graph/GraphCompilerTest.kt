package com.prostuti.student.domain.graph

import com.prostuti.student.domain.graph.GraphTestSupport.assertClose
import com.prostuti.student.domain.graph.GraphTestSupport.compileLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphCompilerTest {

    private fun kindOf(vararg lines: String) = compileLines(*lines).lines.last().kind
    private fun plotOf(vararg lines: String) = compileLines(*lines).lines.last().plot
    private fun errorOf(vararg lines: String) = compileLines(*lines).lines.last().error?.kind

    @Test fun classifiesEveryTwoDimensionalForm() {
        assertTrue(plotOf("y = x^2") is GraphPlot.ExplicitY)
        assertTrue(plotOf("x^2 = y") is GraphPlot.ExplicitY)
        assertTrue(plotOf("sin x") is GraphPlot.ExplicitY) // bare expression in x
        assertTrue(plotOf("y = 3") is GraphPlot.ExplicitY) // horizontal line
        assertTrue(plotOf("x = y^2") is GraphPlot.ExplicitX)
        assertTrue(plotOf("x = 3") is GraphPlot.ExplicitX) // vertical line
        assertTrue(plotOf("r = 1 + cos θ") is GraphPlot.Polar)
        assertTrue(plotOf("(cos t, sin t)") is GraphPlot.Parametric)
        assertTrue(plotOf("x^2 + y^2 = 25") is GraphPlot.Implicit)
        assertTrue(plotOf("x^2/9 + y^2/4 = 1") is GraphPlot.Implicit)
        assertTrue(plotOf("r = 2θ") is GraphPlot.Polar)
        assertTrue(plotOf("y > x^2") is GraphPlot.ExplicitYInequality)
        assertTrue(plotOf("x ≤ sin y") is GraphPlot.ExplicitXInequality)
        assertTrue(plotOf("x^2 + y^2 < 9") is GraphPlot.Region)
        assertTrue(plotOf("-1 < y < x") is GraphPlot.Region)
        assertTrue(plotOf("(2, 3)") is GraphPlot.Point)
        assertTrue(plotOf("∫_0^2 x^2 dx") is GraphPlot.IntegralArea)
    }

    @Test fun mirroredInequalityFlipsTheRelation() {
        val p = plotOf("x^2 < y") as GraphPlot.ExplicitYInequality
        assertEquals(Relation.GT, p.relation)
    }

    @Test fun regionFieldIsNegativeInside() {
        val p = plotOf("x^2 + y^2 < 9") as GraphPlot.Region
        assertTrue(p.field.evaluate(0.0, 0.0) < 0)
        assertTrue(p.field.evaluate(4.0, 0.0) > 0)
        assertTrue(p.strict)
        val band = plotOf("-1 ≤ y ≤ 1") as GraphPlot.Region
        assertTrue(band.field.evaluate(0.0, 0.5) < 0)
        assertTrue(band.field.evaluate(0.0, 2.0) > 0)
        assertTrue(band.field.evaluate(0.0, -2.0) > 0)
    }

    @Test fun implicitEquationsMayUsePolarNames() {
        val p = plotOf("r = 2") as GraphPlot.Polar
        assertClose(2.0, p.r.evaluate(1.0))
        val implicit = plotOf("r^2 = x") as GraphPlot.Implicit
        assertClose(0.0, implicit.field.evaluate(1.0, 0.0)) // r² − x = 1 − 1
    }

    @Test fun integralBetweenTwoCurvesKeepsBothCurves() {
        val p = plotOf("∫_0^1 (x - x^2) dx") as GraphPlot.IntegralArea
        assertNotNull(p.bottom)
        assertClose(1.0 / 6, p.result.value, 1e-12)
        assertClose(0.25, p.top.evaluate(0.25))
        assertClose(0.0625, p.bottom!!.evaluate(0.25))
        // x² − 1: the right side does not vary, so this is area under one curve.
        assertNull((plotOf("∫_0^1 (x^2 - 1) dx") as GraphPlot.IntegralArea).bottom)
    }

    @Test fun parametersSliderOnlyForPlainNumbers() {
        val graph = compileLines("a = 2", "b = 2a", "c = -1.5")
        assertEquals(true, graph.lines[0].parameter?.isSlider)
        assertEquals(false, graph.lines[1].parameter?.isSlider)
        assertEquals(true, graph.lines[2].parameter?.isSlider)
        assertClose(4.0, graph.parameters.getValue("b"))
    }

    @Test fun functionDefinitionsArePlotted() {
        assertEquals(GraphLineKind.FUNCTION_DEFINITION, kindOf("f(x) = x^2"))
        assertTrue(plotOf("f(x) = x^2") is GraphPlot.ExplicitY)
        assertNull(plotOf("g(a, b) = a + b")) // two inputs: defined, not drawn
        assertTrue(plotOf("f(x) = x^2", "y = f'(x)") is GraphPlot.ExplicitY)
    }

    @Test fun undefinedVariablesAreNamedForSliderSuggestions() {
        val error = compileLines("y = a x^2 + b").lines.last().error!!
        assertEquals(GraphErrorKind.UNDEFINED_VARIABLE, error.kind)
        assertEquals(listOf("a", "b"), error.names)
    }

    @Test fun definitionProblemsAreReported() {
        val dup = compileLines("a = 1", "a = 2")
        assertEquals(GraphErrorKind.DUPLICATE_DEFINITION, dup.lines[0].error?.kind)
        assertEquals(GraphErrorKind.DUPLICATE_DEFINITION, dup.lines[1].error?.kind)
        val cycle = compileLines("a = b + 1", "b = a + 1")
        assertTrue(cycle.lines.all { it.error != null })
        assertEquals(GraphErrorKind.CIRCULAR_DEFINITION, compileLines("f(x) = g(x)", "g(x) = f(x)").lines[0].error?.kind)
        assertEquals(GraphErrorKind.RESERVED_NAME, errorOf("π = 3"))
        assertEquals(GraphErrorKind.INVALID_DEFINITION, errorOf("a = x + 1"))
        assertEquals(GraphErrorKind.BROKEN_DEPENDENCY, errorOf("a = 1", "a = 2", "y = a x"))
    }

    @Test fun threeDimensionalInputIsDeferred() {
        assertEquals(GraphErrorKind.NEEDS_3D, errorOf("z = x^2 + y^2"))
        assertEquals(GraphErrorKind.NEEDS_3D, errorOf("(1, 2, 3)"))
    }

    @Test fun blankAndValueLines() {
        assertEquals(GraphLineKind.EMPTY, kindOf("   "))
        val line = compileLines("2 + 2").lines.last()
        assertEquals(GraphLineKind.VALUE, line.kind)
        assertClose(4.0, line.value!!)
    }

    @Test fun unsupportedCombinationsAreErrorsNotCrashes() {
        assertEquals(GraphErrorKind.UNSUPPORTED_EQUATION, errorOf("x + y"))
        assertEquals(GraphErrorKind.UNSUPPORTED_EQUATION, errorOf("y = t"))
        assertEquals(GraphErrorKind.TUPLE_NOT_ALLOWED, errorOf("2 (1, 2)"))
    }

    @Test fun definedFunctionNameDetection() {
        assertEquals("f", GraphCompiler.definedFunctionName("f(x) = x^2"))
        assertEquals("a_1", GraphCompiler.definedFunctionName("a_{1}(t) = t"))
        assertNull(GraphCompiler.definedFunctionName("y(x) = 2"))
        assertNull(GraphCompiler.definedFunctionName("sin(x) = 2"))
        assertNull(GraphCompiler.definedFunctionName("y = f(x)"))
    }
}
