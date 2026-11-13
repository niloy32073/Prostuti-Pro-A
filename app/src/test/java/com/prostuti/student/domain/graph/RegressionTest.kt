package com.prostuti.student.domain.graph

import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.graph.GraphTestSupport.assertClose
import com.prostuti.student.domain.graph.GraphTestSupport.compileLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

class RegressionTest {

    private val xs = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)

    @Test fun exactLineIsRecovered() {
        val ys = DoubleArray(xs.size) { 2.5 * xs[it] - 1 }
        val r = Regression.fit(xs, ys, RegressionModel.LINEAR)!!
        assertClose(2.5, r.coefficients[0], 1e-12)
        assertClose(-1.0, r.coefficients[1], 1e-12)
        assertClose(1.0, r.rSquared, 1e-12)
        assertClose(1.0, r.r!!, 1e-12)
        assertEquals("y = 2.5 x - 1", r.display)
    }

    @Test fun noisyLineMatchesTheClosedForm() {
        val x = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0)
        val y = doubleArrayOf(1.1, 2.9, 5.2, 6.8, 9.1)
        // m = Sxy / Sxx, c = ȳ − m x̄ (hand computed: Sxy = 19.9, Sxx = 10)
        val r = Regression.fit(x, y, RegressionModel.LINEAR)!!
        assertClose(1.99, r.coefficients[0], 1e-12)
        assertClose(1.04, r.coefficients[1], 1e-12)
        assertTrue(r.rSquared > 0.99 && r.rSquared < 1)
    }

    @Test fun quadraticAndCubicAreExactOnPolynomialData() {
        val quad = DoubleArray(xs.size) { 3 * xs[it].pow(2) - 2 * xs[it] + 7 }
        val q = Regression.fit(xs, quad, RegressionModel.QUADRATIC)!!
        assertClose(3.0, q.coefficients[0], 1e-9)
        assertClose(-2.0, q.coefficients[1], 1e-9)
        assertClose(7.0, q.coefficients[2], 1e-9)
        val cub = DoubleArray(xs.size) { 0.5 * xs[it].pow(3) - xs[it] + 2 }
        val c = Regression.fit(xs, cub, RegressionModel.CUBIC)!!
        assertClose(0.5, c.coefficients[0], 1e-9)
        assertClose(0.0, c.coefficients[1], 1e-8)
        assertClose(-1.0, c.coefficients[2], 1e-8)
        assertClose(2.0, c.coefficients[3], 1e-8)
    }

    @Test fun polynomialFitIsStableFarFromZero() {
        // years-like x values: a naive normal-equation fit loses all precision here
        val x = DoubleArray(8) { 2000.0 + it }
        val y = DoubleArray(8) { 0.25 * (x[it] - 2003).pow(2) + 4 }
        val q = Regression.fit(x, y, RegressionModel.QUADRATIC)!!
        val f = Regression.function(RegressionModel.QUADRATIC, q.coefficients)
        for (i in x.indices) assertClose(y[i], f(x[i]), 1e-6)
    }

    @Test fun exponentialPowerAndLogarithmicFits() {
        val e = DoubleArray(xs.size) { 2 * exp(0.3 * xs[it]) }
        val fe = Regression.fit(xs, e, RegressionModel.EXPONENTIAL)!!
        assertClose(2.0, fe.coefficients[0], 1e-9)
        assertClose(0.3, fe.coefficients[1], 1e-9)
        val p = DoubleArray(xs.size) { 2.006 * xs[it].pow(0.5) } // pendulum: T = 2.006 √L
        val fp = Regression.fit(xs, p, RegressionModel.POWER)!!
        assertClose(2.006, fp.coefficients[0], 1e-9)
        assertClose(0.5, fp.coefficients[1], 1e-9)
        val l = DoubleArray(xs.size) { 1 + 3 * ln(xs[it]) }
        val fl = Regression.fit(xs, l, RegressionModel.LOGARITHMIC)!!
        assertClose(1.0, fl.coefficients[0], 1e-9)
        assertClose(3.0, fl.coefficients[1], 1e-9)
    }

    @Test fun nonlinearFitMinimisesRealErrorsNotLogErrors() {
        val x = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0)
        val y = doubleArrayOf(1.0, 2.9, 7.5, 19.0, 56.0)
        val fit = Regression.fit(x, y, RegressionModel.EXPONENTIAL)!!
        val f = Regression.function(RegressionModel.EXPONENTIAL, fit.coefficients)
        fun sse(g: (Double) -> Double) = x.indices.sumOf { (y[it] - g(x[it])).pow(2) }
        // the log-linear start, for comparison
        val ly = y.map { ln(it) }
        val mx = x.average(); val my = ly.average()
        val b = x.indices.sumOf { (x[it] - mx) * (ly[it] - my) } / x.indices.sumOf { (x[it] - mx).pow(2) }
        val a = exp(my - b * mx)
        assertTrue(sse(f) < sse { a * exp(b * it) })
    }

    @Test fun impossibleFitsReturnNull() {
        assertNull(Regression.fit(doubleArrayOf(1.0), doubleArrayOf(2.0), RegressionModel.LINEAR))
        assertNull(Regression.fit(xs, DoubleArray(xs.size) { -1.0 }, RegressionModel.EXPONENTIAL)) // needs y > 0
        assertNull(Regression.fit(doubleArrayOf(-1.0, 2.0), doubleArrayOf(1.0, 2.0), RegressionModel.POWER)) // needs x > 0
        assertNull(Regression.fit(doubleArrayOf(2.0, 2.0, 2.0), doubleArrayOf(1.0, 2.0, 3.0), RegressionModel.LINEAR)) // vertical
    }

    // ── tables, fields, notes in the compiler ───────────────────────────────────────────

    @Test fun tableLineFitsAndDrawsItsCurve() {
        val graph = GraphCompiler().compile(
            listOf(
                GraphLineInput(0, "k = 2"),
                GraphLineInput(1, "", TableInput(listOf("1", "2", "3", "", "π"), listOf("k", "4", "6", "", "2π"), RegressionModel.LINEAR)),
            ),
            AngleUnit.RAD,
        )
        val line = graph.lines[1]
        assertEquals(GraphLineKind.TABLE, line.kind)
        val table = line.plot as GraphPlot.Table
        assertEquals(4, table.xs.size) // the blank row is skipped, k and π are evaluated
        assertClose(2.0, table.result!!.coefficients[0], 1e-12)
        assertClose(14.0, table.fit!!.f.evaluate(7.0), 1e-9)
        assertTrue(line.latex!!.startsWith("y = 2"))
    }

    @Test fun tableWithoutFitIsJustPoints() {
        val graph = GraphCompiler().compile(
            listOf(GraphLineInput(0, "", TableInput(listOf("1", "2"), listOf("3", "4"), RegressionModel.NONE))),
            AngleUnit.RAD,
        )
        val table = graph.lines[0].plot as GraphPlot.Table
        assertNull(table.fit)
        assertEquals(2, table.xs.size)
    }

    @Test fun vectorFieldsAndNotes() {
        val g = compileLines("field(-y, x)", "# magnetic field around a wire", "field(x, z)")
        assertEquals(GraphLineKind.VECTOR_FIELD, g.lines[0].kind)
        val f = g.lines[0].plot as GraphPlot.VectorField
        assertClose(-2.0, f.p.evaluate(1.0, 2.0))
        assertEquals(GraphLineKind.NOTE, g.lines[1].kind)
        assertNull(g.lines[1].error)
        assertEquals(GraphErrorKind.UNSUPPORTED_EQUATION, g.lines[2].error?.kind) // fields are 2D only
    }
}
