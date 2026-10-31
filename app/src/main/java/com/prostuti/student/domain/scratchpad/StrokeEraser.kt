package com.prostuti.student.domain.scratchpad

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Eraser geometry for the scratch canvas. The eraser is a circle dragged from one point to the
 * next; the drag is sampled every half radius so fast swipes leave no gaps.
 */
class StrokeEraser {

    /** Partial eraser: cuts away only the parts of strokes under the eraser, splitting them. */
    fun erasePartial(strokes: List<ScratchStroke>, from: StrokePoint, to: StrokePoint, radius: Float): List<ScratchStroke> {
        var result = strokes
        for (center in samples(from, to, radius)) {
            result = result.flatMap { cut(it, center, radius + it.widthPx / 2) }
        }
        return result
    }

    /** Stroke eraser: removes every whole stroke the eraser touches. */
    fun eraseWhole(strokes: List<ScratchStroke>, from: StrokePoint, to: StrokePoint, radius: Float): List<ScratchStroke> {
        val centers = samples(from, to, radius)
        return strokes.filterNot { stroke -> centers.any { touches(stroke, it, radius + stroke.widthPx / 2) } }
    }

    private fun samples(from: StrokePoint, to: StrokePoint, radius: Float): List<StrokePoint> {
        val length = hypot(to.x - from.x, to.y - from.y)
        val steps = max(1, (length / max(radius / 2, 0.5f)).toInt())
        return (0..steps).map { i ->
            val t = i.toFloat() / steps
            StrokePoint(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
        }
    }

    private fun touches(stroke: ScratchStroke, c: StrokePoint, r: Float): Boolean {
        val pts = stroke.points
        if (pts.size == 1) return distance(pts[0], c) <= r
        return pts.zipWithNext().any { (a, b) -> segmentDistance(a, b, c) <= r }
    }

    /** Splits [stroke] around the circle (c, r); returns the pieces left outside it. */
    private fun cut(stroke: ScratchStroke, c: StrokePoint, r: Float): List<ScratchStroke> {
        val pts = stroke.points
        if (pts.isEmpty()) return emptyList()
        if (pts.size == 1) return if (distance(pts[0], c) <= r) emptyList() else listOf(stroke)
        if (!boundsNear(pts, c, r)) return listOf(stroke)

        val pieces = mutableListOf<List<StrokePoint>>()
        var current = mutableListOf<StrokePoint>()
        if (distance(pts[0], c) > r) current += pts[0]
        var changed = false

        for (i in 0 until pts.lastIndex) {
            val a = pts[i]
            val b = pts[i + 1]
            val hit = insideInterval(a, b, c, r)
            if (hit == null) {
                if (current.isEmpty()) current += a
                current += b
                continue
            }
            changed = true
            val (t0, t1) = hit
            if (t0 > 0f) current += lerp(a, b, t0)
            if (current.size >= 2) pieces += current
            current = mutableListOf()
            if (t1 < 1f) {
                current += lerp(a, b, t1)
                current += b
            }
        }
        if (current.size >= 2) pieces += current
        if (!changed) return listOf(stroke)
        return pieces
            .filter { piece -> piece.zipWithNext().sumOf { (p, q) -> distance(p, q).toDouble() } >= MIN_PIECE_LENGTH }
            .map { ScratchStroke(it, stroke.widthPx) }
    }

    /** Parameter range [t0, t1] ⊂ [0, 1] of segment a→b that lies inside the circle, or null. */
    private fun insideInterval(a: StrokePoint, b: StrokePoint, c: StrokePoint, r: Float): Pair<Float, Float>? {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val fx = a.x - c.x
        val fy = a.y - c.y
        val qa = dx * dx + dy * dy
        if (qa < 1e-6f) return if (distance(a, c) <= r) 0f to 1f else null
        val qb = 2 * (fx * dx + fy * dy)
        val qc = fx * fx + fy * fy - r * r
        val disc = qb * qb - 4 * qa * qc
        if (disc <= 0f) return null
        val s = sqrt(disc)
        val t0 = (-qb - s) / (2 * qa)
        val t1 = (-qb + s) / (2 * qa)
        if (t1 <= 0f || t0 >= 1f) return null
        return t0.coerceAtLeast(0f) to t1.coerceAtMost(1f)
    }

    private fun boundsNear(pts: List<StrokePoint>, c: StrokePoint, r: Float): Boolean {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (p in pts) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
        return c.x >= minX - r && c.x <= maxX + r && c.y >= minY - r && c.y <= maxY + r
    }

    private fun segmentDistance(a: StrokePoint, b: StrokePoint, c: StrokePoint): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lengthSq = dx * dx + dy * dy
        if (lengthSq < 1e-6f) return distance(a, c)
        val t = (((c.x - a.x) * dx + (c.y - a.y) * dy) / lengthSq).coerceIn(0f, 1f)
        return distance(StrokePoint(a.x + dx * t, a.y + dy * t), c)
    }

    private fun lerp(a: StrokePoint, b: StrokePoint, t: Float) = StrokePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

    private fun distance(a: StrokePoint, b: StrokePoint): Float = hypot(a.x - b.x, a.y - b.y)

    private companion object {
        const val MIN_PIECE_LENGTH = 0.5
    }
}
