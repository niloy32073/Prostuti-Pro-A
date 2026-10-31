package com.prostuti.student.domain.scratchpad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeEraserTest {

    private val eraser = StrokeEraser()

    /** Horizontal line from x = 0 to 100 at y = 0, sampled every 10px. */
    private val line = ScratchStroke((0..10).map { StrokePoint(it * 10f, 0f) }, widthPx = 2f)
    private val farLine = ScratchStroke(listOf(StrokePoint(0f, 200f), StrokePoint(100f, 200f)), widthPx = 2f)

    @Test fun `partial erase in the middle splits the line in two`() {
        val center = StrokePoint(50f, 0f)
        val result = eraser.erasePartial(listOf(line), center, center, radius = 9f)
        assertEquals(2, result.size)
        val left = result[0].points
        val right = result[1].points
        // Cut at |x − 50| = radius + width/2 = 10
        assertEquals(40f, left.last().x, 0.01f)
        assertEquals(60f, right.first().x, 0.01f)
        assertEquals(0f, left.first().x, 0f)
        assertEquals(100f, right.last().x, 0f)
    }

    @Test fun `partial erase at the end only shortens the line`() {
        val center = StrokePoint(100f, 0f)
        val result = eraser.erasePartial(listOf(line), center, center, radius = 9f)
        assertEquals(1, result.size)
        assertEquals(90f, result[0].points.last().x, 0.01f)
    }

    @Test fun `partial erase dragged across removes the swept band`() {
        val result = eraser.erasePartial(listOf(line), StrokePoint(20f, -30f), StrokePoint(20f, 30f), radius = 4f)
        assertEquals(2, result.size)
        assertTrue(result[0].points.last().x <= 15.01f)
        assertTrue(result[1].points.first().x >= 24.99f)
    }

    @Test fun `stroke eraser removes the whole touched line only`() {
        val center = StrokePoint(50f, 2f)
        val result = eraser.eraseWhole(listOf(line, farLine), center, center, radius = 3f)
        assertEquals(listOf(farLine), result)
    }

    @Test fun `fast stroke-eraser swipe between samples still hits`() {
        // Swipe crosses the line between its endpoints; no single sample lands exactly on it.
        val result = eraser.eraseWhole(listOf(line), StrokePoint(55f, -100f), StrokePoint(55f, 100f), radius = 3f)
        assertTrue(result.isEmpty())
    }

    @Test fun `eraser away from strokes changes nothing`() {
        val center = StrokePoint(50f, 100f)
        val strokes = listOf(line, farLine)
        assertEquals(strokes, eraser.erasePartial(strokes, center, center, radius = 9f))
        assertEquals(strokes, eraser.eraseWhole(strokes, center, center, radius = 9f))
    }

    @Test fun `erasing a dot removes it`() {
        val dot = ScratchStroke(listOf(StrokePoint(10f, 10f)), widthPx = 4f)
        val c = StrokePoint(12f, 10f)
        assertTrue(eraser.erasePartial(listOf(dot), c, c, radius = 3f).isEmpty())
    }
}
