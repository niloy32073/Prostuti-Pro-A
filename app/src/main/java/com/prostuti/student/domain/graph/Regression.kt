package com.prostuti.student.domain.graph

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Least-squares best fits for a data table.
 *
 * Polynomials are solved on centred, scaled x (well-conditioned) and expanded back. Exponential and
 * power fits start from the textbook log-linear fit and are then refined by Gauss–Newton on the
 * original data, so they minimise the real vertical errors. R² is always measured on the original y.
 */
object Regression {

    fun fit(xs: DoubleArray, ys: DoubleArray, model: RegressionModel): RegressionResult? {
        if (model == RegressionModel.NONE || xs.size != ys.size || xs.size < model.parameterCount) return null
        val coefficients = when (model) {
            RegressionModel.LINEAR -> polynomial(xs, ys, 1)?.reversedArray() // m, c
            RegressionModel.QUADRATIC -> polynomial(xs, ys, 2)?.reversedArray()
            RegressionModel.CUBIC -> polynomial(xs, ys, 3)?.reversedArray()
            RegressionModel.EXPONENTIAL -> exponential(xs, ys)
            RegressionModel.POWER -> power(xs, ys)
            RegressionModel.LOGARITHMIC -> logarithmic(xs, ys)
            RegressionModel.NONE -> null
        } ?: return null
        if (coefficients.any { !it.isFinite() }) return null
        val f = function(model, coefficients)
        val mean = ys.average()
        var ssRes = 0.0
        var ssTot = 0.0
        for (i in xs.indices) {
            val e = ys[i] - f(xs[i])
            ssRes += e * e
            ssTot += (ys[i] - mean) * (ys[i] - mean)
        }
        val r2 = if (ssTot == 0.0) (if (ssRes < 1e-24) 1.0 else 0.0) else 1 - ssRes / ssTot
        val r = if (model == RegressionModel.LINEAR) pearson(xs, ys) else null
        return RegressionResult(
            model, coefficients,
            equation = text(model, coefficients) { plain(it) },
            display = text(model, coefficients) { rounded(it) },
            rSquared = r2,
            r = r,
        )
    }

    /** y(x) for the fitted coefficients (same order as [RegressionResult.coefficients]). */
    fun function(model: RegressionModel, c: DoubleArray): (Double) -> Double = when (model) {
        RegressionModel.LINEAR, RegressionModel.QUADRATIC, RegressionModel.CUBIC -> { x ->
            var y = 0.0
            for (k in c) y = y * x + k // Horner, highest power first
            y
        }
        RegressionModel.EXPONENTIAL -> { x -> c[0] * exp(c[1] * x) }
        RegressionModel.POWER -> { x -> c[0] * x.pow(c[1]) }
        RegressionModel.LOGARITHMIC -> { x -> c[0] + c[1] * ln(x) }
        RegressionModel.NONE -> { _ -> Double.NaN }
    }

    // ── fits ────────────────────────────────────────────────────────────────────────────────

    /** Coefficients lowest power first, by normal equations on z = (x − mean) / spread. */
    private fun polynomial(xs: DoubleArray, ys: DoubleArray, degree: Int): DoubleArray? {
        val n = degree + 1
        val mean = xs.average()
        val spread = xs.maxOf { abs(it - mean) }.takeIf { it > 0 } ?: return null
        val z = DoubleArray(xs.size) { (xs[it] - mean) / spread }
        val a = Array(n) { DoubleArray(n) }
        val b = DoubleArray(n)
        for (i in xs.indices) {
            val powers = DoubleArray(2 * n) { z[i].pow(it) }
            for (r in 0 until n) {
                b[r] += powers[r] * ys[i]
                for (c in 0 until n) a[r][c] += powers[r + c]
            }
        }
        val scaled = solve(a, b) ?: return null
        // Expand Σ s_k ((x − mean)/spread)^k into plain powers of x.
        val out = DoubleArray(n)
        for (k in 0 until n) {
            val factor = scaled[k] / spread.pow(k)
            for (j in 0..k) out[j] += factor * binomial(k, j) * (-mean).pow(k - j)
        }
        return out
    }

    private fun logarithmic(xs: DoubleArray, ys: DoubleArray): DoubleArray? {
        if (xs.any { it <= 0 }) return null
        val lx = DoubleArray(xs.size) { ln(xs[it]) }
        val p = polynomial(lx, ys, 1) ?: return null
        return doubleArrayOf(p[0], p[1]) // a + b ln x
    }

    private fun exponential(xs: DoubleArray, ys: DoubleArray): DoubleArray? {
        if (ys.any { it <= 0 }) return null
        val ly = DoubleArray(ys.size) { ln(ys[it]) }
        val p = polynomial(xs, ly, 1) ?: return null
        return gaussNewton(xs, ys, doubleArrayOf(exp(p[0]), p[1])) { x, c, g ->
            val e = exp(c[1] * x)
            g[0] = e
            g[1] = c[0] * x * e
            c[0] * e
        }
    }

    private fun power(xs: DoubleArray, ys: DoubleArray): DoubleArray? {
        if (xs.any { it <= 0 } || ys.any { it <= 0 }) return null
        val lx = DoubleArray(xs.size) { ln(xs[it]) }
        val ly = DoubleArray(ys.size) { ln(ys[it]) }
        val p = polynomial(lx, ly, 1) ?: return null
        return gaussNewton(xs, ys, doubleArrayOf(exp(p[0]), p[1])) { x, c, g ->
            val v = x.pow(c[1])
            g[0] = v
            g[1] = c[0] * v * ln(x)
            c[0] * v
        }
    }

    private fun interface Model2 {
        /** Value at x, writing ∂/∂c₀ and ∂/∂c₁ into g. */
        fun eval(x: Double, c: DoubleArray, g: DoubleArray): Double
    }

    /** Two-parameter Gauss–Newton with step halving; keeps the start if it can't improve. */
    private fun gaussNewton(xs: DoubleArray, ys: DoubleArray, start: DoubleArray, model: Model2): DoubleArray {
        var c = start.copyOf()
        val g = DoubleArray(2)
        fun sse(p: DoubleArray): Double {
            var s = 0.0
            for (i in xs.indices) { val e = ys[i] - model.eval(xs[i], p, g); s += e * e }
            return s
        }
        var best = sse(c)
        repeat(50) {
            var a00 = 0.0; var a01 = 0.0; var a11 = 0.0; var b0 = 0.0; var b1 = 0.0
            for (i in xs.indices) {
                val e = ys[i] - model.eval(xs[i], c, g)
                a00 += g[0] * g[0]; a01 += g[0] * g[1]; a11 += g[1] * g[1]
                b0 += g[0] * e; b1 += g[1] * e
            }
            val det = a00 * a11 - a01 * a01
            if (abs(det) < 1e-300) return c
            val d0 = (b0 * a11 - b1 * a01) / det
            val d1 = (a00 * b1 - a01 * b0) / det
            var step = 1.0
            while (step > 1e-6) {
                val trial = doubleArrayOf(c[0] + step * d0, c[1] + step * d1)
                val s = sse(trial)
                if (s.isFinite() && s < best) {
                    val gain = best - s
                    c = trial
                    best = s
                    if (gain <= 1e-15 * (1 + best)) return c
                    break
                }
                step /= 2
            }
            if (step <= 1e-6) return c
        }
        return c
    }

    /** Gaussian elimination with partial pivoting. */
    private fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = b.size
        for (col in 0 until n) {
            val pivot = (col until n).maxBy { abs(a[it][col]) }
            if (abs(a[pivot][col]) < 1e-14) return null
            a[col] = a[pivot].also { a[pivot] = a[col] }
            b[col] = b[pivot].also { b[pivot] = b[col] }
            for (r in col + 1 until n) {
                val f = a[r][col] / a[col][col]
                for (c in col until n) a[r][c] -= f * a[col][c]
                b[r] -= f * b[col]
            }
        }
        val x = DoubleArray(n)
        for (r in n - 1 downTo 0) {
            var s = b[r]
            for (c in r + 1 until n) s -= a[r][c] * x[c]
            x[r] = s / a[r][r]
        }
        return x
    }

    private fun binomial(n: Int, k: Int): Double {
        var r = 1.0
        for (i in 1..k) r = r * (n - k + i) / i
        return r
    }

    private fun pearson(xs: DoubleArray, ys: DoubleArray): Double {
        val mx = xs.average()
        val my = ys.average()
        var sxy = 0.0; var sxx = 0.0; var syy = 0.0
        for (i in xs.indices) {
            sxy += (xs[i] - mx) * (ys[i] - my)
            sxx += (xs[i] - mx) * (xs[i] - mx)
            syy += (ys[i] - my) * (ys[i] - my)
        }
        return if (sxx == 0.0 || syy == 0.0) Double.NaN else sxy / sqrt(sxx * syy)
    }

    // ── text ────────────────────────────────────────────────────────────────────────────────

    private fun plain(v: Double): String =
        BigDecimal(v).round(MathContext(15)).stripTrailingZeros().toPlainString()

    private fun rounded(v: Double): String = GraphNumberFormat.format(v, 4).replace('−', '-').let {
        if (it.contains('×')) plain(BigDecimal(v).round(MathContext(4)).toDouble()) else it
    }

    /** "y = 2.5 x^2 - 3 x + 1" (signs folded, zero terms kept so the form stays recognisable). */
    private fun text(model: RegressionModel, c: DoubleArray, number: (Double) -> String): String {
        fun signed(v: Double, term: String, first: Boolean): String {
            val body = if (term.isEmpty()) number(abs(v)) else "${number(abs(v))} $term"
            return when {
                first -> (if (v < 0) "-" else "") + body
                v < 0 -> " - $body"
                else -> " + $body"
            }
        }
        return "y = " + when (model) {
            RegressionModel.LINEAR -> signed(c[0], "x", true) + signed(c[1], "", false)
            RegressionModel.QUADRATIC -> signed(c[0], "x^2", true) + signed(c[1], "x", false) + signed(c[2], "", false)
            RegressionModel.CUBIC -> signed(c[0], "x^3", true) + signed(c[1], "x^2", false) +
                signed(c[2], "x", false) + signed(c[3], "", false)
            RegressionModel.EXPONENTIAL -> "${number(c[0])} e^(${number(c[1])} x)"
            RegressionModel.POWER -> "${number(c[0])} x^(${number(c[1])})"
            RegressionModel.LOGARITHMIC -> signed(c[0], "", true) + signed(c[1], "ln(x)", false)
            RegressionModel.NONE -> "0"
        }
    }
}
