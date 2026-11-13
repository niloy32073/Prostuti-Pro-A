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
            is GraphPlot.Vector2 -> vector(id, p, viewport)
            is GraphPlot.VectorField -> field(id, p, viewport)
            is GraphPlot.Table -> LineGeometry(
                id, viewport,
                curves = p.fit?.let { ExplicitSampler(fn(it.f), viewport).sample() } ?: emptyList(),
                points = p.xs.indices.map { p.xs[it] to p.ys[it] },
                pointLabels = false,
            )
            is GraphPlot.Surface, is GraphPlot.ImplicitSurface, is GraphPlot.ParametricSurface,
            is GraphPlot.SpaceCurve, is GraphPlot.Point3, is GraphPlot.Vector3,
            is GraphPlot.VolumeUnder, is GraphPlot.Revolution -> null // drawn by the 3D view
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

    /** Shaft as a curve, head as a filled triangle; the head is a fixed size in pixels. */
    private fun vector(id: Long, p: GraphPlot.Vector2, viewport: PlotViewport): LineGeometry {
        val x0 = viewport.px(p.x0)
        val y0 = viewport.py(p.y0)
        val x1 = viewport.px(p.x1)
        val y1 = viewport.py(p.y1)
        val len = kotlin.math.hypot(x1 - x0, y1 - y0)
        if (len < 1e-3f) return LineGeometry(id, viewport, points = listOf(p.x1 to p.y1))
        val ux = (x1 - x0) / len
        val uy = (y1 - y0) / len
        val head = minOf(ARROW_HEAD_PX, len * 0.45f)
        val bx = x1 - ux * head
        val by = y1 - uy * head
        val half = head * 0.45f
        val tip = floatArrayOf(x1, y1, bx - uy * half, by + ux * half, bx + uy * half, by - ux * half)
        return LineGeometry(id, viewport, curves = listOf(floatArrayOf(x0, y0, bx, by)), fills = listOf(tip), solidFills = true)
    }

    /**
     * Arrows on a ~18-across grid. Lengths grow with the square root of the field strength (so weak
     * and strong regions both stay readable) and are capped at one cell; the direction is exact.
     */
    private fun field(id: Long, p: GraphPlot.VectorField, viewport: PlotViewport): LineGeometry {
        val cell = minOf(viewport.widthPx, viewport.heightPx) / FIELD_ARROWS_ACROSS
        val cols = (viewport.widthPx / cell).toInt().coerceAtLeast(1)
        val rows = (viewport.heightPx / cell).toInt().coerceAtLeast(1)
        val ep = p.p.newEnv()
        val eq = p.q.newEnv()
        val samples = ArrayList<FloatArray>(cols * rows)
        var maxMagnitude = 0.0
        for (r in 0 until rows) for (c in 0 until cols) {
            val px = (c + 0.5f) * viewport.widthPx / cols
            val py = (r + 0.5f) * viewport.heightPx / rows
            val x = viewport.worldX(px)
            val y = viewport.worldY(py)
            ep[0] = x; ep[1] = y; eq[0] = x; eq[1] = y
            val u = p.p.eval(ep)
            val v = p.q.eval(eq)
            val m = kotlin.math.hypot(u, v)
            if (!m.isFinite() || m == 0.0) continue
            maxMagnitude = maxOf(maxMagnitude, m)
            // screen direction: y grows downward
            samples += floatArrayOf(px, py, (u / m).toFloat(), (-v / m).toFloat(), m.toFloat())
        }
        val shafts = ArrayList<FloatArray>()
        val heads = ArrayList<FloatArray>()
        val reference = kotlin.math.sqrt(maxMagnitude).takeIf { it > 0 } ?: 1.0
        for ((px, py, dx, dy, m) in samples.map { it.toList() }) {
            val len = (cell * 0.85f * (kotlin.math.sqrt(m.toDouble()) / reference).toFloat()).coerceIn(cell * 0.25f, cell * 0.85f)
            val x0 = px - dx * len / 2
            val y0 = py - dy * len / 2
            val x1 = px + dx * len / 2
            val y1 = py + dy * len / 2
            val head = len * 0.35f
            val bx = x1 - dx * head
            val by = y1 - dy * head
            val half = head * 0.5f
            shafts += floatArrayOf(x0, y0, bx, by)
            heads += floatArrayOf(x1, y1, bx - dy * half, by + dx * half, bx + dy * half, by - dx * half)
        }
        return LineGeometry(id, viewport, curves = shafts, fills = heads, solidFills = true, thinCurves = true)
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

    private companion object {
        const val ARROW_HEAD_PX = 34f
        const val FIELD_ARROWS_ACROSS = 18f
    }
}
