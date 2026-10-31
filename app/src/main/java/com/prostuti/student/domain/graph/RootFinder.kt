package com.prostuti.student.domain.graph

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Brent's root finder and golden-section minimiser, both NaN-safe. */
object RootFinder {

    /**
     * A root of [f] in [a, b] where f(a) and f(b) have opposite signs, accurate to ~1e-14 relative.
     * Returns null when there is no bracket or the function is undefined inside.
     */
    fun brent(f: RealFunction, a0: Double, b0: Double, fa0: Double = f.value(a0), fb0: Double = f.value(b0)): Double? {
        var a = a0
        var b = b0
        var fa = fa0
        var fb = fb0
        if (fa.isNaN() || fb.isNaN()) return null
        if (fa == 0.0) return a
        if (fb == 0.0) return b
        if ((fa > 0) == (fb > 0)) return null
        if (abs(fa) < abs(fb)) { a = b.also { b = a }; fa = fb.also { fb = fa } }
        var c = a
        var fc = fa
        var mflag = true
        var d = 0.0
        repeat(200) {
            if (fb == 0.0) return b
            val tol = 2e-16 * abs(b) + 1e-300
            if (abs(b - a) <= tol) return b
            var s = if (fa != fc && fb != fc) {
                a * fb * fc / ((fa - fb) * (fa - fc)) + b * fa * fc / ((fb - fa) * (fb - fc)) + c * fa * fb / ((fc - fa) * (fc - fb))
            } else {
                b - fb * (b - a) / (fb - fa)
            }
            val lo = (3 * a + b) / 4
            val bisect = !((s > minOf(lo, b) && s < maxOf(lo, b))) ||
                (mflag && abs(s - b) >= abs(b - c) / 2) ||
                (!mflag && abs(s - b) >= abs(c - d) / 2) ||
                (mflag && abs(b - c) < tol) ||
                (!mflag && abs(c - d) < tol)
            if (bisect) { s = (a + b) / 2; mflag = true } else mflag = false
            val fs = f.value(s)
            if (fs.isNaN()) return null
            d = c
            c = b
            fc = fb
            if ((fa > 0) != (fs > 0)) { b = s; fb = fs } else { a = s; fa = fs }
            if (abs(fa) < abs(fb)) { a = b.also { b = a }; fa = fb.also { fb = fa } }
        }
        return b
    }

    /** Minimum of [f] on [a, b] by golden-section search: returns x of the minimum. */
    fun minimize(f: RealFunction, a0: Double, b0: Double, iterations: Int = 80): Double {
        var a = a0
        var b = b0
        val g = (sqrt(5.0) - 1) / 2
        var c = b - g * (b - a)
        var d = a + g * (b - a)
        var fc = f.value(c)
        var fd = f.value(d)
        repeat(iterations) {
            if (abs(b - a) <= 1e-15 * max(1.0, abs(a))) return (a + b) / 2
            if (fc < fd || fd.isNaN()) {
                b = d; d = c; fd = fc
                c = b - g * (b - a); fc = f.value(c)
            } else {
                a = c; c = d; fc = fd
                d = a + g * (b - a); fd = f.value(d)
            }
        }
        return (a + b) / 2
    }
}
