package com.prostuti.student.domain.calculator

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/** EQN mode: simultaneous linear equations (2–4 unknowns) and polynomials (degree 2–4). */
class EquationSolver {

    /**
     * [rows] is the augmented matrix: n rows of n coefficients followed by the constant
     * (a·X + b·Y = c  →  [a, b, c]).
     */
    fun solveLinear(rows: List<List<Double>>): LinearSolution {
        val n = rows.size
        require(n > 0 && rows.all { it.size == n + 1 }) { "Need n rows of n + 1 values" }
        val m = Array(n) { r -> DoubleArray(n + 1) { c -> rows[r][c] } }
        val scale = max(1.0, rows.flatten().maxOf { abs(it) })
        val eps = 1e-12 * scale

        var rank = 0
        val pivotColumns = IntArray(n) { -1 }
        for (col in 0 until n) {
            val pivot = (rank until n).maxByOrNull { abs(m[it][col]) } ?: break
            if (abs(m[pivot][col]) <= eps) continue
            m[rank] = m[pivot].also { m[pivot] = m[rank] }
            for (r in 0 until n) {
                if (r == rank) continue
                val factor = m[r][col] / m[rank][col]
                if (factor == 0.0) continue
                for (c in col..n) m[r][c] -= factor * m[rank][c]
            }
            pivotColumns[rank] = col
            rank++
        }
        if (rank < n) {
            val inconsistent = (rank until n).any { abs(m[it][n]) > 1e-9 * scale }
            return if (inconsistent) LinearSolution.NoSolution else LinearSolution.InfiniteSolutions
        }
        val values = DoubleArray(n)
        for (r in 0 until n) values[pivotColumns[r]] = m[r][n] / m[r][pivotColumns[r]]
        return LinearSolution.Unique(values.map { clean(it) })
    }

    /**
     * Roots of a·Xⁿ + … + constant = 0, [coefficients] highest power first.
     * Real roots come first (largest first), then complex pairs. Throws Math ERROR when a = 0.
     */
    fun solvePolynomial(coefficients: List<Double>): List<Complex> {
        val degree = coefficients.size - 1
        require(degree in 1..4) { "Degree 1–4 only" }
        val lead = coefficients[0]
        if (lead == 0.0) throw CalcException(CalcErrorKind.MATH)
        val monic = coefficients.map { it / lead }

        val roots = when (degree) {
            1 -> listOf(Complex(-monic[1]))
            2 -> quadratic(monic[1], monic[2])
            else -> durandKerner(monic).let { mergeMultipleRoots(it, monic) }
        }
        return roots
            .map { polish(it, monic) }
            .map { tidyComplex(it) }
            .sortedWith(compareBy<Complex> { if (it.isReal) 0 else 1 }.thenByDescending { it.re }.thenByDescending { it.im })
    }

    private fun quadratic(b: Double, c: Double): List<Complex> {
        val disc = b * b - 4 * c
        if (disc >= 0) {
            val s = kotlin.math.sqrt(disc)
            // Stable form: avoid subtracting nearly equal numbers.
            val q = -0.5 * (b + if (b >= 0) s else -s)
            if (q == 0.0) return listOf(Complex(0.0), Complex(0.0))
            return listOf(Complex(q), Complex(c / q))
        }
        val s = kotlin.math.sqrt(-disc) / 2
        return listOf(Complex(-b / 2, s), Complex(-b / 2, -s))
    }

    /** Simultaneous Weierstrass iteration on a monic polynomial. */
    private fun durandKerner(monic: List<Double>): List<Complex> {
        val n = monic.size - 1
        val radius = 1 + monic.drop(1).maxOf { abs(it) }
        val seed = Complex(0.4, 0.9)
        var roots = List(n) { k -> power(seed, k) * radius }
        repeat(800) {
            var change = 0.0
            val next = roots.mapIndexed { i, z ->
                var denominator = Complex.ONE
                roots.forEachIndexed { j, w -> if (j != i) denominator *= (z - w) }
                val updated = z - evaluate(monic, z) / denominator
                change = max(change, (updated - z).abs())
                updated
            }
            roots = next
            if (change < 1e-15 * radius) return roots
        }
        return roots
    }

    /**
     * A root of multiplicity m comes out of the iteration as m points scattered around it; their
     * average is far more accurate. Merge such clusters when the derivatives confirm the multiplicity.
     */
    private fun mergeMultipleRoots(roots: List<Complex>, monic: List<Double>): List<Complex> {
        val remaining = roots.toMutableList()
        val result = mutableListOf<Complex>()
        while (remaining.isNotEmpty()) {
            val first = remaining.removeAt(0)
            val cluster = mutableListOf(first)
            remaining.removeAll { z ->
                val near = (z - first).abs() <= 1e-3 * (1 + first.abs())
                if (near) cluster += z
                near
            }
            if (cluster.size == 1) { result += first; continue }
            var center = Complex.ZERO
            cluster.forEach { center += it }
            center = center * (1.0 / cluster.size)
            // Scatter grows with multiplicity (≈ ε^(1/m)); anything wider is distinct roots.
            val spreadLimit = when (cluster.size) { 2 -> 1e-6; 3 -> 1e-4; else -> 1e-3 } * (1 + center.abs())
            val spread = cluster.maxOf { (it - center).abs() }
            // A root of multiplicity m is a simple root of the (m−1)th derivative: Newton there is exact.
            val refined = if (spread <= spreadLimit) refineMultipleRoot(monic, center, cluster.size) else null
            if (refined != null) {
                repeat(cluster.size) { result += refined }
            } else {
                result += cluster
            }
        }
        return result
    }

    /** Newton on p^(m−1) from [start]; returns the root if p, p′, … p^(m−1) all vanish there. */
    private fun refineMultipleRoot(monic: List<Double>, start: Complex, multiplicity: Int): Complex? {
        val derivatives = generateSequence(monic) { derivative(it) }.take(multiplicity + 1).toList()
        val target = derivatives[multiplicity - 1]
        val slope = derivatives[multiplicity]
        var z = start
        repeat(30) {
            val d = evaluate(slope, z)
            if (d.abs() == 0.0) return@repeat
            val step = evaluate(target, z) / d
            z -= step
            if (step.abs() <= 1e-16 * max(1.0, z.abs())) return@repeat
        }
        val scale = max(1.0, monic.maxOf { abs(it) }) * max(1.0, z.abs()).pow(monic.size)
        val vanishes = derivatives.take(multiplicity).all { evaluate(it, z).abs() <= 1e-8 * scale }
        return z.takeIf { vanishes }
    }

    private fun polish(z: Complex, monic: List<Double>): Complex {
        var x = z
        val d = derivative(monic)
        repeat(4) {
            val fx = evaluate(monic, x)
            val dx = evaluate(d, x)
            if (dx.abs() == 0.0) return x
            val next = x - fx / dx
            if (!next.re.isFinite() || !next.im.isFinite()) return x
            if (evaluate(monic, next).abs() >= fx.abs()) return x
            x = next
        }
        return x
    }

    private fun tidyComplex(z: Complex): Complex {
        val size = max(1.0, z.abs())
        val im = if (abs(z.im) <= 1e-9 * size) 0.0 else z.im
        val re = if (abs(z.re) <= 1e-12 * size) 0.0 else z.re
        return Complex(clean(re), clean(im))
    }

    private fun clean(x: Double): Double = CalcMath.roundSignificant(x, CalcMath.INTERNAL_DIGITS - 2)

    private fun evaluate(coefficients: List<Double>, z: Complex): Complex {
        var acc = Complex.ZERO
        for (c in coefficients) acc = acc * z + Complex(c)
        return acc
    }

    private fun derivative(coefficients: List<Double>): List<Double> {
        val n = coefficients.size - 1
        if (n == 0) return listOf(0.0)
        return coefficients.dropLast(1).mapIndexed { i, c -> c * (n - i) }
    }

    private fun power(z: Complex, k: Int): Complex {
        var r = Complex.ONE
        repeat(k) { r *= z }
        return r
    }
}
