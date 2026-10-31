package com.prostuti.student.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EquationSolverTest {

    private val solver = EquationSolver()

    private fun unique(rows: List<List<Double>>): List<Double> =
        (solver.solveLinear(rows) as LinearSolution.Unique).values

    @Test fun `two unknowns`() {
        // 2X + 3Y = 8, X − Y = −1 → X = 1, Y = 2
        val values = unique(listOf(listOf(2.0, 3.0, 8.0), listOf(1.0, -1.0, -1.0)))
        assertEquals(listOf(1.0, 2.0), values)
    }

    @Test fun `three unknowns`() {
        val values = unique(
            listOf(listOf(1.0, 1.0, 1.0, 6.0), listOf(0.0, 2.0, 5.0, -4.0), listOf(2.0, 5.0, -1.0, 27.0))
        )
        assertEquals(listOf(5.0, 3.0, -2.0), values)
    }

    @Test fun `four unknowns`() {
        // X=1, Y=2, Z=3, T=4
        val rows = listOf(
            listOf(1.0, 1.0, 1.0, 1.0, 10.0),
            listOf(1.0, -1.0, 1.0, -1.0, -2.0),
            listOf(1.0, 2.0, 0.0, 1.0, 9.0),
            listOf(2.0, 0.0, -1.0, 1.0, 3.0),
        )
        unique(rows).zip(listOf(1.0, 2.0, 3.0, 4.0)).forEach { (got, want) -> assertEquals(want, got, 1e-12) }
    }

    @Test fun `parallel lines have no solution`() =
        assertEquals(LinearSolution.NoSolution, solver.solveLinear(listOf(listOf(1.0, 1.0, 1.0), listOf(2.0, 2.0, 5.0))))

    @Test fun `same line has infinite solutions`() =
        assertEquals(LinearSolution.InfiniteSolutions, solver.solveLinear(listOf(listOf(1.0, 1.0, 1.0), listOf(2.0, 2.0, 2.0))))

    @Test fun `quadratic real roots largest first`() {
        val roots = solver.solvePolynomial(listOf(1.0, -3.0, 2.0))
        assertEquals(listOf(Complex(2.0), Complex(1.0)), roots)
    }

    @Test fun `quadratic complex roots`() {
        val roots = solver.solvePolynomial(listOf(1.0, 2.0, 5.0))
        assertEquals(Complex(-1.0, 2.0), roots[0])
        assertEquals(Complex(-1.0, -2.0), roots[1])
    }

    @Test fun `cubic with triple root is exact`() {
        val roots = solver.solvePolynomial(listOf(1.0, -3.0, 3.0, -1.0))   // (X−1)³
        roots.forEach { assertEquals(1.0, it.re, 1e-12); assertEquals(0.0, it.im, 0.0) }
    }

    @Test fun `cubic with one real and two complex roots`() {
        val roots = solver.solvePolynomial(listOf(1.0, 0.0, 0.0, -8.0))     // X³ = 8
        assertEquals(Complex(2.0), roots[0])
        assertEquals(-1.0, roots[1].re, 1e-12)
        assertEquals(Math.sqrt(3.0), kotlin.math.abs(roots[1].im), 1e-12)
    }

    @Test fun `quartic with four real roots`() {
        // (X−1)(X−2)(X−3)(X−4) = X⁴ − 10X³ + 35X² − 50X + 24
        val roots = solver.solvePolynomial(listOf(1.0, -10.0, 35.0, -50.0, 24.0))
        assertEquals(listOf(4.0, 3.0, 2.0, 1.0), roots.map { it.re })
        assertTrue(roots.all { it.isReal })
    }

    @Test fun `quartic with double roots`() {
        // (X−2)²(X+1)² = X⁴ − 2X³ − 3X² + 4X + 4
        val roots = solver.solvePolynomial(listOf(1.0, -2.0, -3.0, 4.0, 4.0))
        assertEquals(listOf(2.0, 2.0, -1.0, -1.0), roots.map { it.re })
    }

    @Test fun `quartic with a quadruple root`() {
        val roots = solver.solvePolynomial(listOf(1.0, -8.0, 24.0, -32.0, 16.0))   // (X−2)⁴
        assertEquals(listOf(2.0, 2.0, 2.0, 2.0), roots.map { it.re })
    }

    @Test fun `quartic with a triple root and a simple root`() {
        val roots = solver.solvePolynomial(listOf(1.0, -1.0, -3.0, 5.0, -2.0))     // (X−1)³(X+2)
        assertEquals(listOf(1.0, 1.0, 1.0, -2.0), roots.map { it.re })
    }

    @Test fun `quartic with only complex roots`() {
        val roots = solver.solvePolynomial(listOf(1.0, 0.0, 0.0, 0.0, 1.0))   // X⁴ = −1
        val h = Math.sqrt(0.5)
        roots.forEach {
            assertEquals(h, kotlin.math.abs(it.re), 1e-12)
            assertEquals(h, kotlin.math.abs(it.im), 1e-12)
        }
    }

    @Test(expected = CalcException::class)
    fun `zero leading coefficient is a math error`() {
        solver.solvePolynomial(listOf(0.0, 1.0, 2.0))
    }
}
