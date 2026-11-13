package com.prostuti.student.ui.screen.playground.graph.space

import kotlin.math.sqrt
import kotlin.math.tan

/** Column-major 4×4 float matrices (the OpenGL layout), plain Kotlin so camera maths is unit-testable. */
internal object Mat4 {

    fun identity() = FloatArray(16).also { it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f }

    fun multiply(a: FloatArray, b: FloatArray): FloatArray {
        val r = FloatArray(16)
        for (col in 0 until 4) for (row in 0 until 4) {
            var s = 0f
            for (k in 0 until 4) s += a[k * 4 + row] * b[col * 4 + k]
            r[col * 4 + row] = s
        }
        return r
    }

    fun scale(s: Float) = identity().also { it[0] = s; it[5] = s; it[10] = s }

    /** Perspective with vertical field of view [fovY] (radians). */
    fun perspective(fovY: Float, aspect: Float, near: Float, far: Float): FloatArray {
        val f = 1f / tan(fovY / 2)
        val r = FloatArray(16)
        r[0] = f / aspect
        r[5] = f
        r[10] = (far + near) / (near - far)
        r[11] = -1f
        r[14] = 2 * far * near / (near - far)
        return r
    }

    fun lookAt(eye: FloatArray, center: FloatArray, up: FloatArray): FloatArray {
        val f = normalize(floatArrayOf(center[0] - eye[0], center[1] - eye[1], center[2] - eye[2]))
        val s = normalize(cross(f, up))
        val u = cross(s, f)
        val r = identity()
        r[0] = s[0]; r[4] = s[1]; r[8] = s[2]
        r[1] = u[0]; r[5] = u[1]; r[9] = u[2]
        r[2] = -f[0]; r[6] = -f[1]; r[10] = -f[2]
        r[12] = -(s[0] * eye[0] + s[1] * eye[1] + s[2] * eye[2])
        r[13] = -(u[0] * eye[0] + u[1] * eye[1] + u[2] * eye[2])
        r[14] = f[0] * eye[0] + f[1] * eye[1] + f[2] * eye[2]
        return r
    }

    /** m × (x, y, z, 1) → (x, y, z, w). */
    fun transform(m: FloatArray, x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
        m[0] * x + m[4] * y + m[8] * z + m[12],
        m[1] * x + m[5] * y + m[9] * z + m[13],
        m[2] * x + m[6] * y + m[10] * z + m[14],
        m[3] * x + m[7] * y + m[11] * z + m[15],
    )

    fun cross(a: FloatArray, b: FloatArray) =
        floatArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

    fun normalize(a: FloatArray): FloatArray {
        val l = sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
        return if (l > 0) floatArrayOf(a[0] / l, a[1] / l, a[2] / l) else a
    }
}
