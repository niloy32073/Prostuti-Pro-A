package com.prostuti.student.ui.screen.playground.graph.space

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** The 3D box should fill the view without ever being cut off, and keep its size while turning. */
class SpaceCameraTest {

    private val boxAndLabels: List<FloatArray> = buildList {
        for (x in listOf(-1f, 1f)) for (y in listOf(-1f, 1f)) for (z in listOf(-1f, 1f)) add(floatArrayOf(x, y, z))
        for (axis in 0..2) for (sign in listOf(-1f, 1f)) add(FloatArray(3).also { it[axis] = sign * 1.22f })
    }

    /** Largest |x| or |y| on screen (−1…1 is the visible area) for one camera. */
    private fun extent(camera: SpaceCamera, width: Int, height: Int, range: Double = 1.0): Float {
        val mvp = camera.mvp(width, height, range)
        return boxAndLabels.maxOf { p ->
            val c = Mat4.transform(mvp, p[0] * range.toFloat(), p[1] * range.toFloat(), p[2] * range.toFloat())
            maxOf(abs(c[0] / c[3]), abs(c[1] / c[3]))
        }
    }

    @Test fun boxAlwaysFitsAndFillsTheView() {
        for ((w, h) in listOf(1080 to 1000, 1080 to 2000, 2000 to 1000, 800 to 800)) {
            for (pitch in listOf(-80f, -30f, 0f, 24f, 60f, 89f)) {
                val perTurn = (0 until 360 step 7).map { yaw -> extent(SpaceCamera(yaw.toFloat(), pitch), w, h) }
                assertTrue("cut off at $w×$h pitch $pitch: ${perTurn.max()}", perTurn.max() <= 0.921f)
                assertTrue("too small at $w×$h pitch $pitch: ${perTurn.max()}", perTurn.max() >= 0.91f)
            }
        }
    }

    @Test fun zoomRangeDoesNotChangeTheBoxOnScreen() {
        val camera = SpaceCamera()
        assertEquals(extent(camera, 1080, 1200, 1.0), extent(camera, 1080, 1200, 25.0), 1e-4f)
    }
}
