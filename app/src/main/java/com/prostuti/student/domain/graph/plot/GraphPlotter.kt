package com.prostuti.student.domain.graph.plot

import com.prostuti.student.domain.graph.CompiledFunction
import com.prostuti.student.domain.graph.CompiledLine
import com.prostuti.student.domain.graph.GraphPlot
import com.prostuti.student.domain.graph.RealFunction
import kotlin.math.cos
import kotlin.math.sin

/**
 * Turns one compiled line into drawable [LineGeometry] for a viewport. Pure and thread-safe:
 * every call makes its own evaluation environments, so lines can be plotted in parallel.
 */
class GraphPlotter {

    /**
     * @param findPoints compute roots/extrema/intersections (only for the selected line — it costs more).
     * @param others the other explicit curves, for intersections with this one.
     */
    fun plot(
        line: CompiledLine,
        options: LineOptions,
        viewport: PlotViewport,
        findPoints: Boolean = false,
        others: List<GraphPlot.ExplicitY> = emptyList(),
    ): LineGeometry? {
        if (!viewport.isValid) return null
        val id = line.id
        return when (val p = line.plot ?: return null) {
            is GraphPlot.ExplicitY -> explicitY(id, p, options, viewport, findPoints, others)
            is GraphPlot.ExplicitX -> LineGeometry(id, viewport, curves = ExplicitSampler(fn(p.g), viewport, transposed = true).sample())
            is GraphPlot.ExplicitYInequality -> {
                val curves = ExplicitSampler(fn(p.f), viewport).sample()
                val edge = if (p.relation == com.prostuti.student.domain.graph.Relation.GT ||
                    p.relation == com.prostuti.student.domain.graph.Relation.GE
                ) 0f else viewport.heightPx.toFloat()
                LineGeometry(id, viewport, curves = curves, dashed = p.relation.isStrict, fills = curves.map { closeToEdge(it, edge, vertical = true) })
            }
            is GraphPlot.ExplicitXInequality -> {
                val curves = ExplicitSampler(fn(p.g), viewport, transposed = true).sample()
                val edge = if (p.relation == com.prostuti.student.domain.graph.Relation.GT ||
                    p.relation == com.prostuti.student.domain.graph.Relation.GE
                ) viewport.widthPx.toFloat() else 0f
                LineGeometry(id, viewport, curves = curves, dashed = p.relation.isStrict, fills = curves.map { closeToEdge(it, edge, vertical = false) })
            }
            is GraphPlot.Polar -> {
                val env = p.r.newEnv()
                val k = p.toRadians
                val curves = CurveSampler(viewport) { t, out ->
                    env[0] = t
                    val r = p.r.eval(env)
                    out[0] = viewport.px(r * cos(t * k))
                    out[1] = viewport.py(r * sin(t * k))
                }.sample(options.rangeMin ?: p.defaultRange.start, options.rangeMax ?: p.defaultRange.endInclusive)
                LineGeometry(id, viewport, curves = curves)
            }
            is GraphPlot.Parametric -> {
                val ex = p.x.newEnv()
                val ey = p.y.newEnv()
                val curves = CurveSampler(viewport) { t, out ->
                    ex[0] = t
                    ey[0] = t
                    out[0] = viewport.px(p.x.eval(ex))
                    out[1] = viewport.py(p.y.eval(ey))
                }.sample(options.rangeMin ?: p.defaultRange.start, options.rangeMax ?: p.defaultRange.endInclusive)
                LineGeometry(id, viewport, curves = curves)
            }
            is GraphPlot.Implicit -> {
                val result = ImplicitPlotter(field(p.field), viewport, fill = false, strict = false).plot()
                LineGeometry(id, viewport, segments = result.segments)
            }
            is GraphPlot.Region -> {
                val result = ImplicitPlotter(field(p.field), viewport, fill = true, strict = p.strict).plot()
                LineGeometry(
                    id, viewport, segments = result.segments, dashed = p.strict,
                    fills = result.polygons, fillRects = result.rects,
                )
            }
            is GraphPlot.Point -> LineGeometry(
                id, viewport,
                points = if (p.x.isFinite() && p.y.isFinite()) listOf(p.x to p.y) else emptyList(),
            )
            is GraphPlot.IntegralArea -> {
                val top = fn(p.top)
                val bottom = p.bottom?.let { fn(it) }
                val curves = ExplicitSampler(top, viewport).sample() + (bottom?.let { ExplicitSampler(it, viewport).sample() } ?: emptyList())
                val area = AreaBuilder.build(top, bottom, p.lower, p.upper, viewport, options.riemann, options.riemannCount)
                LineGeometry(
                    id, viewport, curves = curves,
                    fills = area.positive, negativeFills = area.negative, guides = area.guides,
                    riemannPolygons = area.riemannPolygons, riemannSum = area.riemannSum,
                )
            }
        }
    }

    private fun explicitY(
        id: Long,
        p: GraphPlot.ExplicitY,
        options: LineOptions,
        viewport: PlotViewport,
        findPoints: Boolean,
        others: List<GraphPlot.ExplicitY>,
    ): LineGeometry {
        val f = fn(p.f)
        val df = p.derivative?.let { fn(it) }
        val curves = ExplicitSampler(f, viewport).sample()
        val derivativeCurves = if (options.showDerivative && df != null) ExplicitSampler(df, viewport).sample() else emptyList()
        val tangent = options.tangentAt?.let { x0 -> tangent(f, df, x0, viewport) }
        val points = if (findPoints) {
            val yScale = viewport.yMax - viewport.yMin
            PointOfInterestFinder.forCurve(f, df, viewport.xMin, viewport.xMax, yScale) +
                others.filter { it !== p }.flatMap { other ->
                    PointOfInterestFinder.intersections(f, fn(other.f), viewport.xMin, viewport.xMax, yScale)
                }
        } else {
            emptyList()
        }
        return LineGeometry(
            id, viewport, curves = curves, derivativeCurves = derivativeCurves,
            pointsOfInterest = points, tangent = tangent,
        )
    }

    private fun tangent(f: RealFunction, df: RealFunction?, x0: Double, viewport: PlotViewport): TangentInfo? {
        val y0 = f.value(x0)
        val slope = df?.value(x0) ?: run {
            val h = 1e-5 * maxOf(1.0, kotlin.math.abs(x0))
            (f.value(x0 + h) - f.value(x0 - h)) / (2 * h)
        }
        if (!y0.isFinite() || !slope.isFinite()) return null
        val xa = viewport.xMin
        val xb = viewport.xMax
        val line = floatArrayOf(
            viewport.px(xa), viewport.py(y0 + slope * (xa - x0)).coerceIn(-1e6f, 1e6f),
            viewport.px(xb), viewport.py(y0 + slope * (xb - x0)).coerceIn(-1e6f, 1e6f),
        )
        return TangentInfo(x0, y0, slope, line)
    }

    /** Polyline + its two ends dropped to [edge]: the shaded side of y > f(x) or x < g(y). */
    private fun closeToEdge(curve: FloatArray, edge: Float, vertical: Boolean): FloatArray {
        val n = curve.size
        val out = curve.copyOf(n + 4)
        if (vertical) {
            out[n] = curve[n - 2]; out[n + 1] = edge
            out[n + 2] = curve[0]; out[n + 3] = edge
        } else {
            out[n] = edge; out[n + 1] = curve[n - 1]
            out[n + 2] = edge; out[n + 3] = curve[1]
        }
        return out
    }

    private fun fn(c: CompiledFunction): RealFunction = c.asFunctionOf(0)

    private fun field(c: CompiledFunction): Field2 {
        val env = c.newEnv()
        // ±∞ (1/0) is a pole, not a big number: treat it as undefined so no contour is traced through it.
        return Field2 { x, y ->
            env[0] = x
            env[1] = y
            val v = c.eval(env)
            if (v.isInfinite()) Double.NaN else v
        }
    }
}
