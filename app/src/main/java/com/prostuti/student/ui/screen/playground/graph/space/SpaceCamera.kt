package com.prostuti.student.ui.screen.playground.graph.space

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Orbit camera around the origin, z up. [yaw] turns around the z-axis, [pitch] lifts the eye
 * (degrees). The box −range…range is drawn as the unit cube, so zooming changes the range, not
 * the camera, and the whole box always fits the screen.
 */
@Serializable
data class SpaceCamera(val yaw: Float = DEFAULT_YAW, val pitch: Float = DEFAULT_PITCH) {

    fun rotated(dYaw: Float, dPitch: Float) =
        SpaceCamera((yaw + dYaw) % 360f, (pitch + dPitch).coerceIn(-89f, 89f))

    /** Unit vector from the origin towards the eye. */
    fun eyeDirection(): FloatArray {
        val y = yaw * PI.toFloat() / 180f
        val p = pitch * PI.toFloat() / 180f
        return floatArrayOf(cos(p) * cos(y), cos(p) * sin(y), sin(p))
    }

    /** Projection × view × scale(1/range): world coordinates → clip space. */
    fun mvp(widthPx: Int, heightPx: Int, range: Double): FloatArray {
        val aspect = widthPx.toFloat() / heightPx.coerceAtLeast(1)
        val zoom = fitZoom(aspect)
        // Scale x and y in clip space only: the box grows on screen, depth and lighting stay the same.
        val fit = Mat4.identity().also { it[0] = zoom; it[5] = zoom }
        return Mat4.multiply(fit, Mat4.multiply(viewProjection(this, aspect), Mat4.scale((1.0 / range).toFloat())))
    }

    /**
     * How much to enlarge the picture so the box and its axis labels fill [FIT_MARGIN] of the
     * screen. It is the worst case over a full turn at this pitch, so turning sideways never
     * makes the box grow or shrink; only tilting up/down does, a little.
     */
    private fun fitZoom(aspect: Float): Float {
        var extent = 0f
        for (step in 0..YAW_SAMPLES) {
            val m = viewProjection(SpaceCamera(step * 90f / YAW_SAMPLES, pitch), aspect)
            for (p in FIT_POINTS) {
                val c = Mat4.transform(m, p[0], p[1], p[2])
                if (c[3] <= 1e-6f) continue
                extent = maxOf(extent, abs(c[0] / c[3]), abs(c[1] / c[3]))
            }
        }
        return if (extent > 0f) FIT_MARGIN / extent else 1f
    }
    /** Screen pixel of a world point, or null when it is behind the camera. */
    fun project(mvp: FloatArray, x: Double, y: Double, z: Double, widthPx: Int, heightPx: Int): FloatArray? {
        val c = Mat4.transform(mvp, x.toFloat(), y.toFloat(), z.toFloat())
        if (c[3] <= 1e-6f) return null
        return floatArrayOf((c[0] / c[3] + 1f) / 2f * widthPx, (1f - c[1] / c[3]) / 2f * heightPx)
    }

    companion object {
        private val FOV_Y = (32.0 * PI / 180.0).toFloat()
        private const val YAW_SAMPLES = 18
        private const val FIT_MARGIN = 0.92f

        /** Box corners and axis-label positions (box is −1…1, labels sit just past the axis ends). */
        private val FIT_POINTS: List<FloatArray> = buildList {
            for (x in listOf(-1f, 1f)) for (y in listOf(-1f, 1f)) for (z in listOf(-1f, 1f)) add(floatArrayOf(x, y, z))
            for (axis in 0..2) for (sign in listOf(-1f, 1f)) add(FloatArray(3).also { it[axis] = sign * 1.22f })
        }

        private fun viewProjection(camera: SpaceCamera, aspect: Float): FloatArray {
            val eye = camera.eyeDirection().let { floatArrayOf(it[0] * DISTANCE, it[1] * DISTANCE, it[2] * DISTANCE) }
            val view = Mat4.lookAt(eye, floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 1f))
            return Mat4.multiply(Mat4.perspective(FOV_Y, aspect, 0.5f, 30f), view)
        }

        const val DEFAULT_YAW = -55f
        const val DEFAULT_PITCH = 24f
        const val DISTANCE = 6.4f
    }
}
