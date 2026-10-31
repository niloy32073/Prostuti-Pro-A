package com.prostuti.student.domain.graph.plot

import com.prostuti.student.domain.graph.RealFunction
import kotlin.math.abs

/**
 * Adaptive sampler for y = f(x) (or x = g(y) when [transposed]). It starts with one sample every
 * two pixels and halves any interval whose midpoint is more than a third of a pixel off the chord,
 * so curves are smooth at every zoom without wasting samples on straight parts.
 *
 * - Domain edges (√x at 0, ln x near 0) are found by bisection, so the curve ends exactly there.
 * - A jump that does not shrink when the interval is halved (tan at π/2, 1/x at 0, floor) is a
 *   discontinuity: the line is broken instead of drawing a false vertical segment.
 */
internal class ExplicitSampler(
    private val f: RealFunction,
    private val viewport: PlotViewport,
    private val transposed: Boolean = false,
    /** Optional input range (integral bounds); null = the whole viewport. */
    private val uRange: ClosedFloatingPointRange<Double>? = null,
) {
    private val builder = CurveBuilder()
    private val span: Float = if (transposed) viewport.widthPx.toFloat() else viewport.heightPx.toFloat()
    private val clamp: Float = span * 3f

    fun sample(): List<FloatArray> {
        val uMinView = if (transposed) viewport.yMin else viewport.xMin
        val uMaxView = if (transposed) viewport.yMax else viewport.xMax
        val uMin = maxOf(uMinView, uRange?.start ?: uMinView)
        val uMax = minOf(uMaxView, uRange?.endInclusive ?: uMaxView)
        if (!(uMax > uMin)) return emptyList()
        val pixelsAlong = (if (transposed) viewport.heightPx else viewport.widthPx) * (uMax - uMin) / (uMaxView - uMinView)
        val n = (pixelsAlong / 2).toInt().coerceIn(8, 4000)
        var u0 = uMin
        var v0 = f.value(u0)
        if (v0.isFinite()) emit(u0, v0)
        for (i in 1..n) {
            val u1 = if (i == n) uMax else uMin + (uMax - uMin) * i / n
            val v1 = f.value(u1)
            segment(u0, v0, u1, v1, 0)
            u0 = u1
            v0 = v1
        }
        return builder.finish()
    }

    private fun vPx(v: Double): Float {
        val p = if (transposed) viewport.px(v) else viewport.py(v)
        return p.coerceIn(-clamp, span + clamp)
    }

    private fun emit(u: Double, v: Double) {
        if (transposed) builder.add(vPx(v), viewport.py(u)) else builder.add(viewport.px(u), vPx(v))
    }

    /** Processes (u0, v0] → (u1, v1]; the start point is already emitted when it is finite. */
    private fun segment(u0: Double, v0: Double, u1: Double, v1: Double, depth: Int) {
        val ok0 = v0.isFinite()
        val ok1 = v1.isFinite()
        if (!ok0 && !ok1) {
            if (depth < GAP_DEPTH) {
                // A tiny defined island between two undefined samples (rare): look once more.
                val um = 0.5 * (u0 + u1)
                val vm = f.value(um)
                if (vm.isFinite()) {
                    segment(u0, v0, um, vm, depth + 1)
                    segment(um, vm, u1, v1, depth + 1)
                    return
                }
            }
            builder.breakLine()
            return
        }
        if (ok0 != ok1) {
            edge(u0, v0, u1, v1, ok0, depth)
            return
        }
        val p0 = vPx(v0)
        val p1 = vPx(v1)
        if (depth >= MAX_DEPTH) {
            if (abs(p1 - p0) > JUMP_PX) builder.breakLine() else Unit
            emit(u1, v1)
            return
        }
        val um = 0.5 * (u0 + u1)
        val vm = f.value(um)
        if (!vm.isFinite()) {
            segment(u0, v0, um, vm, depth + 1)
            segment(um, vm, u1, v1, depth + 1)
            return
        }
        val pm = vPx(vm)
        val offscreenSameSide = (p0 < 0 && p1 < 0 && pm < 0) || (p0 > span && p1 > span && pm > span)
        val chordError = abs(pm - 0.5f * (p0 + p1))
        if (offscreenSameSide || (chordError < 0.33f && abs(p1 - p0) < 8f)) {
            emit(u1, v1)
        } else {
            segment(u0, v0, um, vm, depth + 1)
            segment(um, vm, u1, v1, depth + 1)
        }
    }

    /** One end defined, the other not: bisect to the domain edge so the curve stops exactly there. */
    private fun edge(u0: Double, v0: Double, u1: Double, v1: Double, startDefined: Boolean, depth: Int) {
        var good = if (startDefined) u0 else u1
        var bad = if (startDefined) u1 else u0
        var goodV = if (startDefined) v0 else v1
        repeat(EDGE_ITERATIONS) {
            val m = 0.5 * (good + bad)
            val vm = f.value(m)
            if (vm.isFinite()) { good = m; goodV = vm } else bad = m
        }
        if (startDefined) {
            segment(u0, v0, good, goodV, depth + 1)
            builder.breakLine()
        } else {
            builder.breakLine()
            emit(good, goodV)
            segment(good, goodV, u1, v1, depth + 1)
        }
    }

    private companion object {
        const val MAX_DEPTH = 12
        const val GAP_DEPTH = 2
        const val EDGE_ITERATIONS = 40

        /** At full depth a continuous curve moves well under this many pixels between samples. */
        const val JUMP_PX = 1.5f
    }
}
