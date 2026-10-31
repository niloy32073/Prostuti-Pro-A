package com.prostuti.student.domain.graph.plot

import com.prostuti.student.domain.graph.GraphTestSupport.assertClose
import com.prostuti.student.domain.graph.GraphTestSupport.compileLines
import com.prostuti.student.domain.graph.RealFunction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.math.tan

class PlottingTest {

    private val view = PlotViewport(-10.0, 10.0, -10.0, 10.0, 800, 800)
    private val plotter = GraphPlotter()

    private fun geometry(vararg lines: String, options: LineOptions = LineOptions(), points: Boolean = false): LineGeometry {
        val graph = compileLines(*lines)
        return plotter.plot(graph.lines.last(), options, view, findPoints = points)!!
    }

    @Test fun tangentIsBrokenAtEveryAsymptote() {
        val curves = ExplicitSampler({ x -> tan(x) }, view).sample()
        // Asymptotes at ±π/2, ±3π/2, ±5π/2 inside [−10, 10] → 7 separate branches.
        assertEquals(7, curves.size)
        // No segment may run from above the screen to below it (that would be a fake asymptote line).
        val h = view.heightPx.toFloat()
        for (c in curves) for (i in 2 until c.size step 2) {
            val a = c[i - 1]
            val b = c[i + 1]
            assertTrue(!((a < 0 && b > h) || (b < 0 && a > h)))
        }
    }

    @Test fun reciprocalSplitsAtZero() {
        val curves = ExplicitSampler({ x -> 1 / x }, view).sample()
        assertEquals(2, curves.size)
    }

    @Test fun squareRootStartsExactlyAtTheDomainEdge() {
        val curves = ExplicitSampler({ x -> sqrt(x) }, view).sample()
        assertEquals(1, curves.size)
        val firstX = view.worldX(curves[0][0])
        assertClose(0.0, firstX, 1e-6)
    }

    @Test fun sampledPointsLieOnTheCurve() {
        val f = RealFunction { x -> x * x * x / 20 - x }
        for (curve in ExplicitSampler(f, view).sample()) {
            for (i in curve.indices step 2) {
                val x = view.worldX(curve[i])
                val y = f.value(x)
                if (y in view.yMin..view.yMax) assertEquals(view.py(y), curve[i + 1], 0.01f)
            }
        }
    }

    @Test fun floorHasNoVerticalRisers() {
        val curves = ExplicitSampler({ x -> kotlin.math.floor(x) }, view).sample()
        assertTrue(curves.size >= 19) // one flat piece per integer step
    }

    @Test fun implicitCircleIsAccurate() {
        val g = geometry("x^2 + y^2 = 25")
        val s = g.segments!!
        assertTrue(s.size > 100)
        for (i in s.indices step 2) {
            val r = hypot(view.worldX(s[i]), view.worldY(s[i + 1]))
            assertClose(5.0, r, 2e-3)
        }
    }

    @Test fun implicitPoleIsNotDrawnAsACurve() {
        // y = 1/x written implicitly: x·y = 1 has no points near x = 0 even though 1/x − y flips sign there.
        val g = geometry("1/x - y = 0")
        val s = g.segments!!
        for (i in s.indices step 2) assertTrue(abs(view.worldX(s[i])) > 0.04)
    }

    @Test fun regionShadesTheInside() {
        val g = geometry("x^2 + y^2 < 9")
        assertTrue(g.dashed)
        assertTrue(g.fillRects!!.isNotEmpty())
        // every full rect lies inside the circle (allowing one cell of slack)
        val r = g.fillRects!!
        for (i in r.indices step 4) {
            val cx = view.worldX((r[i] + r[i + 2]) / 2)
            val cy = view.worldY((r[i + 1] + r[i + 3]) / 2)
            assertTrue(hypot(cx, cy) < 3.0)
        }
    }

    @Test fun inequalityFillsTowardTheRightSide() {
        val above = geometry("y > x").fills.single()
        // closing points are at the top edge (py = 0)
        assertEquals(0f, above[above.size - 1])
        val below = geometry("y ≤ x")
        assertEquals(view.heightPx.toFloat(), below.fills.single().last())
        assertTrue(!below.dashed)
    }

    @Test fun pointsOfInterestAreExact() {
        val g = geometry("y = x^3 - 3x", points = true)
        val roots = g.pointsOfInterest.filter { it.kind == PointKind.ROOT }.map { it.x }.sorted()
        assertEquals(3, roots.size)
        assertClose(-sqrt(3.0), roots[0], 1e-12)
        assertClose(0.0, roots[1], 1e-12)
        assertClose(sqrt(3.0), roots[2], 1e-12)
        val max = g.pointsOfInterest.single { it.kind == PointKind.MAXIMUM }
        assertClose(-1.0, max.x, 1e-12)
        assertClose(2.0, max.y, 1e-12)
        val min = g.pointsOfInterest.single { it.kind == PointKind.MINIMUM }
        assertClose(1.0, min.x, 1e-12)
        assertClose(-2.0, min.y, 1e-12)
    }

    @Test fun doubleRootIsFoundWithoutSignChange() {
        val g = geometry("y = (x - 2)^2", points = true)
        val root = g.pointsOfInterest.single { it.kind == PointKind.ROOT }
        assertClose(2.0, root.x, 1e-9)
    }

    @Test fun polesAreNotRoots() {
        val g = geometry("y = tan x", points = true)
        val roots = g.pointsOfInterest.filter { it.kind == PointKind.ROOT }
        for (r in roots) assertClose(0.0, kotlin.math.sin(r.x), 1e-9) // only kπ, never π/2 + kπ
        assertEquals(7, roots.size) // −3π … 3π
    }

    @Test fun intersectionsBetweenCurves() {
        val graph = compileLines("y = x^2", "y = x + 2")
        val others = graph.lines.mapNotNull { it.plot as? com.prostuti.student.domain.graph.GraphPlot.ExplicitY }
        val g = plotter.plot(graph.lines[0], LineOptions(), view, findPoints = true, others = others)!!
        val xs = g.pointsOfInterest.filter { it.kind == PointKind.INTERSECTION }.map { it.x }.sorted()
        assertEquals(2, xs.size)
        assertClose(-1.0, xs[0], 1e-12)
        assertClose(2.0, xs[1], 1e-12)
    }

    @Test fun tangentLineHasTheDerivativeSlope() {
        val g = geometry("y = x^2", options = LineOptions(tangentAt = 3.0))
        val t = g.tangent!!
        assertClose(9.0, t.y)
        assertClose(6.0, t.slope)
    }

    @Test fun riemannSumsMatchHandCalculation() {
        // ∫_0^2 x² dx with n = 4, h = 0.5
        val left = geometry("∫_0^2 x^2 dx", options = LineOptions(riemann = RiemannMode.LEFT, riemannCount = 4))
        assertClose(0.5 * (0 + 0.25 + 1 + 2.25), left.riemannSum!!)
        assertEquals(4, left.riemannPolygons.size)
        val right = geometry("∫_0^2 x^2 dx", options = LineOptions(riemann = RiemannMode.RIGHT, riemannCount = 4))
        assertClose(0.5 * (0.25 + 1 + 2.25 + 4), right.riemannSum!!)
        val mid = geometry("∫_0^2 x^2 dx", options = LineOptions(riemann = RiemannMode.MIDPOINT, riemannCount = 4))
        assertClose(0.5 * (0.0625 + 0.5625 + 1.5625 + 3.0625), mid.riemannSum!!)
        val trap = geometry("∫_0^2 x^2 dx", options = LineOptions(riemann = RiemannMode.TRAPEZOID, riemannCount = 4))
        assertClose(0.25 * (0 + 2 * 0.25 + 2 * 1 + 2 * 2.25 + 4), trap.riemannSum!!)
        // more rectangles converge to 8/3
        val fine = geometry("∫_0^2 x^2 dx", options = LineOptions(riemann = RiemannMode.MIDPOINT, riemannCount = 1000))
        assertClose(8.0 / 3, fine.riemannSum!!, 1e-6)
    }

    @Test fun integralAreaSplitsPositiveAndNegative() {
        val g = geometry("∫_(-π)^π sin(x) dx")
        assertEquals(1, g.fills.size)
        assertEquals(1, g.negativeFills.size)
        val between = geometry("∫_0^1 (x - x^2) dx")
        assertEquals(2, between.curves.size) // both curves are drawn
        assertTrue(between.fills.isNotEmpty())
        assertTrue(between.negativeFills.isEmpty())
    }

    @Test fun polarAndParametricCurvesCloseUp() {
        val circle = geometry("r = 3").curves.single()
        val n = circle.size
        assertEquals(circle[0], circle[n - 2], 0.5f)
        assertEquals(circle[1], circle[n - 1], 0.5f)
        val para = geometry("(cos t, sin t)").curves.single()
        for (i in para.indices step 2) assertClose(1.0, hypot(view.worldX(para[i]), view.worldY(para[i + 1])), 1e-3)
    }

    @Test fun gridStepsAndLabels() {
        assertEquals(1.0, GridTicks.step(20.0 / 800, targetPx = 40.0).major, 0.0)
        assertEquals(2.0, GridTicks.step(1.8 / 96).major, 0.0)
        val pi = GridTicks.step(PI / 2 / 96, piAxis = true)
        assertClose(PI / 2, pi.major)
        assertEquals("3π/2", GridTicks.label(3 * PI / 2, pi))
        assertEquals("−π", GridTicks.label(-PI, pi))
        assertEquals("0.3", GridTicks.label(0.30000000004, GridStep(0.1, 5, false)))
        assertEquals(listOf(-2.0, -1.0, 0.0, 1.0, 2.0), GridTicks.ticks(-2.5, 2.5, 1.0))
    }
}
