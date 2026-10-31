package com.prostuti.student.domain.calculator

import kotlin.math.hypot
import kotlin.math.sqrt

data class Complex(val re: Double, val im: Double = 0.0) {
    operator fun plus(o: Complex) = Complex(re + o.re, im + o.im)
    operator fun minus(o: Complex) = Complex(re - o.re, im - o.im)
    operator fun times(o: Complex) = Complex(re * o.re - im * o.im, re * o.im + im * o.re)
    operator fun times(k: Double) = Complex(re * k, im * k)
    operator fun div(o: Complex): Complex {
        val d = o.re * o.re + o.im * o.im
        return Complex((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d)
    }
    operator fun unaryMinus() = Complex(-re, -im)
    fun abs(): Double = hypot(re, im)
    val isReal: Boolean get() = im == 0.0

    companion object {
        val ZERO = Complex(0.0)
        val ONE = Complex(1.0)

        /** Principal square root. */
        fun sqrt(z: Complex): Complex {
            if (z.im == 0.0) return if (z.re >= 0) Complex(sqrt(z.re)) else Complex(0.0, sqrt(-z.re))
            val r = z.abs()
            val re = sqrt((r + z.re) / 2)
            val im = sqrt((r - z.re) / 2) * if (z.im < 0) -1 else 1
            return Complex(re, im)
        }
    }
}
